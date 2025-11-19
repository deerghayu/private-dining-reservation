package com.opentable.reservation.service;

import com.opentable.reservation.dto.AvailabilityResponse;
import com.opentable.reservation.model.Reservation;
import com.opentable.reservation.model.ReservationStatus;
import com.opentable.reservation.model.TimeSlot;
import com.opentable.reservation.repository.ReservationRepository;
import com.opentable.reservation.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private UUID roomId;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        testDate = LocalDate.now().plusDays(7);
    }

    @Test
    void isSlotAvailable_WhenNoReservations_ShouldReturnTrue() {
        // Arrange
        when(reservationRepository.existsByRoomIdAndReservationDateAndTimeSlotAndStatusIn(
                eq(roomId), eq(testDate), eq(TimeSlot.DINNER), anyList()
        )).thenReturn(false);

        // Act
        boolean available = availabilityService.isSlotAvailable(roomId, testDate, TimeSlot.DINNER);

        // Assert
        assertThat(available).isTrue();
    }

    @Test
    void isSlotAvailable_WhenSlotBooked_ShouldReturnFalse() {
        // Arrange
        when(reservationRepository.existsByRoomIdAndReservationDateAndTimeSlotAndStatusIn(
                eq(roomId), eq(testDate), eq(TimeSlot.DINNER), anyList()
        )).thenReturn(true);

        // Act
        boolean available = availabilityService.isSlotAvailable(roomId, testDate, TimeSlot.DINNER);

        // Assert
        assertThat(available).isFalse();
    }

    @Test
    void getAvailability_WithNoReservations_ShouldReturnAllSlotsAvailable() {
        // Arrange
        LocalDate startDate = testDate;
        LocalDate endDate = testDate.plusDays(2);

        when(reservationRepository.findByRoomIdAndReservationDateBetween(roomId, startDate, endDate))
                .thenReturn(List.of());

        // Act
        AvailabilityResponse response = availabilityService.getAvailability(roomId, startDate, endDate);

        // Assert
        assertThat(response.roomId()).isEqualTo(roomId);
        assertThat(response.days()).hasSize(3); // 3 days

        // All slots should be available
        response.days().forEach(day -> {
            assertThat(day.slots()).hasSize(TimeSlot.values().length);
            day.slots().forEach(slot -> {
                assertThat(slot.available()).isTrue();
                assertThat(slot.reason()).isNull();
            });
        });
    }

    @Test
    void getAvailability_WithSomeReservations_ShouldMarkThoseSlotsUnavailable() {
        // Arrange
        LocalDate startDate = testDate;
        LocalDate endDate = testDate.plusDays(2);

        Reservation reservation1 = TestDataBuilder.reservation()
                .reservationDate(testDate)
                .timeSlot(TimeSlot.DINNER)
                .status(ReservationStatus.CONFIRMED)
                .build();

        Reservation reservation2 = TestDataBuilder.reservation()
                .reservationDate(testDate.plusDays(1))
                .timeSlot(TimeSlot.LUNCH)
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findByRoomIdAndReservationDateBetween(roomId, startDate, endDate))
                .thenReturn(Arrays.asList(reservation1, reservation2));

        // Act
        AvailabilityResponse response = availabilityService.getAvailability(roomId, startDate, endDate);

        // Assert
        assertThat(response.days()).hasSize(3);

        // Check first day - DINNER should be unavailable
        var day1 = response.days().get(0);
        assertThat(day1.date()).isEqualTo(testDate);
        var dinnerSlot = day1.slots().stream()
                .filter(s -> s.slot() == TimeSlot.DINNER)
                .findFirst()
                .orElseThrow();
        assertThat(dinnerSlot.available()).isFalse();
        assertThat(dinnerSlot.reason()).isEqualTo("Already booked");

        // Check second day - LUNCH should be unavailable
        var day2 = response.days().get(1);
        assertThat(day2.date()).isEqualTo(testDate.plusDays(1));
        var lunchSlot = day2.slots().stream()
                .filter(s -> s.slot() == TimeSlot.LUNCH)
                .findFirst()
                .orElseThrow();
        assertThat(lunchSlot.available()).isFalse();
    }

    @Test
    void getAvailability_WithCancelledReservations_ShouldShowSlotsAsAvailable() {
        // Arrange
        LocalDate startDate = testDate;
        LocalDate endDate = testDate;

        Reservation cancelledReservation = TestDataBuilder.reservation()
                .reservationDate(testDate)
                .timeSlot(TimeSlot.DINNER)
                .status(ReservationStatus.CANCELLED)
                .build();

        when(reservationRepository.findByRoomIdAndReservationDateBetween(roomId, startDate, endDate))
                .thenReturn(List.of(cancelledReservation));

        // Act
        AvailabilityResponse response = availabilityService.getAvailability(roomId, startDate, endDate);

        // Assert
        var day = response.days().get(0);
        var dinnerSlot = day.slots().stream()
                .filter(s -> s.slot() == TimeSlot.DINNER)
                .findFirst()
                .orElseThrow();

        // Cancelled reservations don't block availability
        assertThat(dinnerSlot.available()).isTrue();
    }
}
