package com.opentable.reservation.listener;

import com.opentable.reservation.event.ReservationCancelledEvent;
import com.opentable.reservation.event.ReservationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener that tracks analytics and business metrics.
 */
@Slf4j
@Component
public class AnalyticsEventListener {

    /**
     * Tracks metrics when a reservation is created.
     * Examples: conversion rates, popular time slots, average party size, etc.
     */
    @Async
    @EventListener
    public void trackReservationCreated(ReservationCreatedEvent event) {
        log.info("[ANALYTICS] Reservation created - Restaurant: {}, Room: {}, TimeSlot: {}, PartySize: {}, Date: {}",
                event.getRestaurantId(),
                event.getRoomName(),
                event.getTimeSlot(),
                event.getPartySize(),
                event.getReservationDate());

        // Simulate sending metrics to analytics service
        trackMetric("reservation.created", 1, event.getRestaurantId());
        trackMetric("reservation.party_size", event.getPartySize(), event.getRestaurantId());
        trackMetric("reservation.time_slot." + event.getTimeSlot(), 1, event.getRestaurantId());
    }

    /**
     * Tracks metrics when a reservation is cancelled.
     * Useful for identifying cancellation patterns and improving service.
     */
    @Async
    @EventListener
    public void trackReservationCancelled(ReservationCancelledEvent event) {
        log.info("[ANALYTICS] Reservation cancelled - Restaurant: {}, Room: {}, CancelledBy: {}",
                event.getRestaurantId(),
                event.getRoomName(),
                event.getCancelledBy());

        // Simulate sending metrics
        trackMetric("reservation.cancelled", 1, event.getRestaurantId());

        // Track cancellation reason for analysis
        if (event.getCancellationReason() != null && !event.getCancellationReason().isBlank()) {
            log.debug("Cancellation reason captured for analytics: {}", event.getCancellationReason());
        }
    }

    /**
     * Simulates sending a metric to an analytics service.
     * In production, this would use Micrometer, StatsD, or a custom metrics client.
     */
    private void trackMetric(String metricName, Number value, Object... tags) {
        log.debug(" Metric tracked: {} = {} (tags: {})", metricName, value, tags);

        // Production implementation:
        // meterRegistry.counter(metricName, Tags.of(convertToTags(tags))).increment(value.doubleValue());
    }
}