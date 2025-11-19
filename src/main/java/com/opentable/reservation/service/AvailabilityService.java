package com.opentable.reservation.service;

import com.opentable.reservation.dto.AvailabilityResponse;
import com.opentable.reservation.model.Reservation;
import com.opentable.reservation.model.ReservationStatus;
import com.opentable.reservation.model.TimeSlot;
import com.opentable.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Cacheable(value = "availability", key = "#roomId + '_' + #startDate + '_' + #endDate")
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID roomId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating availability for room {} between {} and {}", roomId, startDate, endDate);

        List<Reservation> reservations = reservationRepository.findByRoomIdAndReservationDateBetween(roomId, startDate, endDate);
        Map<LocalDate, List<Reservation>> byDate = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getReservationDate));

        List<AvailabilityResponse.DayAvailability> days = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            List<AvailabilityResponse.SlotAvailability> slots = new ArrayList<>();
            for (TimeSlot slot : EnumSet.allOf(TimeSlot.class)) {
                boolean taken = byDate.getOrDefault(currentDate, List.of()).stream()
                        .anyMatch(res -> res.getTimeSlot() == slot && ACTIVE_STATUSES.contains(res.getStatus()));

                // Note: Reason is null when available. Alternative approaches:
                // 1. Omit null fields (@JsonInclude(NON_NULL))
                // 2. Always provide reason (e.g., "Available for booking")
                // Current approach follows Google/Stripe pattern for schema consistency.
                slots.add(new AvailabilityResponse.SlotAvailability(slot, !taken, taken ? "Already booked" : null));
            }
            days.add(new AvailabilityResponse.DayAvailability(currentDate, slots));
            currentDate = currentDate.plusDays(1);
        }
        return new AvailabilityResponse(roomId, days);
    }

    /**
     * Checks if a specific time slot is available for a room on a given date.
     * This is a simple, non-locking check suitable for read-only operations.
     * <p>
     * Note: For reservation creation, double-booking prevention is handled by the database
     * unique constraint (uk_room_date_slot) rather than application-level locking.
     */
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(UUID roomId, LocalDate date, TimeSlot timeSlot) {
        boolean isSlotAvailable = !reservationRepository.existsByRoomIdAndReservationDateAndTimeSlotAndStatusIn(roomId, date, timeSlot, ACTIVE_STATUSES);
        log.trace("Time Slot check room {} date {} timeSlot {} -> {}", roomId, date, timeSlot, isSlotAvailable);
        return isSlotAvailable;
    }
}