package com.opentable.reservation.dto;

import com.opentable.reservation.model.Reservation;
import com.opentable.reservation.model.ReservationStatus;
import com.opentable.reservation.model.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Reservation representation returned by the API")
public record ReservationResponse(
        @Schema(description = "Reservation identifier") UUID id,
        @Schema(description = "Room identifier") UUID roomId,
        @Schema(description = "Restaurant identifier") UUID restaurantId,
        @Schema(description = "Reservation date") LocalDate reservationDate,
        @Schema(description = "Time slot booked") TimeSlot timeSlot,
        @Schema(description = "Party size") int partySize,
        @Schema(description = "Current reservation status") ReservationStatus status,
        @Schema(description = "Diner name") String dinerName,
        @Schema(description = "Diner email") String dinerEmail,
        @Schema(description = "Diner phone number") String dinerPhone,
        @Schema(description = "Special requests recorded for the reservation") String specialRequests,
        @Schema(description = "Creation timestamp") OffsetDateTime createdAt,
        @Schema(description = "Last update timestamp") OffsetDateTime updatedAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
              reservation.getRoom().getId(),
                reservation.getRestaurant().getId(),
                reservation.getReservationDate(),
                reservation.getTimeSlot(),
                reservation.getPartySize(),
                reservation.getStatus(),
                reservation.getDinerName(),
                reservation.getDinerEmail(),
                reservation.getDinerPhone(),
                reservation.getSpecialRequests(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}