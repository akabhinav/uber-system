package com.ridex.commons.config;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String TRIP_EVENTS = "ridex.trip.events";
    public static final String LOCATION_UPDATES = "ridex.location.updates";
    public static final String MATCH_REQUESTS = "ridex.match.requests";
    public static final String MATCH_RESULTS = "ridex.match.results";
    public static final String PAYMENT_EVENTS = "ridex.payment.events";
    public static final String NOTIFICATION_FANOUT = "ridex.notification.fanout";
}
