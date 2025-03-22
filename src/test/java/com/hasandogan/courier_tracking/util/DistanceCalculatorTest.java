package com.hasandogan.courier_tracking.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class DistanceCalculatorTest {

    private DistanceCalculator distanceCalculator;

    @BeforeEach
    public void setup() {
        distanceCalculator = new DistanceCalculator();
    }

    @Test
    public void testCalculateDistance_SamePoint() {
        // Calculate distance from a point to itself
        double distance = distanceCalculator.calculateDistance(40.9923307, 29.1244229, 40.9923307, 29.1244229);

        // Should be 0
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    public void testCalculateDistance_KnownDistance() {
        // Calculate distance between known points
        double distance = distanceCalculator.calculateDistance(
                40.9923307, 29.1244229, // Ataşehir
                41.055783, 29.0210292   // Ortaköy
        );

        // Should be between 11 km and 12 km
        assertTrue(distance > 11000 && distance < 12000);
    }

    @Test
    public void testCalculateDistance_ShortDistance() {
        // Two points approximately 100 meters apart
        double distance = distanceCalculator.calculateDistance(
                40.9923307, 29.1244229,
                40.9931307, 29.1244229  // ~100m north
        );

        // Should be close to 100 meters
        assertTrue(distance > 80 && distance < 120);
    }

    @Test
    public void testCalculateDistance_LatitudeOnly() {
        // Points with same longitude, 1 degree of latitude apart (approximately 111 km)
        double distance = distanceCalculator.calculateDistance(
                40.0, 29.0,
                41.0, 29.0
        );

        // Should be close to 111 km
        assertTrue(distance > 110000 && distance < 112000);
    }

    @Test
    public void testCalculateDistance_LongitudeOnly() {
        // Points with same latitude, 1 degree of longitude apart (approximately 85 km at latitude 40)
        double distance = distanceCalculator.calculateDistance(
                40.0, 29.0,
                40.0, 30.0
        );

        // Should be close to 85 km at latitude 40
        assertTrue(distance > 80000 && distance < 90000);
    }

    @Test
    public void testCalculateDistance_NegativeCoordinates() {
        // Points in southern and western hemispheres
        double distance = distanceCalculator.calculateDistance(
                -33.8688, -171.2093, // Approximate coordinates for Sydney
                -39.9006, -174.8860  // Approximate coordinates for Wellington
        );

        // Should be between 700 km and 750 km
        assertTrue(distance > 700000 && distance < 750000);
    }
}