package com.hasandogan.courier_tracking;

import com.uber.h3core.H3Core;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class CourierTrackingApplicationTests {

	@MockBean
	private H3Core h3Core;

	@Test
	void contextLoads() {
		// Mock H3Core methods to prevent NullPointerException
		when(h3Core.latLngToCellAddress(anyDouble(), anyDouble(), anyInt())).thenReturn("testH3Index");
		when(h3Core.gridDisk(anyString(), anyInt())).thenReturn(Collections.emptyList());
	}

}
