package com.vayro.notification.provider;

import com.vayro.notification.dto.EmailSendResult;

public interface EmailProvider {

    String getProviderName();

    EmailSendResult sendEmail(String toEmail, String recipientName, String subject, String htmlBody, byte[] attachmentPdfBytes, String attachmentFilename);
}
