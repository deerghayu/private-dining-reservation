package com.opentable.reservation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for cancelling a reservation.
 */
public record CancelReservationRequest(
        @NotBlank(message = "Cancelled by is required")
        String cancelledBy,

        String reason
) {
}