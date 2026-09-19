package com.vayro.notification.provider;

import com.vayro.notification.dto.EmailSendResult;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GmailEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(GmailEmailProvider.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:vayro.notifications@gmail.com}")
    private String mailUsername;

    @Value("${app.notification.email.from-name:VAYRO}")
    private String fromName;

    @Value("${app.notification.email.from-address:vayro.notifications@gmail.com}")
    private String fromAddress;

    @Value("${app.notification.email.enabled:false}")
    private boolean emailEnabled;

    public GmailEmailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String getProviderName() {
        return "GMAIL_SMTP";
    }

    @Override
    public EmailSendResult sendEmail(String toEmail, String recipientName, String subject, String htmlBody, byte[] attachmentPdfBytes, String attachmentFilename) {
        if (!emailEnabled) {
            log.warn("[EMAIL_DISABLED] Email notifications are disabled via configuration (app.notification.email.enabled=false). Skipping send to: {}", toEmail);
            return EmailSendResult.failure("Email notifications are disabled by configuration", 400);
        }

        if (toEmail == null || toEmail.trim().isEmpty() || !toEmail.contains("@")) {
            log.error("[EMAIL_INVALID_RECIPIENT] Invalid recipient email address: {}", toEmail);
            return EmailSendResult.failure("Invalid recipient email address: " + toEmail, 400);
        }

        try {
            log.info("[EMAIL_SENDING] Preparing email via Gmail SMTP to: {} (Subject: '{}')", toEmail, subject);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String senderEmail = (fromAddress != null && !fromAddress.trim().isEmpty()) ? fromAddress.trim() : mailUsername;
            if (senderEmail == null || senderEmail.trim().isEmpty()) {
                senderEmail = "noreply@vayro.com";
            }

            helper.setFrom(new InternetAddress(senderEmail, fromName != null ? fromName : "VAYRO"));
            helper.setTo(toEmail.trim());
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            if (attachmentPdfBytes != null && attachmentPdfBytes.length > 0) {
                String fileName = (attachmentFilename != null && !attachmentFilename.trim().isEmpty())
                        ? attachmentFilename.trim()
                        : "VAYRO-Invoice.pdf";
                helper.addAttachment(fileName, new ByteArrayResource(attachmentPdfBytes), "application/pdf");
                log.info("[EMAIL_ATTACHMENT] Attached PDF invoice '{}' ({} bytes)", fileName, attachmentPdfBytes.length);
            }

            mailSender.send(mimeMessage);

            String messageId = mimeMessage.getMessageID();
            if (messageId == null || messageId.trim().isEmpty()) {
                messageId = "gmail-" + UUID.randomUUID().toString();
            }

            log.info("[EMAIL_SENT_SUCCESS] Successfully dispatched email to {} with Message-ID: {}", toEmail, messageId);
            return EmailSendResult.success(messageId);

        } catch (Exception e) {
            log.error("[EMAIL_SEND_FAILED] Failed to send email to {}: {}", toEmail, e.getMessage(), e);
            return EmailSendResult.failure("Gmail SMTP error: " + e.getMessage(), 500);
        }
    }
}
