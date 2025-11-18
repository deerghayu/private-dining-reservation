package com.opentable.reservation.dto;

import com.opentable.reservation.model.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Payload used to create a reservation")
public record CreateReservationRequest(
        @Schema(description = "Room identifier", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID roomId,

        @Schema(description = "Reservation date", example = "2025-11-27", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDate reservationDate,

        @Schema(description = "Time slot for the reservation", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull TimeSlot timeSlot,

        @Schema(description = "Party size", example = "12", minimum = "1")
        @Min(1) int partySize,

        @Schema(description = "Estimated spend for the booking (amount + currency)")
        MonetaryAmount estimatedSpend,

        @Schema(description = "Optional special requests such as dietary restrictions")
        String specialRequests,

        @Valid
        @Schema(description = "Diner placing the reservation", requiredMode = Schema.RequiredMode.REQUIRED)
        Diner diner
) {
    @Schema(description = "Diner contact information")
    public record Diner(
            @Schema(description = "Diner full name", example = "Sam Smith")
            @NotBlank String name,

            @Schema(description = "Diner email address", example = "sam.smith@example.com")
            @NotBlank @Email String email,

            @Schema(description = "Phone number (E.511)", example = "+13087469999")
            @NotBlank String phone
    ) {
    }

    @Schema(description = "Monetary amount representation")
    public record MonetaryAmount(
            @Schema(description = "Amount", example = "1000.00")
            @NotNull BigDecimal amount,

            @Schema(description = "ISO currency code", example = "USD")
            @NotBlank String currency
    ) {
    }
}