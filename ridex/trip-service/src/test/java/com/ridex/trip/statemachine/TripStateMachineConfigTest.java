package com.ridex.trip.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = TripStateMachineConfig.class)
@SpringBootTest(classes = TripStateMachineConfig.class)
class TripStateMachineConfigTest {

    @Autowired
    private StateMachineFactory<TripState, TripEvent> stateMachineFactory;

    private StateMachine<TripState, TripEvent> stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = stateMachineFactory.getStateMachine();
        stateMachine.startReactively().block();
    }

    private void sendEvent(TripEvent event) {
        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(event).build()))
            .blockLast();
    }

    private TripState currentState() {
        return stateMachine.getState().getId();
    }

    @Nested
    @DisplayName("Initial state")
    class InitialState {

        @Test
        @DisplayName("should start in REQUESTED state")
        void shouldStartInRequestedState() {
            assertThat(currentState()).isEqualTo(TripState.REQUESTED);
        }
    }

    @Nested
    @DisplayName("Valid transitions")
    class ValidTransitions {

        @Test
        @DisplayName("REQUESTED -> MATCHED via MATCH_FOUND")
        void requestedToMatched() {
            sendEvent(TripEvent.MATCH_FOUND);
            assertThat(currentState()).isEqualTo(TripState.MATCHED);
        }

        @Test
        @DisplayName("MATCHED -> DRIVER_EN_ROUTE via DRIVER_ARRIVING")
        void matchedToDriverEnRoute() {
            sendEvent(TripEvent.MATCH_FOUND);
            sendEvent(TripEvent.DRIVER_ARRIVING);
            assertThat(currentState()).isEqualTo(TripState.DRIVER_EN_ROUTE);
        }

        @Test
        @DisplayName("DRIVER_EN_ROUTE -> PICKUP via DRIVER_ARRIVED")
        void driverEnRouteToPickup() {
            sendEvent(TripEvent.MATCH_FOUND);
            sendEvent(TripEvent.DRIVER_ARRIVING);
            sendEvent(TripEvent.DRIVER_ARRIVED);
            assertThat(currentState()).isEqualTo(TripState.PICKUP);
        }

        @Test
        @DisplayName("PICKUP -> IN_PROGRESS via START_TRIP")
        void pickupToInProgress() {
            sendEvent(TripEvent.MATCH_FOUND);
            sendEvent(TripEvent.DRIVER_ARRIVING);
            sendEvent(TripEvent.DRIVER_ARRIVED);
            sendEvent(TripEvent.START_TRIP);
            assertThat(currentState()).isEqualTo(TripState.IN_PROGRESS);
        }

        @Test
        @DisplayName("IN_PROGRESS -> COMPLETED via COMPLETE_TRIP")
        void inProgressToCompleted() {
            sendEvent(TripEvent.MATCH_FOUND);
            sendEvent(TripEvent.DRIVER_ARRIVING);
            sendEvent(TripEvent.DRIVER_ARRIVED);
            sendEvent(TripEvent.START_TRIP);
            sendEvent(TripEvent.COMPLETE_TRIP);
            assertThat(currentState()).isEqualTo(TripState.COMPLETED);
        }

        @Test
        @DisplayName("Full happy path: REQUESTED -> COMPLETED")
        void fullHappyPath() {
            sendEvent(TripEvent.MATCH_FOUND);
            assertThat(currentState()).isEqualTo(TripState.MATCHED);

            sendEvent(TripEvent.DRIVER_ARRIVING);
            assertThat(currentState()).isEqualTo(TripState.DRIVER_EN_ROUTE);

            sendEvent(TripEvent.DRIVER_ARRIVED);
            assertThat(currentState()).isEqualTo(TripState.PICKUP);

            sendEvent(TripEvent.START_TRIP);
            assertThat(currentState()).isEqualTo(TripState.IN_PROGRESS);

            sendEvent(TripEvent.COMPLETE_TRIP);
            assertThat(currentState()).isEqualTo(TripState.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Cancellation transitions")
    class CancellationTransitions {

        @Test
        @DisplayName("REQUESTED -> CANCELLED via CANCEL_TRIP")
        void requestedToCancelled() {
            sendEvent(TripEvent.CANCEL_TRIP);
            assertThat(currentState()).isEqualTo(TripState.CANCELLED);
        }

        @Test
        @DisplayName("MATCHED -> CANCELLED via CANCEL_TRIP")
        void matchedToCancelled() {
            sendEvent(TripEvent.MATCH_FOUND);
            assertThat(currentState()).isEqualTo(TripState.MATCHED);

            sendEvent(TripEvent.CANCEL_TRIP);
            assertThat(currentState()).isEqualTo(TripState.CANCELLED);
        }

        @Test
        @DisplayName("DRIVER_EN_ROUTE -> CANCELLED via CANCEL_TRIP")
        void driverEnRouteToCancelled() {
            sendEvent(TripEvent.MATCH_FOUND);
            sendEvent(TripEvent.DRIVER_ARRIVING);
            assertThat(currentState()).isEqualTo(TripState.DRIVER_EN_ROUTE);

            sendEvent(TripEvent.CANCEL_TRIP);
            assertThat(currentState()).isEqualTo(TripState.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("REQUESTED should not accept DRIVER_ARRIVING")
        void requestedShouldNotAcceptDriverArriving() {
            sendEvent(TripEvent.DRIVER_ARRIVING);
            assertThat(currentState()).isEqualTo(TripState.REQUESTED);
        }

        @Test
        @DisplayName("REQUESTED should not accept START_TRIP")
        void requestedShouldNotAcceptStartTrip() {
            sendEvent(TripEvent.START_TRIP);
            assertThat(currentState()).isEqualTo(TripState.REQUESTED);
        }

        @Test
        @DisplayName("REQUESTED should not accept COMPLETE_TRIP")
        void requestedShouldNotAcceptCompleteTrip() {
            sendEvent(TripEvent.COMPLETE_TRIP);
            assertThat(currentState()).isEqualTo(TripState.REQUESTED);
        }

        @Test
        @DisplayName("MATCHED should not accept START_TRIP")
        void matchedShouldNotAcceptStartTrip() {
            sendEvent(TripEvent.MATCH_FOUND);
            assertThat(currentState()).isEqualTo(TripState.MATCHED);

            sendEvent(TripEvent.START_TRIP);
            assertThat(currentState()).isEqualTo(TripState.MATCHED);
        }

        @Test
        @DisplayName("IN_PROGRESS should not accept CANCEL_TRIP (no transition configured)")
        void inProgressShouldNotAcceptCancel() {
            sendEvent(TripEvent.MATCH_FOUND);
            sendEvent(TripEvent.DRIVER_ARRIVING);
            sendEvent(TripEvent.DRIVER_ARRIVED);
            sendEvent(TripEvent.START_TRIP);
            assertThat(currentState()).isEqualTo(TripState.IN_PROGRESS);

            sendEvent(TripEvent.CANCEL_TRIP);
            assertThat(currentState()).isEqualTo(TripState.IN_PROGRESS);
        }

        @Test
        @DisplayName("PICKUP should not accept CANCEL_TRIP (no transition configured)")
        void pickupShouldNotAcceptCancel() {
            sendEvent(TripEvent.MATCH_FOUND);
            sendEvent(TripEvent.DRIVER_ARRIVING);
            sendEvent(TripEvent.DRIVER_ARRIVED);
            assertThat(currentState()).isEqualTo(TripState.PICKUP);

            sendEvent(TripEvent.CANCEL_TRIP);
            assertThat(currentState()).isEqualTo(TripState.PICKUP);
        }

        @Test
        @DisplayName("COMPLETED is terminal - should not accept any events")
        void completedIsTerminal() {
            sendEvent(TripEvent.MATCH_FOUND);
            sendEvent(TripEvent.DRIVER_ARRIVING);
            sendEvent(TripEvent.DRIVER_ARRIVED);
            sendEvent(TripEvent.START_TRIP);
            sendEvent(TripEvent.COMPLETE_TRIP);
            assertThat(currentState()).isEqualTo(TripState.COMPLETED);

            sendEvent(TripEvent.CANCEL_TRIP);
            assertThat(currentState()).isEqualTo(TripState.COMPLETED);
        }

        @Test
        @DisplayName("CANCELLED is terminal - should not accept any events")
        void cancelledIsTerminal() {
            sendEvent(TripEvent.CANCEL_TRIP);
            assertThat(currentState()).isEqualTo(TripState.CANCELLED);

            sendEvent(TripEvent.MATCH_FOUND);
            assertThat(currentState()).isEqualTo(TripState.CANCELLED);
        }
    }
}
