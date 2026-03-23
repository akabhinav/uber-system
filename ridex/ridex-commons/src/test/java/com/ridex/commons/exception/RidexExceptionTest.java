package com.ridex.commons.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RidexExceptionTest {

    @Test
    void ridexException_defaultErrorCode() {
        RidexException ex = new RidexException("something went wrong");
        assertThat(ex.getMessage()).isEqualTo("something went wrong");
        assertThat(ex.getErrorCode()).isEqualTo("RIDEX_ERROR");
    }

    @Test
    void ridexException_customErrorCode() {
        RidexException ex = new RidexException("CUSTOM_CODE", "custom message");
        assertThat(ex.getMessage()).isEqualTo("custom message");
        assertThat(ex.getErrorCode()).isEqualTo("CUSTOM_CODE");
    }

    @Test
    void ridexException_withCause() {
        RuntimeException cause = new RuntimeException("root cause");
        RidexException ex = new RidexException("ERR", "wrapped", cause);
        assertThat(ex.getMessage()).isEqualTo("wrapped");
        assertThat(ex.getErrorCode()).isEqualTo("ERR");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void ridexException_isRuntimeException() {
        assertThat(new RidexException("test")).isInstanceOf(RuntimeException.class);
    }

    // --- TripNotFoundException ---

    @Test
    void tripNotFoundException_extendsRidexException() {
        UUID tripId = UUID.randomUUID();
        TripNotFoundException ex = new TripNotFoundException(tripId);
        assertThat(ex).isInstanceOf(RidexException.class);
    }

    @Test
    void tripNotFoundException_setsErrorCode() {
        UUID tripId = UUID.randomUUID();
        TripNotFoundException ex = new TripNotFoundException(tripId);
        assertThat(ex.getErrorCode()).isEqualTo("TRIP_NOT_FOUND");
    }

    @Test
    void tripNotFoundException_containsTripIdInMessage() {
        UUID tripId = UUID.randomUUID();
        TripNotFoundException ex = new TripNotFoundException(tripId);
        assertThat(ex.getMessage()).contains(tripId.toString());
        assertThat(ex.getMessage()).startsWith("Trip not found: ");
    }

    // --- DriverUnavailableException ---

    @Test
    void driverUnavailableException_extendsRidexException() {
        UUID driverId = UUID.randomUUID();
        DriverUnavailableException ex = new DriverUnavailableException(driverId);
        assertThat(ex).isInstanceOf(RidexException.class);
    }

    @Test
    void driverUnavailableException_setsErrorCode() {
        UUID driverId = UUID.randomUUID();
        DriverUnavailableException ex = new DriverUnavailableException(driverId);
        assertThat(ex.getErrorCode()).isEqualTo("DRIVER_UNAVAILABLE");
    }

    @Test
    void driverUnavailableException_containsDriverIdInMessage() {
        UUID driverId = UUID.randomUUID();
        DriverUnavailableException ex = new DriverUnavailableException(driverId);
        assertThat(ex.getMessage()).contains(driverId.toString());
        assertThat(ex.getMessage()).startsWith("Driver unavailable: ");
    }

    // --- PaymentFailedException ---

    @Test
    void paymentFailedException_extendsRidexException() {
        PaymentFailedException ex = new PaymentFailedException("card declined");
        assertThat(ex).isInstanceOf(RidexException.class);
    }

    @Test
    void paymentFailedException_setsErrorCode() {
        PaymentFailedException ex = new PaymentFailedException("insufficient funds");
        assertThat(ex.getErrorCode()).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    void paymentFailedException_containsReasonInMessage() {
        PaymentFailedException ex = new PaymentFailedException("expired card");
        assertThat(ex.getMessage()).isEqualTo("Payment failed: expired card");
    }
}
