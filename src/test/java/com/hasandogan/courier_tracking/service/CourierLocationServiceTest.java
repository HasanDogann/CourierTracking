package com.hasandogan.courier_tracking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hasandogan.courier_tracking.model.CourierLocation;
import com.hasandogan.courier_tracking.model.Store;
import com.hasandogan.courier_tracking.util.DistanceCalculator;
import com.uber.h3core.H3Core;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CourierLocationServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Resource storesJsonFile;

    @Mock
    private H3Core h3Core;

    @Mock
    private DistanceCalculator distanceCalculator;

    @InjectMocks
    private CourierLocationService courierLocationService;

    private CourierLocation atasehirLocation;
    private CourierLocation ortakoyLocation;

    @BeforeEach
    public void setup() throws IOException {
        // Create test locations
        atasehirLocation = new CourierLocation(
            LocalDateTime.of(2025, 3, 22, 10, 0),
            "courier123",
            40.9923307,
            29.1244229
        );

        ortakoyLocation = new CourierLocation(
            LocalDateTime.of(2025, 3, 22, 11, 0),
            "courier123",
            41.055783,
            29.0210292
        );

        // Create test stores
        Store atasehirStore = new Store();
        atasehirStore.setName("Ataşehir MMM Migros");
        atasehirStore.setLat(40.9923307);
        atasehirStore.setLng(29.1244229);

        Store ortakoyStore = new Store();
        ortakoyStore.setName("Ortaköy MMM Migros");
        ortakoyStore.setLat(41.055783);
        ortakoyStore.setLng(29.0210292);

        // Mock store loading
        Store[] stores = new Store[] {atasehirStore, ortakoyStore};
        String storesJson = new ObjectMapper().writeValueAsString(stores);
        InputStream inputStream = new ByteArrayInputStream(storesJson.getBytes());
        
        doReturn(inputStream).when(storesJsonFile).getInputStream();
        doReturn(stores).when(objectMapper).readValue(any(InputStream.class), eq(Store[].class));
        
        // Set up H3Core mock - use doReturn instead of when for more flexibility
        doReturn("testH3Index").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        doReturn(Collections.emptyList()).when(h3Core).gridDisk(anyString(), anyInt());
        
        // Set up the stores list through reflection
        List<Store> storeList = new ArrayList<>();
        storeList.add(atasehirStore);
        storeList.add(ortakoyStore);
        ReflectionTestUtils.setField(courierLocationService, "stores", storeList);
        
        // Load stores manually
        courierLocationService.loadStores();
    }

    @Test
    public void testProcessLocation() {
        // Given
        doReturn("index1").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        doReturn(Collections.emptyList()).when(h3Core).gridDisk(anyString(), anyInt());
        doReturn(150.0).when(distanceCalculator).calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());

        ReflectionTestUtils.setField(courierLocationService, "distanceCalculator", distanceCalculator);

        // When
        courierLocationService.processLocation(atasehirLocation);

        Map<String, List<CourierLocation>> courierLocations = 
            (Map<String, List<CourierLocation>>) ReflectionTestUtils.getField(courierLocationService, "courierLocations");
        
        // Then
        assertNotNull(courierLocations);
        assertTrue(courierLocations.containsKey("courier123"));
        assertEquals(1, courierLocations.get("courier123").size());
        assertEquals(atasehirLocation, courierLocations.get("courier123").get(0));
    }

    @Test
    public void testGetTotalTravelDistance_WithNoLocations() {
        // When no locations are present
        double distance = courierLocationService.getTotalTravelDistance("courier123");
        
        // Then distance should be 0
        assertEquals(0.0, distance);
    }

    @Test
    public void testGetTotalTravelDistance_WithOneLocation() {
        // Given
        doReturn("singleLocationIndex").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        doReturn(Collections.emptyList()).when(h3Core).gridDisk(anyString(), anyInt());
        doReturn(150.0).when(distanceCalculator).calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());

        ReflectionTestUtils.setField(courierLocationService, "distanceCalculator", distanceCalculator);

        courierLocationService.processLocation(atasehirLocation);
        
        // When
        double distance = courierLocationService.getTotalTravelDistance("courier123");
        
        // Then
        assertEquals(0.0, distance);
    }

    @Test
    public void testGetTotalTravelDistance_WithMultipleLocations() {
        // Given
        doReturn("atasehirH3Index").when(h3Core).latLngToCellAddress(eq(atasehirLocation.getLatitude()), eq(atasehirLocation.getLongitude()), anyInt());
        doReturn("ortakoyH3Index").when(h3Core).latLngToCellAddress(eq(ortakoyLocation.getLatitude()), eq(ortakoyLocation.getLongitude()), anyInt());
        doReturn("someH3Index").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());

        doReturn(Collections.emptyList()).when(h3Core).gridDisk(anyString(), anyInt());
        
        // Mock distance calculation between store and location (for store entrance check) to return > 100
        doReturn(150.0).when(distanceCalculator).calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());

        // Mock specific distance calculation for travel distance
        doReturn(11180.91).when(distanceCalculator).calculateDistance(
                eq(atasehirLocation.getLatitude()), eq(atasehirLocation.getLongitude()),
                eq(ortakoyLocation.getLatitude()), eq(ortakoyLocation.getLongitude()));

        ReflectionTestUtils.setField(courierLocationService, "distanceCalculator", distanceCalculator);

        courierLocationService.processLocation(atasehirLocation);
        courierLocationService.processLocation(ortakoyLocation);
        
        // When
        double distance = courierLocationService.getTotalTravelDistance("courier123");

        assertEquals(11180.91, distance);
        verify(distanceCalculator, atLeastOnce()).calculateDistance(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        );
    }

    @Test
    public void testGetTotalTravelDistance_OfCourier_LessThan1000M() {
       // Given
        doReturn("formattedDistanceIndex").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        doReturn(Collections.emptyList()).when(h3Core).gridDisk(anyString(), anyInt());

        doReturn(150.0).when(distanceCalculator).calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());
        doReturn(500.0).when(distanceCalculator).calculateDistance(
                eq(atasehirLocation.getLatitude()), eq(atasehirLocation.getLongitude()),
                eq(ortakoyLocation.getLatitude()), eq(ortakoyLocation.getLongitude()));
        
        ReflectionTestUtils.setField(courierLocationService, "distanceCalculator", distanceCalculator);

        courierLocationService.processLocation(atasehirLocation);
        courierLocationService.processLocation(ortakoyLocation);
        
        // When
        String formattedDistance = courierLocationService.getTotalTravelDistanceOfCourier("courier123");
        
        // Then
        assertEquals("500,00 mt", formattedDistance);
    }

    @Test
    public void testGetTotalTravelDistance_OfCourier_MoreThan1000M() {
        // Given
        doReturn("formattedDistanceIndex").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        doReturn(Collections.emptyList()).when(h3Core).gridDisk(anyString(), anyInt());

        doReturn(150.0).when(distanceCalculator).calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());
        doReturn(8500.0).when(distanceCalculator).calculateDistance(
                eq(atasehirLocation.getLatitude()), eq(atasehirLocation.getLongitude()),
                eq(ortakoyLocation.getLatitude()), eq(ortakoyLocation.getLongitude()));

        ReflectionTestUtils.setField(courierLocationService, "distanceCalculator", distanceCalculator);

        courierLocationService.processLocation(atasehirLocation);
        courierLocationService.processLocation(ortakoyLocation);
        
        // When
        String formattedDistance = courierLocationService.getTotalTravelDistanceOfCourier("courier123");
        
        // Then
        assertEquals("8,50 km", formattedDistance);
    }

    @Test
    public void testGetCourierLocations() {
        // Given
        doReturn("locationTestIndex").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        doReturn(Collections.emptyList()).when(h3Core).gridDisk(anyString(), anyInt());
        doReturn(150.0).when(distanceCalculator).calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());

        ReflectionTestUtils.setField(courierLocationService, "distanceCalculator", distanceCalculator);

        courierLocationService.processLocation(atasehirLocation);
        courierLocationService.processLocation(ortakoyLocation);
        
        // When
        List<CourierLocation> locations = courierLocationService.getCourierLocations("courier123");
        
        // Then
        assertNotNull(locations);
        assertEquals(2, locations.size());
        assertEquals(atasehirLocation, locations.get(0));
        assertEquals(ortakoyLocation, locations.get(1));
    }

    @Test
    public void testGetCourierLocations_CourierNotFound() {
        // When getting locations for a non-existent courier
        List<CourierLocation> locations = courierLocationService.getCourierLocations("nonexistentCourier");
        // Then
        assertNull(locations);
    }

    @Test
    public void testStoreEntrance() {
        // Given
        doReturn("courierIndex").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        doReturn(Collections.emptyList()).when(h3Core).gridDisk(anyString(), anyInt());

        doReturn(50.0).when(distanceCalculator).calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());
        ReflectionTestUtils.setField(courierLocationService, "distanceCalculator", distanceCalculator);
        
        // When
        courierLocationService.processLocation(atasehirLocation);
        
        Map<String, Map<String, LocalDateTime>> lastEntranceTime =
            (Map<String, Map<String, LocalDateTime>>) ReflectionTestUtils.getField(courierLocationService, "lastEntranceTime");

        // Then
        assertNotNull(lastEntranceTime);
        assertTrue(lastEntranceTime.containsKey("courier123"));
        assertTrue(lastEntranceTime.get("courier123").containsKey("Ataşehir MMM Migros"));
    }

    @Test
    public void testStoreEntrance_WithNeighborH3Cell() {
        // Given
        // H3 calculations for different H3 cells but within neighboring range
        doReturn("courierIndex").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        doReturn("storeIndex").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        doReturn(List.of("storeIndex", "courierIndex")).when(h3Core).gridDisk(eq("storeIndex"), anyInt());

        doReturn(90.0).when(distanceCalculator).calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());

        ReflectionTestUtils.setField(courierLocationService, "distanceCalculator", distanceCalculator);
        
        // When
        courierLocationService.processLocation(atasehirLocation);
        
        Map<String, Map<String, LocalDateTime>> lastEntranceTime =
            (Map<String, Map<String, LocalDateTime>>) ReflectionTestUtils.getField(courierLocationService, "lastEntranceTime");

        // Then
        assertNotNull(lastEntranceTime);
        assertTrue(lastEntranceTime.containsKey("courier123"));
        assertTrue(lastEntranceTime.get("courier123").containsKey("Ataşehir MMM Migros"));
    }

    @Test
    public void testStoreEntrance_Outside100Meters() {
        // Given
        doReturn("courierIndex").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        // NOT include courierIndex in neighbors
        doReturn(Collections.emptyList()).when(h3Core).gridDisk(anyString(), anyInt());
        doReturn(150.0).when(distanceCalculator).calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());

        ReflectionTestUtils.setField(courierLocationService, "distanceCalculator", distanceCalculator);
        
        // When
        courierLocationService.processLocation(atasehirLocation);
        
        Map<String, Map<String, LocalDateTime>> lastEntranceTime =
            (Map<String, Map<String, LocalDateTime>>) ReflectionTestUtils.getField(courierLocationService, "lastEntranceTime");

        // Then
        // courier isn't in the map at all or the store shouldn't be in the courier's map
        if (lastEntranceTime != null && lastEntranceTime.containsKey("courier123")) {
            assertFalse(lastEntranceTime.get("courier123").containsKey("Ataşehir MMM Migros"));
        }
    }

    @Test
    public void testDuplicateStoreEntrance_WithinOneMinute() {
        // Given
        doReturn("sameIndex").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        doReturn(Collections.emptyList()).when(h3Core).gridDisk(anyString(), anyInt());

        doReturn(50.0).when(distanceCalculator).calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());

        ReflectionTestUtils.setField(courierLocationService, "distanceCalculator", distanceCalculator);
        
        // Create two locations with timestamps less than 1 minute
        LocalDateTime now = LocalDateTime.of(2025, 3, 22, 12, 0);
        CourierLocation location1 = new CourierLocation(now, "courier123", 40.9923307, 29.1244229);
        CourierLocation location2 = new CourierLocation(now.plusSeconds(30), "courier123", 40.9923307, 29.1244229);
        
        // Process the first location - it should register an entrance
        courierLocationService.processLocation(location1);
        
        // Process the second location - it should NOT register another entrance
        courierLocationService.processLocation(location2);
        

        Map<String, Map<String, LocalDateTime>> lastEntranceTime = 
            (Map<String, Map<String, LocalDateTime>>) ReflectionTestUtils.getField(courierLocationService, "lastEntranceTime");

        // Then
        assertNotNull(lastEntranceTime);
        assertTrue(lastEntranceTime.containsKey("courier123"));
        
        // The entrance time should be from the first entrance
        LocalDateTime storedTime = lastEntranceTime.get("courier123").get("Ataşehir MMM Migros");
        assertEquals(location1.getTime(), storedTime);
    }

    @Test
    public void testDuplicateStoreEntrance_AfterOneMinute() {
        // Given
        doReturn("sameIndex").when(h3Core).latLngToCellAddress(anyDouble(), anyDouble(), anyInt());
        doReturn(Collections.emptyList()).when(h3Core).gridDisk(anyString(), anyInt());
        doReturn(50.0).when(distanceCalculator).calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());

        ReflectionTestUtils.setField(courierLocationService, "distanceCalculator", distanceCalculator);
        
        // Create two locations with timestamps more than 1 minute apart
        LocalDateTime now = LocalDateTime.of(2025, 3, 22, 13, 0);
        CourierLocation location1 = new CourierLocation(now, "courier123", 40.9923307, 29.1244229);
        CourierLocation location2 = new CourierLocation(now.plusMinutes(2), "courier123", 40.9923307, 29.1244229);

        courierLocationService.processLocation(location1);
        courierLocationService.processLocation(location2);

        Map<String, Map<String, LocalDateTime>> lastEntranceTime =
                (Map<String, Map<String, LocalDateTime>>) ReflectionTestUtils.getField(courierLocationService, "lastEntranceTime");
        
        // Verify lastEntranceTime was updated to the second entrance
        assertNotNull(lastEntranceTime);
        assertTrue(lastEntranceTime.containsKey("courier123"));
        LocalDateTime storedTime = lastEntranceTime.get("courier123").get("Ataşehir MMM Migros");
        assertEquals(location2.getTime(), storedTime);
    }
} 