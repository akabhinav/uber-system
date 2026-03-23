package com.ridex.location.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridex.commons.events.LocationEvent;
import com.ridex.location.service.LocationFanoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
public class DriverWebSocketHandler implements WebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(DriverWebSocketHandler.class);
    private final LocationFanoutService fanoutService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public DriverWebSocketHandler(LocationFanoutService fanoutService,
                                   ReactiveRedisTemplate<String, String> redisTemplate,
                                   ObjectMapper objectMapper) {
        this.fanoutService = fanoutService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.receive()
            .map(msg -> msg.getPayloadAsText())
            .flatMap(payload -> {
                try {
                    LocationEvent event = objectMapper.readValue(payload, LocationEvent.class);
                    return redisTemplate.opsForGeo()
                        .add("driver:locations",
                            new org.springframework.data.geo.Point(event.lng(), event.lat()),
                            event.driverId().toString())
                        .then(fanoutService.broadcast(event));
                } catch (Exception e) {
                    log.error("Failed to process driver location update", e);
                    return Mono.empty();
                }
            })
            .then();
    }
}
