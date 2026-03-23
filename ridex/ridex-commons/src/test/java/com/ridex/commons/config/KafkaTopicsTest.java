package com.ridex.commons.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicsTest {

    @Test
    void tripEvents_hasCorrectValue() {
        assertThat(KafkaTopics.TRIP_EVENTS).isEqualTo("ridex.trip.events");
    }

    @Test
    void locationUpdates_hasCorrectValue() {
        assertThat(KafkaTopics.LOCATION_UPDATES).isEqualTo("ridex.location.updates");
    }

    @Test
    void matchRequests_hasCorrectValue() {
        assertThat(KafkaTopics.MATCH_REQUESTS).isEqualTo("ridex.match.requests");
    }

    @Test
    void matchResults_hasCorrectValue() {
        assertThat(KafkaTopics.MATCH_RESULTS).isEqualTo("ridex.match.results");
    }

    @Test
    void paymentEvents_hasCorrectValue() {
        assertThat(KafkaTopics.PAYMENT_EVENTS).isEqualTo("ridex.payment.events");
    }

    @Test
    void notificationFanout_hasCorrectValue() {
        assertThat(KafkaTopics.NOTIFICATION_FANOUT).isEqualTo("ridex.notification.fanout");
    }

    @Test
    void allTopics_haveRidexPrefix() {
        assertThat(KafkaTopics.TRIP_EVENTS).startsWith("ridex.");
        assertThat(KafkaTopics.LOCATION_UPDATES).startsWith("ridex.");
        assertThat(KafkaTopics.MATCH_REQUESTS).startsWith("ridex.");
        assertThat(KafkaTopics.MATCH_RESULTS).startsWith("ridex.");
        assertThat(KafkaTopics.PAYMENT_EVENTS).startsWith("ridex.");
        assertThat(KafkaTopics.NOTIFICATION_FANOUT).startsWith("ridex.");
    }
}
