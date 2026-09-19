package com.vayro.notification.dto;

import com.vayro.notification.entity.Notification;
import com.vayro.notification.entity.NotificationStatus;
import com.vayro.notification.entity.NotificationType;
import java.time.LocalDateTime;

public class NotificationResponse {

    private Long id;
    private String idempotencyKey;
    private NotificationType type;
    private String bookingReference;
    private String recipientEmail;
    private String recipientName;
    private String recipientRole;
    private String subject;
    private String messageContent;
    private String attachmentName;
    private boolean hasInvoiceAttachment;
    private NotificationStatus status;
    private String provider;
    private String providerMessageId;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    public NotificationResponse() {
    }

    public static NotificationResponse fromEntity(Notification entity) {
        if (entity == null) return null;
        NotificationResponse res = new NotificationResponse();
        res.setId(entity.getId());
        res.setIdempotencyKey(entity.getIdempotencyKey());
        res.setType(entity.getType());
        res.setBookingReference(entity.getBookingReference());
        res.setRecipientEmail(entity.getRecipientEmail());
        res.setRecipientName(entity.getRecipientName());
        res.setRecipientRole(entity.getRecipientRole());
        res.setSubject(entity.getSubject());
        res.setMessageContent(entity.getMessageContent());
        res.setAttachmentName(entity.getAttachmentName());
        res.setHasInvoiceAttachment(entity.getPdfInvoiceData() != null && entity.getPdfInvoiceData().length > 0);
        res.setStatus(entity.getStatus());
        res.setProvider(entity.getProvider());
        res.setProviderMessageId(entity.getProviderMessageId());
        res.setErrorMessage(entity.getErrorMessage());
        res.setRetryCount(entity.getRetryCount());
        res.setSentAt(entity.getSentAt());
        res.setCreatedAt(entity.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public boolean isHasInvoiceAttachment() {
        return hasInvoiceAttachment;
    }

    public void setHasInvoiceAttachment(boolean hasInvoiceAttachment) {
        this.hasInvoiceAttachment = hasInvoiceAttachment;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
