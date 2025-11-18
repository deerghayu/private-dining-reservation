package com.opentable.reservation.repository;

import com.opentable.reservation.model.Reservation;
import com.opentable.reservation.model.ReservationStatus;
import com.opentable.reservation.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    boolean existsByRoomIdAndReservationDateAndTimeSlotAndStatusIn(
            UUID roomId,
            LocalDate reservationDate,
            TimeSlot timeSlot,
            List<ReservationStatus> statuses
    );
}