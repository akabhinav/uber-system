package com.ridex.commons.exception;

import java.util.UUID;

public class DriverUnavailableException extends RidexException {
    public DriverUnavailableException(UUID driverId) {
        super("DRIVER_UNAVAILABLE", "Driver unavailable: " + driverId);
    }
}
