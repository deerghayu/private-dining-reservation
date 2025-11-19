package com.opentable.reservation.service;

import com.opentable.reservation.dto.AvailabilityResponse;
import com.opentable.reservation.model.Reservation;
import com.opentable.reservation.model.ReservationStatus;
import com.opentable.reservation.model.TimeSlot;
import com.opentable.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service to check availability of time slots for room reservations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {

    private static final List<ReservationStatus> ACTIVE_STATUSES = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    private final ReservationRepository reservationRepository;

    /**
     * Gets the availability of time slots for a room between specified dates.
     */
    public AvailabilityResponse getAvailability(UUID roomId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating availability for room {} between {} and {}", roomId, startDate, endDate);

        List<Reservation> reservations = reservationRepository.findByRoomIdAndReservationDateBetween(roomId, startDate, endDate);
        Map<LocalDate, List<Reservation>> byDate = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getReservationDate));

        List<AvailabilityResponse.DayAvailability> dayAvailability = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            List<AvailabilityResponse.SlotAvailability> slotsAvailability = new ArrayList<>();
            for (TimeSlot slot : EnumSet.allOf(TimeSlot.class)) {
                boolean taken = byDate.getOrDefault(currentDate, List.of()).stream()
                        .anyMatch(res -> res.getTimeSlot() == slot && ACTIVE_STATUSES.contains(res.getStatus()));
                slotsAvailability.add(new AvailabilityResponse.SlotAvailability(slot, !taken, taken ? "Already booked" : null));
            }
            dayAvailability.add(new AvailabilityResponse.DayAvailability(currentDate, slotsAvailability));
            currentDate = currentDate.plusDays(1);
        }
        return new AvailabilityResponse(roomId, dayAvailability);
    }

    /**
     * Checks if a specific time slot is available for a room on a given date.
     */
    public boolean isSlotAvailable(UUID roomId, LocalDate date, TimeSlot timeSlot) {
        boolean isSlotAvailable = !reservationRepository.existsByRoomIdAndReservationDateAndTimeSlotAndStatusIn(roomId, date, timeSlot, ACTIVE_STATUSES);
        log.trace("Time Slot check room {} date {} timeSlot {} -> {}", roomId, date, timeSlot, isSlotAvailable);
        return isSlotAvailable;
    }
}