package com.opentable.reservation.event;

import com.opentable.reservation.model.TimeSlot;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event published when a reservation is cancelled.
 * This event can be consumed by listeners to send cancellation notifications,
 * update availability caches, or track cancellation metrics.
 */
@Getter
public class ReservationCancelledEvent {

    private final UUID reservationId;
    private final UUID restaurantId;
    private final UUID roomId;
    private final String roomName;
    private final LocalDate reservationDate;
    private final TimeSlot timeSlot;
    private final String dinerName;
    private final String dinerEmail;
    private final String cancelledBy;
    private final String cancellationReason;
    private final OffsetDateTime cancelledAt;

    public ReservationCancelledEvent(
            UUID reservationId,
            UUID restaurantId,
            UUID roomId,
            String roomName,
            LocalDate reservationDate,
            TimeSlot timeSlot,
            String dinerName,
            String dinerEmail,
            String cancelledBy,
            String cancellationReason,
            OffsetDateTime cancelledAt
    ) {
        this.reservationId = reservationId;
        this.restaurantId = restaurantId;
        this.roomId = roomId;
        this.roomName = roomName;
        this.reservationDate = reservationDate;
        this.timeSlot = timeSlot;
        this.dinerName = dinerName;
        this.dinerEmail = dinerEmail;
        this.cancelledBy = cancelledBy;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = cancelledAt;
    }

    @Override
    public String toString() {
        return "ReservationCancelledEvent{" +
                "reservationId=" + reservationId +
                ", restaurantId=" + restaurantId +
                ", roomName='" + roomName + '\'' +
                ", reservationDate=" + reservationDate +
                ", timeSlot=" + timeSlot +
                ", cancelledBy='" + cancelledBy + '\'' +
                ", cancelledAt=" + cancelledAt +
                '}';
    }
}