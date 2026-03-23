package com.ridex.commons.exception;

public class RidexException extends RuntimeException {
    private final String errorCode;

    public RidexException(String message) {
        super(message);
        this.errorCode = "RIDEX_ERROR";
    }

    public RidexException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RidexException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
