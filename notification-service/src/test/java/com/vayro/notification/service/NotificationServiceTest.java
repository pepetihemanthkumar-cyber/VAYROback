package com.vayro.notification.service;

import com.vayro.notification.dto.BookingConfirmedNotificationRequest;
import com.vayro.notification.dto.EmailSendResult;
import com.vayro.notification.dto.NotificationResponse;
import com.vayro.notification.dto.VehicleReturnedNotificationRequest;
import com.vayro.notification.entity.NotificationStatus;
import com.vayro.notification.entity.NotificationType;
import com.vayro.notification.provider.GmailEmailProvider;
import com.vayro.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private GmailEmailProvider gmailEmailProvider;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        when(gmailEmailProvider.getProviderName()).thenReturn("GMAIL_SMTP");
    }

    @Test
    @DisplayName("Requirement 1: Successful booking sends BOOKING_CONFIRMED email with PDF invoice to customer email")
    void shouldProcessBookingConfirmedNotificationSuccessfully() {
        when(gmailEmailProvider.sendEmail(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(EmailSendResult.success("GMAIL-MSG-12345"));

        BookingConfirmedNotificationRequest request = new BookingConfirmedNotificationRequest(
                1001L,
                "VYR-1001",
                "Maruti Swift",
                "Hemanth Kumar",
                "hemanth@example.com",
                "+91 98765 43210",
                LocalDateTime.now().plusDays(1),
                "2026-09-20 10:00 AM",
                LocalDateTime.now().plusDays(3),
                "2026-09-22 10:00 AM",
                "Airport Hub",
                2,
                BigDecimal.valueOf(4500.00),
                BigDecimal.valueOf(1000.00),
                "BOOKING_CONFIRMED:VYR-1001"
        );

        NotificationResponse response = notificationService.processBookingConfirmedNotification(request);

        assertNotNull(response);
        assertEquals("BOOKING_CONFIRMED:VYR-1001", response.getIdempotencyKey());
        assertEquals(NotificationType.BOOKING_CONFIRMED, response.getType());
        assertEquals(NotificationStatus.SENT, response.getStatus());
        assertEquals("hemanth@example.com", response.getRecipientEmail());
        assertEquals("CUSTOMER", response.getRecipientRole());
        assertTrue(response.getMessageContent().contains("Maruti Swift"));
        assertTrue(response.getMessageContent().contains("VYR-1001"));
        assertEquals("GMAIL-MSG-12345", response.getProviderMessageId());
        assertTrue(response.isHasInvoiceAttachment());

        verify(gmailEmailProvider, times(1)).sendEmail(
                eq("hemanth@example.com"),
                eq("Hemanth Kumar"),
                contains("Booking Confirmed"),
                anyString(),
                isNotNull(),
                eq("VAYRO-Invoice-VYR-1001.pdf")
        );
    }

    @Test
    @DisplayName("Requirement 2: Vehicle return sends VEHICLE_RETURNED email strictly to configured admin email")
    void shouldProcessVehicleReturnedNotificationSuccessfully() {
        when(gmailEmailProvider.sendEmail(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(EmailSendResult.success("ADMIN-EMAIL-999"));

        VehicleReturnedNotificationRequest request = new VehicleReturnedNotificationRequest(
                1001L,
                "VYR-1001",
                "Maruti Swift",
                "Hemanth Kumar",
                "hemanth@example.com",
                LocalDateTime.now(),
                "2026-09-22 11:30 AM",
                "AVAILABLE",
                "VEHICLE_RETURNED:VYR-1001"
        );

        NotificationResponse response = notificationService.processVehicleReturnedNotification(request);

        assertNotNull(response);
        assertEquals("VEHICLE_RETURNED:VYR-1001", response.getIdempotencyKey());
        assertEquals(NotificationType.VEHICLE_RETURNED, response.getType());
        assertEquals(NotificationStatus.SENT, response.getStatus());
        assertEquals("admin@vayro.com", response.getRecipientEmail());
        assertEquals("ADMIN", response.getRecipientRole());
        assertTrue(response.getMessageContent().contains("VAYRO ADMIN"));
        assertTrue(response.getMessageContent().contains("Maruti Swift"));

        verify(gmailEmailProvider, times(1)).sendEmail(
                eq("admin@vayro.com"),
                eq("VAYRO Admin"),
                contains("Vehicle Returned"),
                anyString(),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("Duplicate booking notification is prevented (Idempotency)")
    void shouldPreventDuplicateBookingConfirmedNotification() {
        when(gmailEmailProvider.sendEmail(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(EmailSendResult.success("MSG-SINGLE"));

        BookingConfirmedNotificationRequest request = new BookingConfirmedNotificationRequest(
                1002L,
                "VYR-IDEMP-01",
                "Hyundai Creta",
                "User One",
                "user1@example.com",
                null,
                LocalDateTime.now().plusDays(1),
                null,
                LocalDateTime.now().plusDays(2),
                null,
                null,
                1,
                BigDecimal.valueOf(2500),
                null,
                null
        );

        // First call
        NotificationResponse first = notificationService.processBookingConfirmedNotification(request);
        assertEquals(NotificationStatus.SENT, first.getStatus());

        // Second call (retry / duplicate)
        NotificationResponse second = notificationService.processBookingConfirmedNotification(request);
        assertEquals(NotificationStatus.SENT, second.getStatus());
        assertEquals(first.getId(), second.getId());

        // Provider must be invoked exactly once
        verify(gmailEmailProvider, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("PDF invoice is retrievable by booking reference")
    void shouldRetrievePdfInvoiceByBookingReference() {
        when(gmailEmailProvider.sendEmail(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(EmailSendResult.success("MSG-PDF-TEST"));

        BookingConfirmedNotificationRequest request = new BookingConfirmedNotificationRequest(
                1003L,
                "VYR-PDF-01",
                "Mahindra Thar",
                "Adventurer",
                "thar@example.com",
                null,
                LocalDateTime.now().plusDays(1),
                null,
                LocalDateTime.now().plusDays(3),
                null,
                null,
                2,
                BigDecimal.valueOf(7000),
                null,
                null
        );

        notificationService.processBookingConfirmedNotification(request);

        byte[] pdfBytes = notificationService.getPdfInvoiceByBookingReference("VYR-PDF-01");
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
