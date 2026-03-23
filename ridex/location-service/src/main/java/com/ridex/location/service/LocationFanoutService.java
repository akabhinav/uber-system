package com.ridex.location.service;

import com.ridex.commons.events.LocationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocationFanoutService {
    private static final Logger log = LoggerFactory.getLogger(LocationFanoutService.class);
    private final ConcurrentHashMap<String, Sinks.Many<LocationEvent>> driverSinks = new ConcurrentHashMap<>();

    @Value("${ridex.location.fanout-buffer-size:1024}")
    private int bufferSize;

    public Mono<Void> broadcast(LocationEvent event) {
        Sinks.Many<LocationEvent> sink = driverSinks.get(event.driverId().toString());
        if (sink != null) {
            sink.tryEmitNext(event);
        }
        return Mono.empty();
    }

    public Flux<LocationEvent> subscribe(String driverId) {
        Sinks.Many<LocationEvent> sink = driverSinks.computeIfAbsent(driverId,
            k -> Sinks.many().multicast().onBackpressureBuffer(bufferSize));
        return sink.asFlux()
            .doOnCancel(() -> {
                log.info("Rider unsubscribed from driver: {}", driverId);
                driverSinks.remove(driverId);
            });
    }
}
