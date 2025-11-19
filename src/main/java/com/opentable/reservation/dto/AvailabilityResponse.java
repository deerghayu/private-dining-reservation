package com.opentable.reservation.dto;

import com.opentable.reservation.model.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Availability grid for a room across multiple days")
public record AvailabilityResponse(
        @Schema(description = "Room identifier") UUID roomId,
        @Schema(description = "List of day + slot availability entries") List<DayAvailability> days
) {
    @Schema(description = "Availability for a single date")
    public record DayAvailability(
            @Schema(description = "Date of availability entry", example = "2025-11-27") LocalDate date,
            @Schema(description = "Slots for the given date") List<SlotAvailability> slots
    ) {
    }

    @Schema(description = "Availability of a single slot within a date")
    public record SlotAvailability(
            @Schema(description = "Slot name") TimeSlot slot,
            @Schema(description = "Whether the slot is available") boolean available,
            @Schema(description = "Optional reason if unavailable") String reason
    ) {
    }
}