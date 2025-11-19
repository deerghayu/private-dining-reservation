package com.opentable.reservation.listener;

import com.opentable.reservation.event.ReservationCancelledEvent;
import com.opentable.reservation.event.ReservationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for reservation events and simulates sending notification emails to diners.
 * * In real-world scenario this would integrate with an email service (SendGrid, SES, etc.)
 */
@Slf4j
@Component
public class NotificationEventListener {

    /**
     * Handles reservation created events by simulating sending a confirmation notification.
     */
    @Async
    @EventListener
    public void handleReservationCreated(ReservationCreatedEvent event) {
        log.info("[NOTIFICATION] Sending confirmation email to {} for reservation {} on {} at {}",
                event.getDinerEmail(),
                event.getReservationId(),
                event.getReservationDate(),
                event.getTimeSlot());

        // Simulate email sending
        simulateEmailSending(event.getDinerEmail(), "Reservation Confirmation", buildConfirmationEmailBody(event));

        log.debug("Confirmation email sent successfully for reservation {}", event.getReservationId());
    }

    /**
     * Handles reservation cancelled events by simulating sending a cancellation notification.
     */
    @Async
    @EventListener
    public void handleReservationCancelled(ReservationCancelledEvent event) {
        log.info("[NOTIFICATION] Sending cancellation email to {} for reservation {}",
                event.getDinerEmail(),
                event.getReservationId());

        // Simulate email sending
        simulateEmailSending(event.getDinerEmail(), "Reservation Cancelled", buildCancellationEmailBody(event));

        log.debug("Cancellation email sent successfully for reservation {}", event.getReservationId());
    }

    /**
     * Simulates the process of sending an email. In a real implementation, this would
     * integrate with an external email service provider.
     */
    private void simulateEmailSending(String to, String subject, String body) {
        try {
            // Simulate email service latency
            Thread.sleep(100);
            log.debug("Email sent - To: {}, Subject: {}", to, subject);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email sending interrupted", e);
        }
    }

    private String buildConfirmationEmailBody(ReservationCreatedEvent event) {
        return String.format("""
                        Dear %s,
                        
                        Your reservation at %s has been confirmed!
                        
                        Date: %s
                        Time: %s
                        Party Size: %d
                        Room: %s
                        
                        We look forward to serving you!
                        
                        Reservation ID: %s
                        """,
                event.getDinerName(),
                event.getRoomName(),
                event.getReservationDate(),
                event.getTimeSlot(),
                event.getPartySize(),
                event.getRoomName(),
                event.getReservationId()
        );
    }

    private String buildCancellationEmailBody(ReservationCancelledEvent event) {
        return String.format("""
                        Dear %s,
                        
                        Your reservation at %s has been cancelled.
                        
                        Original Date: %s
                        Original Time: %s
                        Reservation ID: %s
                        
                        We hope to serve you in the future.
                        """,
                event.getDinerName(),
                event.getRoomName(),
                event.getReservationDate(),
                event.getTimeSlot(),
                event.getReservationId()
        );
    }
}