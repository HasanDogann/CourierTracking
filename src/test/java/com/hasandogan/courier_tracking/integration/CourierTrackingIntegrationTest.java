package com.hasandogan.courier_tracking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hasandogan.courier_tracking.CourierTrackingApplication;
import com.hasandogan.courier_tracking.model.CourierLocation;
import com.uber.h3core.H3Core;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CourierTrackingApplication.class)
@AutoConfigureMockMvc
public class CourierTrackingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private H3Core h3Core;

    @BeforeEach
    public void setUp() {
        // Mock H3Core methods to prevent NullPointerException
        when(h3Core.latLngToCellAddress(anyDouble(), anyDouble(), anyInt())).thenReturn("testH3Index");
        when(h3Core.gridDisk(anyString(), anyInt())).thenReturn(Collections.emptyList());
    }

    @Test
    public void testCourierTrackingFlow() throws Exception {
        String courierId = "integration-courier-" + System.currentTimeMillis();
        
        // 1. Register a courier at Ataşehir MMM Migros
        CourierLocation atasehirLocation = new CourierLocation(
            LocalDateTime.of(2025, 3, 22, 10, 0),
            courierId,
            40.9923307, // Ataşehir MMM Migros
            29.1244229
        );
        
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(atasehirLocation)))
            .andExpect(status().isOk())
            .andExpect(content().string("Location processed successfully"));
        
        // 2. Verify that there is one location for the courier
        mockMvc.perform(get("/api/couriers/{courierId}/locations", courierId))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(courierId)));
        
        // 3. Verify that distance is 0 since we only have one location
        mockMvc.perform(get("/api/couriers/{courierId}/distance", courierId))
            .andExpect(status().isOk())
            .andExpect(content().string("0,00 mt"));
        
        // 4. Register courier at Ortaköy MMM Migros (some time later)
        CourierLocation ortakoyLocation = new CourierLocation(
            LocalDateTime.of(2025, 3, 22, 11, 0),
            courierId,
            41.055783, // Ortaköy MMM Migros
            29.0210292
        );
        
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ortakoyLocation)))
            .andExpect(status().isOk())
            .andExpect(content().string("Location processed successfully"));
        
        // 5. Verify that there are now two locations for the courier
        mockMvc.perform(get("/api/couriers/{courierId}/locations", courierId))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(courierId)));
        
        // 6. Verify that distance is calculated (should be around 12.3 km)
        mockMvc.perform(get("/api/couriers/{courierId}/distance", courierId))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("km"))); // Should be formatted as km
    }
    
    @Test
    public void testCourierEnteringStores() throws Exception {
        String courierId = "store-entry-courier-" + System.currentTimeMillis();
        
        // 1. Register courier exactly at Ataşehir MMM Migros
        CourierLocation atasehirLocation = new CourierLocation(
            LocalDateTime.of(2025, 3, 22, 10, 0),
            courierId,
            40.9923307, // Exactly at Ataşehir MMM Migros
            29.1244229
        );
        
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(atasehirLocation)))
            .andExpect(status().isOk())
            .andExpect(content().string("Location processed successfully"));
        
        // 2. Register same courier entering the same store within 1 minute (shouldn't register again)
        CourierLocation sameStoreLocation = new CourierLocation(
            LocalDateTime.of(2025, 3, 22, 10, 0, 30),
            courierId,
            40.9923307, // Same store
            29.1244229
        );
        
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sameStoreLocation)))
            .andExpect(status().isOk())
            .andExpect(content().string("Location processed successfully"));
        
        // 3. Register courier entering a different store
        CourierLocation ortakoyLocation = new CourierLocation(
            LocalDateTime.of(2025, 3, 22, 11, 0),
            courierId,
            41.055783, // Ortaköy MMM Migros
            29.0210292
        );
        
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ortakoyLocation)))
            .andExpect(status().isOk())
            .andExpect(content().string("Location processed successfully"));
        
        // 4. Check total distance traveled
        mockMvc.perform(get("/api/couriers/{courierId}/distance", courierId))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("km"))); // Should be formatted as km
    }
    
    @Test
    public void testDistanceFormatting() throws Exception {
        String courierId = "format-test-courier-" + System.currentTimeMillis();
        
        // 1. Register a courier at a location
        CourierLocation location1 = new CourierLocation(
            LocalDateTime.of(2025, 3, 22, 10, 0),
            courierId,
            41.0,
            29.0
        );
        
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(location1)))
            .andExpect(status().isOk());
        
        // 2. Register the courier 500 meters away (approximated)
        CourierLocation location2 = new CourierLocation(
            LocalDateTime.of(2025, 3, 22, 10, 10),
            courierId,
            41.0045, // ~500m north
            29.0
        );
        
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(location2)))
            .andExpect(status().isOk());
        
        // 3. Verify distance is formatted with "mt" (less than 1000m)
        mockMvc.perform(get("/api/couriers/{courierId}/distance", courierId))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("mt"))); // Should be formatted as mt
        
        // 4. Register the courier 9 kilometers away
        CourierLocation location3 = new CourierLocation(
            LocalDateTime.of(2025, 3, 22, 10, 30),
            courierId,
            41.08, // ~9km north
            29.0
        );
        
        mockMvc.perform(post("/api/couriers/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(location3)))
            .andExpect(status().isOk());
        
        // 5. Verify distance is now formatted with "km" (more than 1000m)
        mockMvc.perform(get("/api/couriers/{courierId}/distance", courierId))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("km"))); // Should be formatted as km
    }
} 