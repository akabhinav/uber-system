package com.ridex.matching.kafka;

import com.ridex.commons.config.KafkaTopics;
import com.ridex.commons.events.MatchRequest;
import com.ridex.matching.service.MatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MatchRequestConsumer {
    private static final Logger log = LoggerFactory.getLogger(MatchRequestConsumer.class);
    private final MatchingService matchingService;

    public MatchRequestConsumer(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @KafkaListener(topics = KafkaTopics.MATCH_REQUESTS, groupId = "matching-service")
    public void onMatchRequest(MatchRequest request) {
        log.info("Received match request for trip={}", request.tripId());
        try {
            matchingService.findMatch(request);
        } catch (Exception e) {
            log.error("Failed to process match request for trip={}", request.tripId(), e);
        }
    }
}
