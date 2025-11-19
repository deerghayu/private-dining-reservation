package com.opentable.reservation.event;

import com.opentable.reservation.model.TimeSlot;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event published when a new reservation is successfully created.
 * This event can be consumed by various listeners to perform asynchronous tasks
 * such as sending confirmation emails, updating analytics, or notifying staff.
 */
@Getter
public class ReservationCreatedEvent {

    private final UUID reservationId;
    private final UUID restaurantId;
    private final UUID roomId;
    private final String roomName;
    private final LocalDate reservationDate;
    private final TimeSlot timeSlot;
    private final int partySize;
    private final String dinerName;
    private final String dinerEmail;
    private final String dinerPhone;
    private final String specialRequests;
    private final OffsetDateTime createdAt;

    public ReservationCreatedEvent(
            UUID reservationId,
            UUID restaurantId,
            UUID roomId,
            String roomName,
            LocalDate reservationDate,
            TimeSlot timeSlot,
            int partySize,
            String dinerName,
            String dinerEmail,
            String dinerPhone,
            String specialRequests,
            OffsetDateTime createdAt
    ) {
        this.reservationId = reservationId;
        this.restaurantId = restaurantId;
        this.roomId = roomId;
        this.roomName = roomName;
        this.reservationDate = reservationDate;
        this.timeSlot = timeSlot;
        this.partySize = partySize;
        this.dinerName = dinerName;
        this.dinerEmail = dinerEmail;
        this.dinerPhone = dinerPhone;
        this.specialRequests = specialRequests;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ReservationCreatedEvent{" +
                "reservationId=" + reservationId +
                ", restaurantId=" + restaurantId +
                ", roomName='" + roomName + '\'' +
                ", reservationDate=" + reservationDate +
                ", timeSlot=" + timeSlot +
                ", dinerEmail='" + dinerEmail + '\'' +
                '}';
    }
}
