package com.hasandogan.courier_tracking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourierLocation {
    private LocalDateTime time;
    private String courierId;
    private double latitude;
    private double longitude;
}