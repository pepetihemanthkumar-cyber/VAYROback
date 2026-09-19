package com.vayro.notification.service;

import com.vayro.notification.dto.BookingConfirmedNotificationRequest;
import com.vayro.notification.dto.EmailSendResult;
import com.vayro.notification.dto.NotificationResponse;
import com.vayro.notification.dto.VehicleReturnedNotificationRequest;
import com.vayro.notification.entity.Notification;
import com.vayro.notification.entity.NotificationStatus;
import com.vayro.notification.entity.NotificationType;
import com.vayro.notification.provider.GmailEmailProvider;
import com.vayro.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final GmailEmailProvider gmailEmailProvider;
    private final EmailTemplateService emailTemplateService;
    private final PdfInvoiceGenerator pdfInvoiceGenerator;

    @Value("${app.notification.admin-email:admin@vayro.com}")
    private String adminNotificationEmail;

    @Value("${app.notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.notification.email.max-retries:3}")
    private int maxRetries;

    @Value("${app.notification.email.retry-delay-ms:1000}")
    private long retryDelayMs;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            GmailEmailProvider gmailEmailProvider,
            EmailTemplateService emailTemplateService,
            PdfInvoiceGenerator pdfInvoiceGenerator
    ) {
        this.notificationRepository = notificationRepository;
        this.gmailEmailProvider = gmailEmailProvider;
        this.emailTemplateService = emailTemplateService;
        this.pdfInvoiceGenerator = pdfInvoiceGenerator;
    }

    @Override
    @Transactional
    public NotificationResponse processBookingConfirmedNotification(BookingConfirmedNotificationRequest request) {
        if (request == null || request.getBookingReference() == null || request.getBookingReference().isBlank()) {
            throw new IllegalArgumentException("Booking reference is required for notification processing.");
        }

        String ref = request.getBookingReference().trim();
        String idempotencyKey = "BOOKING_CONFIRMED:" + ref;

        // 1. Check idempotency for duplicate prevention
        Optional<Notification> existingOpt = notificationRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            Notification existing = existingOpt.get();
            if (existing.getStatus() == NotificationStatus.SENT || existing.getStatus() == NotificationStatus.SKIPPED) {
                log.info("[IDEMPOTENT_HIT] Booking confirmation notification already processed ({}) for reference: {}", existing.getStatus(), ref);
                return NotificationResponse.fromEntity(existing);
            }
        }

        String recipientEmail = request.getCustomerEmail() != null ? request.getCustomerEmail().trim() : null;
        if (recipientEmail == null || recipientEmail.isEmpty() || !recipientEmail.contains("@")) {
            log.warn("[EMAIL_INVALID] Invalid customer email '{}' for booking {}. Marking notification FAILED.", recipientEmail, ref);
            Notification failedNotification = existingOpt.orElseGet(Notification::new);
            failedNotification.setIdempotencyKey(idempotencyKey);
            failedNotification.setType(NotificationType.BOOKING_CONFIRMED);
            failedNotification.setBookingReference(ref);
            failedNotification.setRecipientEmail(recipientEmail != null ? recipientEmail : "invalid@email.com");
            failedNotification.setRecipientName(request.getCustomerName());
            failedNotification.setRecipientRole("CUSTOMER");
            failedNotification.setSubject("Booking Confirmed: " + request.getVehicleName() + " (Ref: " + ref + ")");
            failedNotification.setMessageContent("FAILED_EMAIL_VALIDATION");
            failedNotification.setStatus(NotificationStatus.FAILED);
            failedNotification.setErrorMessage("Invalid customer email address: " + recipientEmail);
            return NotificationResponse.fromEntity(notificationRepository.save(failedNotification));
        }

        // 2. Generate PDF Invoice
        byte[] pdfBytes = null;
        String attachmentFilename = "VAYRO-Invoice-" + ref + ".pdf";
        try {
            pdfBytes = pdfInvoiceGenerator.generateBookingInvoice(request);
        } catch (Exception e) {
            log.error("[PDF_GEN_ERROR] Failed to generate PDF invoice for booking {}: {}", ref, e.getMessage(), e);
        }

        // 3. Generate HTML email body
        String subject = "VAYRO Booking Confirmed — " + ref;
        String htmlBody = emailTemplateService.generateBookingConfirmedHtml(request);

        // 4. Save notification entity in SENDING state
        Notification notification = existingOpt.orElseGet(Notification::new);
        notification.setIdempotencyKey(idempotencyKey);
        notification.setType(NotificationType.BOOKING_CONFIRMED);
        notification.setBookingReference(ref);
        notification.setRecipientEmail(recipientEmail);
        notification.setRecipientName(request.getCustomerName());
        notification.setRecipientRole("CUSTOMER");
        notification.setSubject(subject);
        notification.setMessageContent(htmlBody);
        notification.setAttachmentName(attachmentFilename);
        if (pdfBytes != null) {
            notification.setPdfInvoiceData(pdfBytes);
        }
        notification.setStatus(NotificationStatus.SENDING);
        notification = notificationRepository.save(notification);

        // 5. Dispatch email via Gmail SMTP with retry
        notification = executeEmailDispatchWithRetry(notification, recipientEmail, request.getCustomerName(), subject, htmlBody, pdfBytes, attachmentFilename);
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public NotificationResponse processVehicleReturnedNotification(VehicleReturnedNotificationRequest request) {
        if (request == null || request.getBookingReference() == null || request.getBookingReference().isBlank()) {
            throw new IllegalArgumentException("Booking reference is required for vehicle return notification.");
        }

        String ref = request.getBookingReference().trim();
        String idempotencyKey = "VEHICLE_RETURNED:" + ref;

        // 1. Check idempotency for duplicate prevention
        Optional<Notification> existingOpt = notificationRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            Notification existing = existingOpt.get();
            if (existing.getStatus() == NotificationStatus.SENT || existing.getStatus() == NotificationStatus.SKIPPED) {
                log.info("[IDEMPOTENT_HIT] Vehicle return notification already processed ({}) for reference: {}", existing.getStatus(), ref);
                return NotificationResponse.fromEntity(existing);
            }
        }

        String adminEmail = (adminNotificationEmail != null && !adminNotificationEmail.trim().isEmpty())
                ? adminNotificationEmail.trim()
                : "admin@vayro.com";

        // 2. Generate HTML email body & subject
        String subject = "VAYRO Vehicle Returned — " + request.getVehicleName() + " — " + ref;
        String htmlBody = emailTemplateService.generateVehicleReturnedAdminHtml(request);

        // 3. Save notification entity in SENDING state
        Notification notification = existingOpt.orElseGet(Notification::new);
        notification.setIdempotencyKey(idempotencyKey);
        notification.setType(NotificationType.VEHICLE_RETURNED);
        notification.setBookingReference(ref);
        notification.setRecipientEmail(adminEmail);
        notification.setRecipientName("VAYRO Admin");
        notification.setRecipientRole("ADMIN");
        notification.setSubject(subject);
        notification.setMessageContent(htmlBody);
        notification.setStatus(NotificationStatus.SENDING);
        notification = notificationRepository.save(notification);

        // 4. Dispatch email via Gmail SMTP with retry
        notification = executeEmailDispatchWithRetry(notification, adminEmail, "VAYRO Admin", subject, htmlBody, null, null);
        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) return null;
        return notificationRepository.findByIdempotencyKey(idempotencyKey)
                .map(NotificationResponse::fromEntity)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByBookingReference(String bookingReference) {
        if (bookingReference == null) return List.of();
        return notificationRepository.findByBookingReference(bookingReference).stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getLatestNotificationForBooking(String bookingReference) {
        if (bookingReference == null) return null;
        return notificationRepository.findFirstByBookingReferenceOrderByCreatedAtDesc(bookingReference)
                .map(NotificationResponse::fromEntity)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPdfInvoiceByBookingReference(String bookingReference) {
        if (bookingReference == null || bookingReference.isBlank()) {
            return null;
        }

        return notificationRepository.findFirstByBookingReferenceOrderByCreatedAtDesc(bookingReference.trim())
                .map(Notification::getPdfInvoiceData)
                .filter(bytes -> bytes != null && bytes.length > 0)
                .orElse(null);
    }

    @Override
    @Transactional
    public NotificationResponse resendBookingConfirmedNotification(String bookingReference) {
        if (bookingReference == null || bookingReference.isBlank()) {
            throw new IllegalArgumentException("Booking reference is required for resending notification.");
        }

        String ref = bookingReference.trim();
        String idempotencyKey = "BOOKING_CONFIRMED:" + ref;

        Optional<Notification> notifOpt = notificationRepository.findByIdempotencyKey(idempotencyKey);
        if (notifOpt.isEmpty()) {
            log.warn("[RESEND_NOT_FOUND] No booking confirmation notification found for reference: {}", ref);
            throw new IllegalArgumentException("No existing confirmation notification found for booking: " + ref);
        }

        Notification notification = notifOpt.get();
        log.info("[RESEND_INITIATED] Initiating deliberate resend for booking reference: {} (Current status: {})", ref, notification.getStatus());

        if (!emailEnabled) {
            log.info("[EMAIL_RESEND_SKIPPED] Resend skipped for {} because NOTIFICATION_EMAIL_ENABLED=false.", notification.getRecipientEmail());
            notification.setStatus(NotificationStatus.SKIPPED);
            notification.setErrorMessage("Email notifications are disabled by configuration (NOTIFICATION_EMAIL_ENABLED=false)");
            return NotificationResponse.fromEntity(notificationRepository.save(notification));
        }

        notification.setStatus(NotificationStatus.SENDING);
        notification = notificationRepository.save(notification);

        byte[] pdfBytes = notification.getPdfInvoiceData();
        String attachmentFilename = notification.getAttachmentName() != null ? notification.getAttachmentName() : ("VAYRO-Invoice-" + ref + ".pdf");

        notification = executeEmailDispatchWithRetry(
                notification,
                notification.getRecipientEmail(),
                notification.getRecipientName(),
                notification.getSubject(),
                notification.getMessageContent(),
                pdfBytes,
                attachmentFilename
        );

        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    private Notification executeEmailDispatchWithRetry(Notification notification, String recipientEmail, String recipientName,
                                                      String subject, String htmlBody, byte[] attachmentPdfBytes, String attachmentFilename) {
        notification.setProvider(gmailEmailProvider.getProviderName());

        if (!emailEnabled) {
            notification.setStatus(NotificationStatus.SKIPPED);
            notification.setProviderMessageId("EMAIL_DISABLED_BY_CONFIG");
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage("Email notifications disabled via NOTIFICATION_EMAIL_ENABLED=false");
            log.info("[EMAIL_SKIPPED] Skipped email delivery to {} because NOTIFICATION_EMAIL_ENABLED=false. Invoice PDF is retained and downloadable.", recipientEmail);
            return notification;
        }

        EmailSendResult lastResult = null;
        int attempts = 0;
        int limit = Math.max(1, maxRetries);

        while (attempts < limit) {
            attempts++;
            notification.setRetryCount(attempts);

            try {
                lastResult = gmailEmailProvider.sendEmail(recipientEmail, recipientName, subject, htmlBody, attachmentPdfBytes, attachmentFilename);
                if (lastResult.isSuccess()) {
                    notification.setStatus(NotificationStatus.SENT);
                    notification.setProviderMessageId(lastResult.getProviderMessageId());
                    notification.setSentAt(LocalDateTime.now());
                    notification.setErrorMessage(null);
                    log.info("[EMAIL_DELIVERED_OK] Email successfully sent to {} via {} on attempt {}", recipientEmail, gmailEmailProvider.getProviderName(), attempts);
                    return notification;
                } else {
                    log.warn("[EMAIL_ATTEMPT_FAILED] Email attempt {} failed to {}: {}", attempts, recipientEmail, lastResult.getErrorMessage());
                }
            } catch (Exception e) {
                log.warn("[EMAIL_EXCEPTION] Email attempt {} exception for {}: {}", attempts, recipientEmail, e.getMessage());
                lastResult = EmailSendResult.failure("Exception: " + e.getMessage(), 500);
            }

            if (attempts < limit && retryDelayMs > 0) {
                try {
                    Thread.sleep(retryDelayMs * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        notification.setStatus(NotificationStatus.FAILED);
        notification.setErrorMessage(lastResult != null ? lastResult.getErrorMessage() : "Email delivery failed after " + attempts + " retries");
        log.error("[EMAIL_PERMANENT_FAIL] Email delivery permanently failed to {} after {} attempts: {}", recipientEmail, attempts, notification.getErrorMessage());
        return notification;
    }
}
