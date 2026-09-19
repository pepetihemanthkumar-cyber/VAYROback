package com.vayro.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vayro.booking.client.VehicleServiceClient;
import com.vayro.booking.dto.*;
import com.vayro.booking.entity.BookingStatus;
import com.vayro.booking.entity.PaymentStatus;
import com.vayro.booking.exception.BookingNotFoundException;
import com.vayro.booking.exception.BookingOverlapException;
import com.vayro.booking.exception.InvalidBookingStateException;
import com.vayro.booking.exception.UnauthorizedBookingAccessException;
import com.vayro.booking.security.AuthenticatedUser;
import com.vayro.booking.security.JwtService;
import com.vayro.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private VehicleServiceClient vehicleServiceClient;

    @Autowired
    private JwtService jwtService;

    private ObjectMapper objectMapper;
    private String userJwt;
    private String adminJwt;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Use standard mock tokens generated or simulated with test jwt secret
        // In this test environment, we can construct standard Bearer auth or mock filter
    }

    private BookingResponse createSampleResponse(Long id, String ref, BookingStatus status) {
        BookingResponse r = new BookingResponse();
        r.setId(id);
        r.setBookingReference(ref);
        r.setVehicleId("veh-001");
        r.setVehicleName("BMW 3 Series");
        r.setUserId("1001");
        r.setCustomerName("Alice Customer");
        r.setCustomerEmail("user@vayro.com");
        r.setPickupDateTime(LocalDateTime.now().plusDays(1));
        r.setReturnDateTime(LocalDateTime.now().plusDays(4));
        r.setRentalDays(3);
        r.setPricePerDay(BigDecimal.valueOf(4500));
        r.setBaseAmount(BigDecimal.valueOf(13500));
        r.setAddOnsAmount(BigDecimal.ZERO);
        r.setDiscountAmount(BigDecimal.ZERO);
        r.setTaxAmount(BigDecimal.valueOf(2430));
        r.setTotalAmount(BigDecimal.valueOf(15930));
        r.setStatus(status);
        r.setPaymentStatus(PaymentStatus.PENDING);
        r.setPickupLocation("Vijayawada Airport");
        r.setReturnLocation("Vijayawada Airport");
        return r;
    }

    @Test
    @DisplayName("GET /api/bookings/availability/{vehicleId} is public and returns 200")
    void testPublicAvailabilityEndpoint() throws Exception {
        when(bookingService.checkAvailability(eq("veh-001"), any(), any()))
                .thenReturn(new AvailabilityResponse("veh-001", true, "Vehicle is available for the requested period."));

        mockMvc.perform(get("/api/bookings/availability/veh-001")
                        .param("pickupDateTime", "2026-09-20T10:00:00")
                        .param("returnDateTime", "2026-09-23T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value("veh-001"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("POST /api/bookings fails with 401 when unauthorized")
    void testCreateBookingUnauthorized() throws Exception {
        BookingCreateRequest req = new BookingCreateRequest(
                "veh-001",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                "HQ", "HQ"
        );

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /actuator/health is public and returns 200")
    void testActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
