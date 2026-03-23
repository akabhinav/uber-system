package com.ridex.commons.exception;

public class PaymentFailedException extends RidexException {
    public PaymentFailedException(String reason) {
        super("PAYMENT_FAILED", "Payment failed: " + reason);
    }
}
