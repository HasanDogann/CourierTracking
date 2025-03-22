package com.hasandogan.courier_tracking.controller;

import com.hasandogan.courier_tracking.model.CourierLocation;
import com.hasandogan.courier_tracking.service.CourierLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/couriers")
public class CourierLocationController {

    private final CourierLocationService courierLocationService;

    @Autowired
    public CourierLocationController(CourierLocationService courierLocationService) {
        this.courierLocationService = courierLocationService;
    }

    @PostMapping("/location")
    public ResponseEntity<String> registerLocation(@RequestBody CourierLocation location) {
        courierLocationService.processLocation(location);
        return ResponseEntity.ok("Location processed successfully");
    }

    @GetMapping("/{courierId}/distance")
    public ResponseEntity<String> getTotalDistance(@PathVariable String courierId) {
        String formattedDistance = courierLocationService.getTotalTravelDistanceOfCourier(courierId);
        return ResponseEntity.ok(formattedDistance);
    }

    @GetMapping("/{courierId}/locations")
    public ResponseEntity<?> getCourierLocations(@PathVariable String courierId) {
        List<CourierLocation> locations = courierLocationService.getCourierLocations(courierId);
        if (locations == null || locations.isEmpty()) {
            return ResponseEntity.ok("No locations found for courier " + courierId);
        }
        return ResponseEntity.ok(locations);
    }
}
