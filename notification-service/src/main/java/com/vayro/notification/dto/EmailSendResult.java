package com.vayro.notification.dto;

public class EmailSendResult {

    private final boolean success;
    private final String providerMessageId;
    private final String errorMessage;
    private final Integer statusCode;

    private EmailSendResult(boolean success, String providerMessageId, String errorMessage, Integer statusCode) {
        this.success = success;
        this.providerMessageId = providerMessageId;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
    }

    public static EmailSendResult success(String providerMessageId) {
        return new EmailSendResult(true, providerMessageId, null, 200);
    }

    public static EmailSendResult failure(String errorMessage, Integer statusCode) {
        return new EmailSendResult(false, null, errorMessage, statusCode != null ? statusCode : 500);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        return "EmailSendResult{" +
                "success=" + success +
                ", providerMessageId='" + providerMessageId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }
}
