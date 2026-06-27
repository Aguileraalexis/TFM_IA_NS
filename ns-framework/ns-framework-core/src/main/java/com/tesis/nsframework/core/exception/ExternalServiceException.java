package com.tesis.nsframework.core.exception;

public class ExternalServiceException extends FrameworkException {
    private final String serviceName;
    private final int suggestedHttpStatus;

    public ExternalServiceException(String serviceName, int suggestedHttpStatus, String message) {
        super(message);
        this.serviceName = serviceName;
        this.suggestedHttpStatus = suggestedHttpStatus;
    }

    public ExternalServiceException(String serviceName, int suggestedHttpStatus, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.suggestedHttpStatus = suggestedHttpStatus;
    }

    public String serviceName() {
        return serviceName;
    }

    public int suggestedHttpStatus() {
        return suggestedHttpStatus;
    }

    public static ExternalServiceException unavailable(String serviceName, String message, Throwable cause) {
        return new ExternalServiceException(serviceName, 503, message, cause);
    }

    public static ExternalServiceException timeout(String serviceName, String message, Throwable cause) {
        return new ExternalServiceException(serviceName, 504, message, cause);
    }

    public static ExternalServiceException badGateway(String serviceName, String message, Throwable cause) {
        return new ExternalServiceException(serviceName, 502, message, cause);
    }
}
