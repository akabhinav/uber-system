package com.ridex.location.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridex.location.service.LocationFanoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
public class RiderWebSocketHandler implements WebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(RiderWebSocketHandler.class);
    private final LocationFanoutService fanoutService;
    private final ObjectMapper objectMapper;

    public RiderWebSocketHandler(LocationFanoutService fanoutService, ObjectMapper objectMapper) {
        this.fanoutService = fanoutService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String driverIdParam = session.getHandshakeInfo().getUri().getQuery();
        if (driverIdParam == null || !driverIdParam.startsWith("driverId=")) {
            return session.close();
        }

        String driverId = driverIdParam.substring("driverId=".length());
        log.info("Rider subscribing to driver location: {}", driverId);

        return session.send(
            fanoutService.subscribe(driverId)
                .map(event -> {
                    try {
                        return session.textMessage(objectMapper.writeValueAsString(event));
                    } catch (Exception e) {
                        return session.textMessage("{}");
                    }
                })
        );
    }
}
