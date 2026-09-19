package com.vayro.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vayro.notification.dto.BookingConfirmedNotificationRequest;
import com.vayro.notification.dto.NotificationResponse;
import com.vayro.notification.dto.VehicleReturnedNotificationRequest;
import com.vayro.notification.entity.NotificationStatus;
import com.vayro.notification.entity.NotificationType;
import com.vayro.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @Test
    @DisplayName("POST /api/notifications/booking-confirmed should return 200 OK")
    void testBookingConfirmedEndpoint() throws Exception {
        NotificationResponse mockResponse = new NotificationResponse();
        mockResponse.setId(1L);
        mockResponse.setIdempotencyKey("BOOKING_CONFIRMED:VYR-REST-01");
        mockResponse.setType(NotificationType.BOOKING_CONFIRMED);
        mockResponse.setStatus(NotificationStatus.SENT);
        mockResponse.setRecipientEmail("test@example.com");
        mockResponse.setRecipientRole("CUSTOMER");

        when(notificationService.processBookingConfirmedNotification(any(BookingConfirmedNotificationRequest.class)))
                .thenReturn(mockResponse);

        BookingConfirmedNotificationRequest request = new BookingConfirmedNotificationRequest(
                101L,
                "VYR-REST-01",
                "Honda City",
                "Test User",
                "test@example.com",
                "+919876543210",
                LocalDateTime.now().plusDays(1),
                "2026-09-20 10:00 AM",
                LocalDateTime.now().plusDays(2),
                "2026-09-21 10:00 AM",
                "Bangalore Hub",
                1,
                BigDecimal.valueOf(5000),
                BigDecimal.ZERO,
                "BOOKING_CONFIRMED:VYR-REST-01"
        );

        mockMvc.perform(post("/api/notifications/booking-confirmed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotencyKey").value("BOOKING_CONFIRMED:VYR-REST-01"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("POST /api/notifications/vehicle-returned should return 200 OK")
    void testVehicleReturnedEndpoint() throws Exception {
        NotificationResponse mockResponse = new NotificationResponse();
        mockResponse.setId(2L);
        mockResponse.setIdempotencyKey("VEHICLE_RETURNED:VYR-REST-02");
        mockResponse.setType(NotificationType.VEHICLE_RETURNED);
        mockResponse.setStatus(NotificationStatus.SENT);
        mockResponse.setRecipientEmail("admin@vayro.com");
        mockResponse.setRecipientRole("ADMIN");

        when(notificationService.processVehicleReturnedNotification(any(VehicleReturnedNotificationRequest.class)))
                .thenReturn(mockResponse);

        VehicleReturnedNotificationRequest request = new VehicleReturnedNotificationRequest(
                102L,
                "VYR-REST-02",
                "Honda Elevate",
                "Admin User",
                "admin@example.com",
                LocalDateTime.now(),
                "2026-09-21 05:00 PM",
                "AVAILABLE",
                "VEHICLE_RETURNED:VYR-REST-02"
        );

        mockMvc.perform(post("/api/notifications/vehicle-returned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotencyKey").value("VEHICLE_RETURNED:VYR-REST-02"))
                .andExpect(jsonPath("$.recipientRole").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /api/notifications/invoice/{ref} should return PDF bytes")
    void testInvoiceDownloadEndpoint() throws Exception {
        byte[] mockPdf = new byte[]{1, 2, 3, 4, 5};
        when(notificationService.getPdfInvoiceByBookingReference("VYR-REST-01")).thenReturn(mockPdf);

        mockMvc.perform(get("/api/notifications/invoice/VYR-REST-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
