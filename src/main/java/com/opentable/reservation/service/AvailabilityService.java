package com.opentable.reservation.service;

import com.opentable.reservation.model.ReservationStatus;
import com.opentable.reservation.model.TimeSlot;
import com.opentable.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service to check availability of time slots for room reservations.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {

    private static final List<ReservationStatus> ACTIVE_STATUSES = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    private final ReservationRepository reservationRepository;

    public boolean isSlotAvailable(UUID roomId, LocalDate date, TimeSlot timeSlot) {
        boolean isAvailable = !reservationRepository.existsByRoomIdAndReservationDateAndTimeSlotAndStatusIn(roomId, date, timeSlot, ACTIVE_STATUSES);
        log.trace("Time Slot check room {} date {} timeSlot {} -> {}", roomId, date, timeSlot, isAvailable);
        return isAvailable;
    }
}