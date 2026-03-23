package com.ridex.trip.statemachine;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
public class TripStateMachineConfig extends StateMachineConfigurerAdapter<TripState, TripEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<TripState, TripEvent> states) throws Exception {
        states.withStates()
            .initial(TripState.REQUESTED)
            .end(TripState.COMPLETED)
            .end(TripState.CANCELLED)
            .states(EnumSet.allOf(TripState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<TripState, TripEvent> transitions) throws Exception {
        transitions
            .withExternal().source(TripState.REQUESTED).target(TripState.MATCHED).event(TripEvent.MATCH_FOUND)
            .and()
            .withExternal().source(TripState.MATCHED).target(TripState.DRIVER_EN_ROUTE).event(TripEvent.DRIVER_ARRIVING)
            .and()
            .withExternal().source(TripState.DRIVER_EN_ROUTE).target(TripState.PICKUP).event(TripEvent.DRIVER_ARRIVED)
            .and()
            .withExternal().source(TripState.PICKUP).target(TripState.IN_PROGRESS).event(TripEvent.START_TRIP)
            .and()
            .withExternal().source(TripState.IN_PROGRESS).target(TripState.COMPLETED).event(TripEvent.COMPLETE_TRIP)
            .and()
            .withExternal().source(TripState.REQUESTED).target(TripState.CANCELLED).event(TripEvent.CANCEL_TRIP)
            .and()
            .withExternal().source(TripState.MATCHED).target(TripState.CANCELLED).event(TripEvent.CANCEL_TRIP)
            .and()
            .withExternal().source(TripState.DRIVER_EN_ROUTE).target(TripState.CANCELLED).event(TripEvent.CANCEL_TRIP);
    }
}
