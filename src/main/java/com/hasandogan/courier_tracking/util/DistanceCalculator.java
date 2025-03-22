package com.hasandogan.courier_tracking.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371;

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude differences to radians
        double deltaLatitude = Math.toRadians(lat2 - lat1);
        double deltaLongitude = Math.toRadians(lon2 - lon1);

        // Part of the Haversine formula
        double a = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);

        // Calculate the great-circle distance
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceInKilometers = EARTH_RADIUS_KM * c;
        double distanceInMeters = distanceInKilometers * 1000;

        // Round the result to two decimal places
        BigDecimal roundedDistance = new BigDecimal(distanceInMeters).setScale(2, RoundingMode.HALF_UP);
        return roundedDistance.doubleValue();
    }
}