package com.ridex.payment.kafka;

import com.ridex.commons.config.KafkaTopics;
import com.ridex.commons.events.TripEvent;
import com.ridex.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TripCompletedConsumer {
    private static final Logger log = LoggerFactory.getLogger(TripCompletedConsumer.class);
    private final PaymentService paymentService;

    public TripCompletedConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = KafkaTopics.TRIP_EVENTS, groupId = "payment-service")
    public void onTripEvent(TripEvent event) {
        if ("COMPLETED".equals(event.status())) {
            log.info("Trip completed, processing payment for trip={}", event.tripId());
            paymentService.processPaymentForTrip(event).subscribe();
        }
    }
}
