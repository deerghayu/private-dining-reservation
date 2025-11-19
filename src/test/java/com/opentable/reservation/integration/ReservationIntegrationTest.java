package com.opentable.reservation.integration;

import com.opentable.reservation.dto.CreateReservationRequest;
import com.opentable.reservation.dto.CreateReservationRequest.Diner;
import com.opentable.reservation.dto.CreateReservationRequest.MonetaryAmount;
import com.opentable.reservation.dto.ReservationResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for end-to-end reservation workflows from service layer through database.
 */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.opentable.reservation.TestContainersConfiguration.class)
class ReservationIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Restaurant restaurant;
    private Room room;

    @BeforeEach
    void setUp() {
        // Clean up
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        restaurantRepository.deleteAll();

        // Create test data
        restaurant = TestDataBuilder.restaurant()
                .name("Integration Test Restaurant")
                .build();
        restaurant = restaurantRepository.save(restaurant);

        room = TestDataBuilder.room()
                .restaurant(restaurant)
                .name("Integration Test Room")
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
    }

    @Test
    void endToEndReservationFlow_CreateAndRetrieve() {
        // Create a reservation
        CreateReservationRequest request = new CreateReservationRequest(
                room.getId(),
                LocalDate.now().plusDays(7),
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                "Integration test reservation",
                new Diner("Integration Test User", "integration@test.com", "+1-555-9999")
        );

        ReservationResponse created = reservationService.createReservation(request);

        // Verify creation
        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(ReservationStatus.CONFIRMED);

        // Retrieve the reservation
        ReservationResponse retrieved = reservationService.getReservationDetails(created.id());

        // Verify all fields
        assertThat(retrieved.id()).isEqualTo(created.id());
        assertThat(retrieved.dinerEmail()).isEqualTo("integration@test.com");
        assertThat(retrieved.dinerName()).isEqualTo("Integration Test User");
        assertThat(retrieved.partySize()).isEqualTo(4);
        assertThat(retrieved.specialRequests()).isEqualTo("Integration test reservation");
    }

    @Test
    void endToEndReservationFlow_CreateAndCancel() {
        // Create a reservation
        CreateReservationRequest request = new CreateReservationRequest(
                room.getId(),
                LocalDate.now().plusDays(7),
                TimeSlot.LUNCH,
                6,
                new MonetaryAmount(new BigDecimal("700.00"), "USD"),
                null,
                new Diner("Cancel Test User", "cancel@test.com", "+1-555-8888")
        );

        ReservationResponse created = reservationService.createReservation(request);

        // Cancel the reservation
        ReservationResponse cancelled = reservationService.cancelReservation(
                created.id(),
                "cancel@test.com",
                "Plans changed"
        );

        // Verify cancellation
        assertThat(cancelled.status()).isEqualTo(ReservationStatus.CANCELLED);

        // Verify cancellation fields in database
        Reservation dbReservation = reservationRepository.findById(cancelled.id()).orElseThrow();
        assertThat(dbReservation.getCancelledBy()).isEqualTo("cancel@test.com");
        assertThat(dbReservation.getCancellationReason()).isEqualTo("Plans changed");
        assertThat(dbReservation.getCancelledAt()).isNotNull();
    }

    @Test
    void endToEndReservationFlow_ListDinerReservations() {
        String email = "multi@test.com";

        // Create multiple reservations for the same diner
        for (int i = 0; i < 3; i++) {
            CreateReservationRequest request = new CreateReservationRequest(
                    room.getId(),
                    LocalDate.now().plusDays(7 + i),
                    TimeSlot.DINNER,
                    4,
                    new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                    null,
                    new Diner("Multi Test User", email, "+1-555-7777")
            );
            reservationService.createReservation(request);
        }

        // Retrieve all reservations
        Page<ReservationResponse> reservations = reservationService.listReservationsByDiner(email, false, PageRequest.of(0, 20));

        // Verify
        assertThat(reservations.getContent()).hasSize(3);
        assertThat(reservations.getContent()).allMatch(r -> r.dinerEmail().equalsIgnoreCase(email));
    }

    @Test
    void endToEndReservationFlow_ListRestaurantReservations() {
        // Create reservations for different rooms in the same restaurant
        Room room2 = TestDataBuilder.room()
                .restaurant(restaurant)
                .name("Second Room")
                .build();
        room2 = roomRepository.save(room2);

        CreateReservationRequest request1 = new CreateReservationRequest(
                room.getId(),
                LocalDate.now().plusDays(7),
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                null,
                new Diner("User 1", "user1@test.com", "+1-555-0001")
        );

        CreateReservationRequest request2 = new CreateReservationRequest(
                room2.getId(),
                LocalDate.now().plusDays(7),
                TimeSlot.DINNER,
                6,
                new MonetaryAmount(new BigDecimal("800.00"), "USD"),
                null,
                new Diner("User 2", "user2@test.com", "+1-555-0002")
        );

        reservationService.createReservation(request1);
        reservationService.createReservation(request2);

        // List all reservations for the restaurant
        Page<ReservationResponse> reservations = reservationService.listReservationsByRestaurant(restaurant.getId(), PageRequest.of(0, 20));

        // Verify
        assertThat(reservations.getContent()).hasSize(2);
    }

    @Test
    void endToEndReservationFlow_VerifyDatabasePersistence() {
        // Create reservation
        CreateReservationRequest request = new CreateReservationRequest(
                room.getId(),
                LocalDate.now().plusDays(7),
                TimeSlot.BREAKFAST,
                2,
                new MonetaryAmount(new BigDecimal("550.00"), "USD"),
                null,
                new Diner("DB Test User", "db@test.com", "+1-555-6666")
        );

        ReservationResponse created = reservationService.createReservation(request);

        // Verify directly in database
        Page<Reservation> dbReservations = reservationRepository.findByDinerEmailIgnoreCase("db@test.com", PageRequest.of(0, 20));

        assertThat(dbReservations.getContent()).hasSize(1);

        Reservation dbReservation = dbReservations.getContent().get(0);
        assertThat(dbReservation.getId()).isEqualTo(created.id());
        assertThat(dbReservation.getRoom().getId()).isEqualTo(room.getId());
        assertThat(dbReservation.getRestaurant().getId()).isEqualTo(restaurant.getId());
        assertThat(dbReservation.getTimeSlot()).isEqualTo(TimeSlot.BREAKFAST);
        assertThat(dbReservation.getPartySize()).isEqualTo(2);
        assertThat(dbReservation.getCreatedAt()).isNotNull();
        assertThat(dbReservation.getUpdatedAt()).isNotNull();
    }
}