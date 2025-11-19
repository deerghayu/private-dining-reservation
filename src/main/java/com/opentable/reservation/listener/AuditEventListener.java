package com.opentable.reservation.listener;

import com.opentable.reservation.event.ReservationCancelledEvent;
import com.opentable.reservation.event.ReservationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Listens for reservation events and creates audit logs.
 */
@Slf4j
@Component
public class AuditEventListener {

    /**
     * Audits reservation creation events.
     */
    @Async
    @EventListener
    public void auditReservationCreated(ReservationCreatedEvent event) {
        String auditEntry = String.format(
                "[AUDIT] RESERVATION_CREATED | ID: %s | Restaurant: %s | Room: %s | Date: %s | Time: %s | Diner: %s | Party: %d | Timestamp: %s",
                event.getReservationId(),
                event.getRestaurantId(),
                event.getRoomName(),
                event.getReservationDate(),
                event.getTimeSlot(),
                event.getDinerEmail(),
                event.getPartySize(),
                event.getCreatedAt()
        );

        log.info(auditEntry);

        // In production, persist to audit table:
    }

    /**
     * Audits reservation cancellation events.
     */
    @Async
    @EventListener
    public void auditReservationCancelled(ReservationCancelledEvent event) {
        String auditEntry = String.format(
                "[AUDIT] RESERVATION_CANCELLED | ID: %s | Restaurant: %s | Room: %s | Date: %s | Time: %s | Cancelled By: %s | Reason: %s | Timestamp: %s",
                event.getReservationId(),
                event.getRestaurantId(),
                event.getRoomName(),
                event.getReservationDate(),
                event.getTimeSlot(),
                event.getCancelledBy(),
                event.getCancellationReason(),
                event.getCancelledAt()
        );

        log.info(auditEntry);

        // In production, persist to audit table
    }
}