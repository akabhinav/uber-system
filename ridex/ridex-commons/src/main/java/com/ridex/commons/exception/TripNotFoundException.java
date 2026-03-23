package com.ridex.commons.exception;

import java.util.UUID;

public class TripNotFoundException extends RidexException {
    public TripNotFoundException(UUID tripId) {
        super("TRIP_NOT_FOUND", "Trip not found: " + tripId);
    }
}
