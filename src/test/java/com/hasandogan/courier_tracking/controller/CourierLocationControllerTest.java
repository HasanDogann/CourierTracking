package com.hasandogan.courier_tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hasandogan.courier_tracking.model.CourierLocation;
import com.hasandogan.courier_tracking.service.CourierLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourierLocationController.class)
public class CourierLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourierLocationService courierLocationService;

    @Autowired
    private ObjectMapper objectMapper;

    private CourierLocation atasehirLocation;
    private CourierLocation ortakoyLocation;

    @BeforeEach
    public void setup() {
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
    }

    @Test
    public void testRegisterLocation() throws Exception {
        mockMvc.perform(post("/api/couriers/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atasehirLocation)))
                .andExpect(status().isOk())
                .andExpect(content().string("Location processed successfully"));

        verify(courierLocationService).processLocation(any(CourierLocation.class));
    }

    @Test
    public void testGetTotalDistance() throws Exception {
        when(courierLocationService.getTotalTravelDistanceOfCourier("courier123")).thenReturn("12.34 km");

        mockMvc.perform(get("/api/couriers/courier123/distance"))
                .andExpect(status().isOk())
                .andExpect(content().string("12.34 km"));

        verify(courierLocationService).getTotalTravelDistanceOfCourier("courier123");
    }

    @Test
    public void testGetCourierLocations_WhenLocationsExist() throws Exception {
        List<CourierLocation> locations = new ArrayList<>();
        locations.add(atasehirLocation);
        locations.add(ortakoyLocation);

        when(courierLocationService.getCourierLocations("courier123")).thenReturn(locations);

        mockMvc.perform(get("/api/couriers/courier123/locations"))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(locations)));

        verify(courierLocationService).getCourierLocations("courier123");
    }

    @Test
    public void testGetCourierLocations_WhenNoLocations() throws Exception {
        when(courierLocationService.getCourierLocations("courier123")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/couriers/courier123/locations"))
                .andExpect(status().isOk())
                .andExpect(content().string("No locations found for courier courier123"));

        verify(courierLocationService).getCourierLocations("courier123");
    }
} 