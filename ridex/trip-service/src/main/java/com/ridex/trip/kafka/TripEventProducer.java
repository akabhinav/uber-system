package com.ridex.trip.kafka;

import com.ridex.commons.config.KafkaTopics;
import com.ridex.commons.events.TripEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TripEventProducer {
    private static final Logger log = LoggerFactory.getLogger(TripEventProducer.class);
    private final KafkaTemplate<String, TripEvent> kafkaTemplate;

    public TripEventProducer(KafkaTemplate<String, TripEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTripEvent(TripEvent event) {
        log.info("Publishing trip event: tripId={}, status={}", event.tripId(), event.status());
        kafkaTemplate.send(KafkaTopics.TRIP_EVENTS, event.tripId().toString(), event);
    }
}
