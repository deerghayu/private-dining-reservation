package com.opentable.reservation.repository;

import com.opentable.reservation.model.Reservation;
import com.opentable.reservation.model.ReservationStatus;
import com.opentable.reservation.model.TimeSlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    Page<Reservation> findByDinerEmailIgnoreCase(String dinerEmail, Pageable pageable);

    @Query("select r from Reservation r where r.dinerEmail = :email and r.reservationDate >= :from")
    Page<Reservation> findUpcomingReservations(@Param("email") String email, @Param("from") LocalDate from, Pageable pageable);

    Page<Reservation> findByRestaurantId(UUID restaurantId, Pageable pageable);

    List<Reservation> findByRoomIdAndReservationDateBetween(UUID roomId, LocalDate start, LocalDate end);
}