package com.opentable.reservation.service;

import com.opentable.reservation.dto.CreateReservationRequest;
import com.opentable.reservation.dto.CreateReservationRequest.Diner;
import com.opentable.reservation.dto.CreateReservationRequest.MonetaryAmount;
import com.opentable.reservation.dto.ReservationResponse;
import com.opentable.reservation.exception.BusinessException;
import com.opentable.reservation.exception.NotFoundException;
import com.opentable.reservation.model.*;
import com.opentable.reservation.repository.ReservationRepository;
import com.opentable.reservation.repository.RoomRepository;
import com.opentable.reservation.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests all business logic including validation, error cases, and event publishing.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReservationService reservationService;

    private Restaurant restaurant;
    private Room room;
    private CreateReservationRequest validRequest;

    @BeforeEach
    void setUp() {
        restaurant = TestDataBuilder.restaurant().build();
        restaurant.setId(UUID.randomUUID());

        room = TestDataBuilder.room()
                .restaurant(restaurant)
                .minCapacity(2)
                .maxCapacity(10)
                .minimumSpend(new BigDecimal("500.00"), "USD")
                .active(true)
                .build();
        room.setId(UUID.randomUUID());

        validRequest = new CreateReservationRequest(
                room.getId(),
                LocalDate.now().plusDays(7),
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                "Window seat preferred",
                new Diner("Sam Smith", "sam.smith@example.com", "+1-555-1234")
        );
    }

    @Test
    void createReservation_WithValidRequest_ShouldSucceed() {
        // Arrange
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        Reservation savedReservation = TestDataBuilder.reservation()
                .room(room)
                .restaurant(restaurant)
                .build();
        savedReservation.setId(UUID.randomUUID());

        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        // Act
        ReservationResponse response = reservationService.createReservation(validRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(savedReservation.getId());
        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);

        // Verify interactions
        verify(roomRepository).findById(room.getId());
        verify(reservationRepository).save(any(Reservation.class));
        verify(eventPublisher).publishEvent(any(Object.class)); // Event published
    }

    @Test
    void createReservation_ShouldSaveWithCorrectData() {
        // Arrange
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        reservationService.createReservation(validRequest);

        // Assert
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());

        Reservation saved = captor.getValue();
        assertThat(saved.getRoom()).isEqualTo(room);
        assertThat(saved.getRestaurant()).isEqualTo(restaurant);
        assertThat(saved.getReservationDate()).isEqualTo(validRequest.reservationDate());
        assertThat(saved.getTimeSlot()).isEqualTo(validRequest.timeSlot());
        assertThat(saved.getPartySize()).isEqualTo(validRequest.partySize());
        assertThat(saved.getDinerName()).isEqualTo(validRequest.diner().name());
        assertThat(saved.getDinerEmail()).isEqualTo(validRequest.diner().email());
        assertThat(saved.getDinerPhone()).isEqualTo(validRequest.diner().phone());
        assertThat(saved.getSpecialRequests()).isEqualTo(validRequest.specialRequests());
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void createReservation_WithNonExistentRoom_ShouldThrowNotFoundException() {
        // Arrange
        when(roomRepository.findById(room.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reservationService.createReservation(validRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Room")
                .hasMessageContaining(room.getId().toString());

        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createReservation_WithInactiveRoom_ShouldThrowBusinessException() {
        // Arrange
        room.setActive(false);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.createReservation(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not accepting reservations");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_WithPartySizeBelowMinimum_ShouldThrowBusinessException() {
        // Arrange
        CreateReservationRequest requestWithSmallParty = new CreateReservationRequest(
                room.getId(),
                validRequest.reservationDate(),
                validRequest.timeSlot(),
                1, // Below minCapacity of 2
                validRequest.estimatedSpend(),
                null,
                validRequest.diner()
        );

        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.createReservation(requestWithSmallParty))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("outside room capacity");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_WithPartySizeAboveMaximum_ShouldThrowBusinessException() {
        // Arrange
        CreateReservationRequest requestWithLargeParty = new CreateReservationRequest(
                room.getId(),
                validRequest.reservationDate(),
                validRequest.timeSlot(),
                15, // Above maxCapacity of 10
                validRequest.estimatedSpend(),
                null,
                validRequest.diner()
        );

        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.createReservation(requestWithLargeParty))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("outside room capacity");
    }

    @Test
    void createReservation_WithSpendBelowMinimum_ShouldThrowBusinessException() {
        // Arrange
        CreateReservationRequest requestWithLowSpend = new CreateReservationRequest(
                room.getId(),
                validRequest.reservationDate(),
                validRequest.timeSlot(),
                4,
                new MonetaryAmount(new BigDecimal("300.00"), "USD"), // Below $500 minimum
                null,
                validRequest.diner()
        );

        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.createReservation(requestWithLowSpend))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("satisfy minimum");
    }

    @Test
    void createReservation_WithWrongCurrency_ShouldThrowBusinessException() {
        // Arrange
        CreateReservationRequest requestWithWrongCurrency = new CreateReservationRequest(
                room.getId(),
                validRequest.reservationDate(),
                validRequest.timeSlot(),
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "EUR"), // Wrong currency
                null,
                validRequest.diner()
        );

        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.createReservation(requestWithWrongCurrency))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be provided in USD");
    }

    @Test
    void cancelReservation_WithValidReservation_ShouldSucceed() {
        // Arrange
        Reservation reservation = TestDataBuilder.reservation()
                .room(room)
                .restaurant(restaurant)
                .reservationDate(LocalDate.now().plusDays(7))
                .status(ReservationStatus.CONFIRMED)
                .build();
        reservation.setId(UUID.randomUUID());

        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // Act
        ReservationResponse response = reservationService.cancelReservation(
                reservation.getId(),
                "user@example.com",
                "Change of plans"
        );

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);

        verify(reservationRepository).save(any(Reservation.class));
        verify(eventPublisher).publishEvent(any(Object.class)); // Cancellation event published
    }

    @Test
    void cancelReservation_ShouldSetCancellationFields() {
        // Arrange
        Reservation reservation = TestDataBuilder.reservation()
                .room(room)
                .restaurant(restaurant)
                .reservationDate(LocalDate.now().plusDays(7))
                .status(ReservationStatus.CONFIRMED)
                .build();
        reservation.setId(UUID.randomUUID());

        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String cancelledBy = "staff@restaurant.com";
        String reason = "Emergency closure";

        // Act
        reservationService.cancelReservation(reservation.getId(), cancelledBy, reason);

        // Assert
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());

        Reservation cancelled = captor.getValue();
        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getCancelledBy()).isEqualTo(cancelledBy);
        assertThat(cancelled.getCancellationReason()).isEqualTo(reason);
        assertThat(cancelled.getCancelledAt()).isNotNull();
    }

    @Test
    void cancelReservation_WhenAlreadyCancelled_ShouldThrowBusinessException() {
        // Arrange
        Reservation reservation = TestDataBuilder.reservation()
                .room(room)
                .restaurant(restaurant)
                .status(ReservationStatus.CANCELLED)
                .build();
        reservation.setId(UUID.randomUUID());

        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.cancelReservation(
                reservation.getId(),
                "user@example.com",
                "Reason"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already cancelled");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_WhenInPast_ShouldThrowBusinessException() {
        // Arrange
        Reservation reservation = TestDataBuilder.reservation()
                .room(room)
                .restaurant(restaurant)
                .reservationDate(LocalDate.now().minusDays(1)) // Past date
                .status(ReservationStatus.CONFIRMED)
                .build();
        reservation.setId(UUID.randomUUID());

        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.cancelReservation(
                reservation.getId(),
                "user@example.com",
                "Reason"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot cancel past reservations");
    }

    @Test
    void listReservationsByDiner_ShouldReturnAllReservations() {
        // Arrange
        String email = "diner@example.com";
        List<Reservation> reservations = List.of(
                TestDataBuilder.reservation().room(room).restaurant(restaurant).dinerEmail(email).build(),
                TestDataBuilder.reservation().room(room).restaurant(restaurant).dinerEmail(email).build()
        );
        Page<Reservation> page = new PageImpl<>(reservations);

        when(reservationRepository.findByDinerEmailIgnoreCase(eq(email), any(PageRequest.class))).thenReturn(page);

        // Act
        Page<ReservationResponse> responses = reservationService.listReservationsByDiner(email, false, PageRequest.of(0, 20));

        // Assert
        assertThat(responses.getContent()).hasSize(2);
        verify(reservationRepository).findByDinerEmailIgnoreCase(eq(email), any(PageRequest.class));
    }

    @Test
    void listReservationsByDiner_WithUpcomingOnly_ShouldCallCorrectMethod() {
        // Arrange
        String email = "diner@example.com";
        Page<Reservation> emptyPage = new PageImpl<>(List.of());
        when(reservationRepository.findUpcomingReservations(eq(email), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(emptyPage);

        // Act
        reservationService.listReservationsByDiner(email, true, PageRequest.of(0, 20));

        // Assert
        verify(reservationRepository).findUpcomingReservations(eq(email), any(LocalDate.class), any(PageRequest.class));
        verify(reservationRepository, never()).findByDinerEmailIgnoreCase(any(), any(PageRequest.class));
    }

    @Test
    void listReservationsByRestaurant_ShouldReturnAllReservations() {
        // Arrange
        UUID restaurantId = restaurant.getId();
        List<Reservation> reservations = List.of(
                TestDataBuilder.reservation().room(room).restaurant(restaurant).build(),
                TestDataBuilder.reservation().room(room).restaurant(restaurant).build()
        );
        Page<Reservation> page = new PageImpl<>(reservations);

        when(reservationRepository.findByRestaurantId(eq(restaurantId), any(PageRequest.class))).thenReturn(page);

        // Act
        Page<ReservationResponse> responses = reservationService.listReservationsByRestaurant(restaurantId, PageRequest.of(0, 20));

        // Assert
        assertThat(responses.getContent()).hasSize(2);
        verify(reservationRepository).findByRestaurantId(eq(restaurantId), any(PageRequest.class));
    }

    @Test
    void getReservationDetails_WithValidId_ShouldReturnReservation() {
        // Arrange
        Reservation reservation = TestDataBuilder.reservation()
                .room(room)
                .restaurant(restaurant)
                .build();
        reservation.setId(UUID.randomUUID());

        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        // Act
        ReservationResponse response = reservationService.getReservationDetails(reservation.getId());

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(reservation.getId());
    }

    @Test
    void getReservationDetails_WithInvalidId_ShouldThrowNotFoundException() {
        // Arrange
        UUID invalidId = UUID.randomUUID();
        when(reservationRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reservationService.getReservationDetails(invalidId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Reservation")
                .hasMessageContaining(invalidId.toString());
    }
}