package com.hasandogan.courier_tracking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hasandogan.courier_tracking.model.CourierLocation;
import com.hasandogan.courier_tracking.model.Store;
import com.hasandogan.courier_tracking.util.DistanceCalculator;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CourierLocationService {

    private static final Logger logger = LoggerFactory.getLogger(CourierLocationService.class);

    private final List<Store> stores = new ArrayList<>();
    private final Map<String, List<CourierLocation>> courierLocations = new HashMap<>();
    private final Map<String, Map<String, LocalDateTime>> lastEntranceTime = new HashMap<>(); // courierId -> storeName -> lastTime

    @Value("classpath:stores.json")
    private org.springframework.core.io.Resource storesJsonFile;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private H3Core h3Core;

    private final DistanceCalculator distanceCalculator = new DistanceCalculator();


    @PostConstruct
    public void initializeH3() {
        try {
            logger.info("Attempting to initialize H3Core...");
            h3Core = H3Core.newInstance();
            logger.info("H3Core initialized successfully.");
        } catch (IOException e) {
            logger.error("Error initializing H3Core using default loader: {}", e.getMessage());
            try {
                logger.info("Attempting to initialize H3Core with system loader...");
                h3Core = H3Core.newSystemInstance();
                logger.info("H3Core initialized successfully using system loader.");
            } catch (Exception ex) {
                logger.error("Error initializing H3Core using system loader as well: {}", ex.getMessage());
                logger.error("Please ensure the native H3 library is available on your system.");
            }
        }
    }

    @PostConstruct
    public void loadStores() {
        try {
            Store[] storeArray = objectMapper.readValue(storesJsonFile.getInputStream(), Store[].class);
            stores.addAll(Arrays.asList(storeArray));
            logger.info("Loaded {} stores from stores.json", stores.size());
        } catch (IOException e) {
            logger.error("Error loading stores from stores.json: {}", e.getMessage());
        }
    }

    public void processLocation(CourierLocation location) {
        String courierId = location.getCourierId();
        courierLocations.computeIfAbsent(courierId, k -> new ArrayList<>()).add(location);
        checkIfEnteredStore(location);
    }

    private void checkIfEnteredStore(CourierLocation location) {
        if (h3Core == null) {
            logger.warn("H3Core not initialized. Cannot check store entrances.");
            return;
        }

        LatLng courierLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        try {
            int h3_RESOLUTION = 11;
            String courierH3Index = h3Core.latLngToCellAddress(courierLatLng.lat, courierLatLng.lng, h3_RESOLUTION);

            for (Store store : stores) {
                LatLng storeLatLng = new LatLng(store.getLat(), store.getLng());
                String storeH3Index = h3Core.latLngToCellAddress(storeLatLng.lat, storeLatLng.lng, h3_RESOLUTION);

                if (courierH3Index.equals(storeH3Index)) {
                    double distance = distanceCalculator.calculateDistance(
                            location.getLatitude(), location.getLongitude(),
                            store.getLat(), store.getLng()
                    );
                    if (distance <= 100) {
                        logEntrance(location.getCourierId(), store.getName(), location.getTime());
                    }
                } else {
                    Set<String> neighbors = new HashSet<>(h3Core.gridDisk(storeH3Index, 1));
                    if (neighbors.contains(courierH3Index)) {
                        double distance = distanceCalculator.calculateDistance(
                                location.getLatitude(), location.getLongitude(),
                                store.getLat(), store.getLng()
                        );
                        if (distance <= 100) {
                            logEntrance(location.getCourierId(), store.getName(), location.getTime());
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid coordinates for H3 conversion: {}", e.getMessage());
        }
    }

    private void logEntrance(String courierId, String storeName, LocalDateTime time) {
        if (!shouldLogEntrance(courierId, storeName, time)) {
            logger.info("Courier {} entered store {} at {}", courierId, storeName, time);
            lastEntranceTime.computeIfAbsent(courierId, k -> new HashMap<>())
                    .put(storeName, time);
        }
    }

    private boolean shouldLogEntrance(String courierId, String storeName, LocalDateTime currentTime) {
        Map<String, LocalDateTime> storeTimes = lastEntranceTime.get(courierId);
        if (storeTimes != null) {
            LocalDateTime lastTime = storeTimes.get(storeName);
            return lastTime != null && currentTime.isBefore(lastTime.plusMinutes(1));
        }
        return false;
    }

    public double getTotalTravelDistance(String courierId) {
        List<CourierLocation> locations = courierLocations.get(courierId);
        if (locations == null) {
            return 0.0;
        }
        
        if (locations.size() < 2) {
            logger.info("Courier {} has only {} location points. At least 2 points are needed to calculate distance.", 
                    courierId, locations.size());
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < locations.size() - 1; i++) {
            CourierLocation current = locations.get(i);
            CourierLocation next = locations.get(i + 1);
            totalDistance += distanceCalculator.calculateDistance(
                    current.getLatitude(), current.getLongitude(),
                    next.getLatitude(), next.getLongitude()
            );
        }
        return totalDistance;
    }
    
    public String getTotalTravelDistanceOfCourier(String courierId) {
        double totalDistance = getTotalTravelDistance(courierId);
        
        if (totalDistance < 1000) {
            return String.format("%.2f mt", totalDistance);
        } else {
            double distanceInKm = totalDistance / 1000.0;
            return String.format("%.2f km", distanceInKm);
        }
    }
    
    public List<CourierLocation> getCourierLocations(String courierId) {
        return courierLocations.get(courierId);
    }
}