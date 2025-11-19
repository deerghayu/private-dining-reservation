package com.opentable.reservation.integration;

import com.opentable.reservation.dto.CreateReservationRequest;
import com.opentable.reservation.dto.CreateReservationRequest.Diner;
import com.opentable.reservation.dto.CreateReservationRequest.MonetaryAmount;
import com.opentable.reservation.event.ReservationCancelledEvent;
import com.opentable.reservation.event.ReservationCreatedEvent;
import com.opentable.reservation.model.*;
import com.opentable.reservation.repository.ReservationRepository;
import com.opentable.reservation.repository.RestaurantRepository;
import com.opentable.reservation.repository.RoomRepository;
import com.opentable.reservation.service.ReservationService;
import com.opentable.reservation.testutil.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to verify that reservation-related events are published correctly
 * and can be listened to by event listeners.
 * This test uses a TestEventListener bean to capture events for verification.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({EventPublishingIntegrationTest.TestEventListenerConfig.class, com.opentable.reservation.TestContainersConfiguration.class})
class EventPublishingIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TestEventListener testEventListener;

    private Restaurant restaurant;
    private Room room;

    @BeforeEach
    void setUp() {
        // Clean up
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        restaurantRepository.deleteAll();
        testEventListener.reset();

        // Create test data
        restaurant = TestDataBuilder.restaurant()
                .name("Event Test Restaurant")
                .build();
        restaurant = restaurantRepository.save(restaurant);

        room = TestDataBuilder.room()
                .restaurant(restaurant)
                .name("Event Test Room")
                .minCapacity(2)
                .maxCapacity(10)
                .minimumSpend(new BigDecimal("500.00"), "USD")
                .build();
        room = roomRepository.save(room);
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        restaurantRepository.deleteAll();
        testEventListener.reset();
    }

    @Test
    void createReservation_ShouldPublishReservationCreatedEvent() throws InterruptedException {
        // Arrange
        CreateReservationRequest request = new CreateReservationRequest(
                room.getId(),
                LocalDate.now().plusDays(7),
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                null,
                new Diner("Sam Smith", "sam.smith@example.com", "+1-555-1234")
        );

        // Act
        reservationService.createReservation(request);

        // Assert - wait for async event processing
        boolean eventReceived = testEventListener.createdEventLatch.await(5, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        assertThat(testEventListener.createdEventCount.get()).isEqualTo(1);
        assertThat(testEventListener.lastCreatedEvent).isNotNull();
        assertThat(testEventListener.lastCreatedEvent.getDinerEmail()).isEqualTo("sam.smith@example.com");
    }

    @Test
    void cancelReservation_ShouldPublishReservationCancelledEvent() throws InterruptedException {
        // Arrange - create a reservation first
        CreateReservationRequest request = new CreateReservationRequest(
                room.getId(),
                LocalDate.now().plusDays(7),
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                null,
                new Diner("Sam Smith", "sam.smith@example.com", "+1-555-1234")
        );
        var reservation = reservationService.createReservation(request);

        // Wait for creation event
        testEventListener.createdEventLatch.await(5, TimeUnit.SECONDS);
        testEventListener.reset();

        // Act - cancel the reservation
        reservationService.cancelReservation(
                reservation.id(),
                "sam.smith@example.com",
                "Plans changed"
        );

        // Assert - wait for async event processing
        boolean eventReceived = testEventListener.cancelledEventLatch.await(5, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        assertThat(testEventListener.cancelledEventCount.get()).isEqualTo(1);
        assertThat(testEventListener.lastCancelledEvent).isNotNull();
        assertThat(testEventListener.lastCancelledEvent.getCancelledBy()).isEqualTo("sam.smith@example.com");
    }

    @Test
    void multipleReservations_ShouldPublishMultipleEvents() throws InterruptedException {
        // Arrange
        int numberOfReservations = 5;
        testEventListener.createdEventLatch = new CountDownLatch(numberOfReservations);

        // Act - create multiple reservations
        for (int i = 0; i < numberOfReservations; i++) {
            CreateReservationRequest request = new CreateReservationRequest(
                    room.getId(),
                    LocalDate.now().plusDays(7 + i), // Different dates
                    TimeSlot.DINNER,
                    4,
                    new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                    null,
                    new Diner("User " + i, "user" + i + "@example.com", "+1-555-000" + i)
            );
            reservationService.createReservation(request);
        }

        // Assert - wait for all events
        boolean allEventsReceived = testEventListener.createdEventLatch.await(10, TimeUnit.SECONDS);
        assertThat(allEventsReceived).isTrue();
        assertThat(testEventListener.createdEventCount.get()).isEqualTo(numberOfReservations);
    }

    @Test
    void events_ShouldBeProcessedAsynchronously() {
        // Arrange
        CreateReservationRequest request = new CreateReservationRequest(
                room.getId(),
                LocalDate.now().plusDays(7),
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                null,
                new Diner("Sam Smith", "sam.smith@example.com", "+1-555-1234")
        );

        long startTime = System.currentTimeMillis();

        // Act
        reservationService.createReservation(request);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert - creation should be fast (async event processing doesn't block)
        // Even with simulated delays in event handlers, this should complete quickly
        assertThat(duration).isLessThan(2000); // Less than 2 seconds
    }

    @Test
    void eventData_ShouldContainAllRelevantInformation() throws InterruptedException {
        // Arrange
        CreateReservationRequest request = new CreateReservationRequest(
                room.getId(),
                LocalDate.now().plusDays(7),
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                "Window seat please",
                new Diner("Sam Smith", "sam.smith@example.com", "+1-555-1234")
        );

        // Act
        var reservation = reservationService.createReservation(request);

        // Assert
        boolean eventReceived = testEventListener.createdEventLatch.await(5, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();

        ReservationCreatedEvent event = testEventListener.lastCreatedEvent;
        assertThat(event).isNotNull();
        assertThat(event.getReservationId()).isEqualTo(reservation.id());
        assertThat(event.getRestaurantId()).isEqualTo(restaurant.getId());
        assertThat(event.getRoomName()).isEqualTo(room.getName());
        assertThat(event.getReservationDate()).isEqualTo(request.reservationDate());
        assertThat(event.getTimeSlot()).isEqualTo(TimeSlot.DINNER);
        assertThat(event.getDinerEmail()).isEqualTo("sam.smith@example.com");
    }

    /**
     * Test configuration to provide the test event listener as a bean
     */
    @TestConfiguration
    static class TestEventListenerConfig {
        @Bean
        public TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }

    /**
     * Test event listener that captures events for verification.
     * This listener runs synchronously (no @Async) to ensure events are captured reliably.
     */
    static class TestEventListener {
        AtomicInteger createdEventCount = new AtomicInteger(0);
        AtomicInteger cancelledEventCount = new AtomicInteger(0);
        CountDownLatch createdEventLatch = new CountDownLatch(1);
        CountDownLatch cancelledEventLatch = new CountDownLatch(1);
        ReservationCreatedEvent lastCreatedEvent;
        ReservationCancelledEvent lastCancelledEvent;

        @EventListener
        public void onReservationCreated(ReservationCreatedEvent event) {
            createdEventCount.incrementAndGet();
            lastCreatedEvent = event;
            createdEventLatch.countDown();
        }

        @EventListener
        public void onReservationCancelled(ReservationCancelledEvent event) {
            cancelledEventCount.incrementAndGet();
            lastCancelledEvent = event;
            cancelledEventLatch.countDown();
        }

        void reset() {
            createdEventCount.set(0);
            cancelledEventCount.set(0);
            createdEventLatch = new CountDownLatch(1);
            cancelledEventLatch = new CountDownLatch(1);
            lastCreatedEvent = null;
            lastCancelledEvent = null;
        }
    }
}