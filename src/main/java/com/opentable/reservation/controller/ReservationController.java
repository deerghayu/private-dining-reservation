package com.opentable.reservation.controller;

import com.opentable.reservation.dto.CancelReservationRequest;
import com.opentable.reservation.dto.CreateReservationRequest;
import com.opentable.reservation.dto.ReservationResponse;
import com.opentable.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * REST API controller for reservation operations.
 * Delegates all business rules to {@link ReservationService}.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reservations", description = "Reservation management APIs for diners and staffs")
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping("/reservations")
    @Operation(
            summary = "Create a reservation",
            description = "Creates a reservation for a given room/date/time slot after validating capacity, spend, and availability."
    )
    @ApiResponse(responseCode = "201", description = "Reservation created",
            content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "409", description = "Room already booked")
    @ApiResponse(responseCode = "422", description = "Business rule violation")
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        log.info("Received create reservation request for room {} on {}", request.roomId(), request.reservationDate());

        ReservationResponse reservation = reservationService.createReservation(request);

        log.info("Reservation created with ID {}", reservation.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @GetMapping("/reservations/{id}")
    @Operation(summary = "Get reservation details", description = "Fetches a reservation by id.")
    @ApiResponse(responseCode = "200", description = "Reservation found",
            content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    @ApiResponse(responseCode = "404", description = "Reservation not found")
    public ResponseEntity<ReservationResponse> getReservationDetails(@PathVariable UUID id) {
        log.info("Received request to fetch reservation with ID {}", id);

        ReservationResponse reservationDetails = reservationService.getReservationDetails(id);

        log.info("Fetched reservation details for ID {}", id);
        return ResponseEntity.ok(reservationDetails);
    }

    @PostMapping("/reservations/{id}/cancel")
    @Operation(
            summary = "Cancel a reservation",
            description = "Allows diners or staff to cancel an upcoming reservation.")
    @ApiResponse(responseCode = "200", description = "Reservation cancelled",
            content = @Content(schema = @Schema(implementation = ReservationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Cancellation not allowed")
    @ApiResponse(responseCode = "404", description = "Reservation not found")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @Parameter(description = "Reservation identifier", in = ParameterIn.PATH) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Cancellation details (who cancelled and why)",
                    required = true
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody CancelReservationRequest request
    ) {
        log.info("Received request to cancel reservation with ID {} by {}", id, request.cancelledBy());

        ReservationResponse reservationResponse = reservationService.cancelReservation(
                id,
                request.cancelledBy(),
                request.reason()
        );

        log.info("Reservation {} cancelled by {}", id, request.cancelledBy());
        return ResponseEntity.ok(reservationResponse);
    }

    @GetMapping("/diners/{email}/reservations")
    @Operation(
            summary = "List reservations for a diner",
            description = "Returns paginated historical or upcoming reservations for a diner. Pass upcomingOnly=true to filter future bookings."
    )
    @ApiResponse(responseCode = "200", description = "Reservations returned")
    public ResponseEntity<Page<ReservationResponse>> dinerReservations(
            @Parameter(description = "Diner email address", in = ParameterIn.PATH) @PathVariable String email,
            @Parameter(description = "Only include future reservations") @RequestParam(defaultValue = "false") boolean upcomingOnly,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Received request to list reservations for diner {} (upcomingOnly={}, page={}, size={})", email, upcomingOnly, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reservationDate"));
        Page<ReservationResponse> reservationsByDiner = reservationService.listReservationsByDiner(email, upcomingOnly, pageable);

        log.info("Found {} reservations for diner {} (page {} of {})", reservationsByDiner.getContent().size(), email, page, reservationsByDiner.getTotalPages());
        return ResponseEntity.ok(reservationsByDiner);
    }

    @GetMapping("/restaurants/{restaurantId}/reservations")
    @Operation(summary = "List reservations for a restaurant", description = "Staff endpoint to view reservations for a restaurant.")
    @ApiResponse(responseCode = "200", description = "Reservations returned")
    public ResponseEntity<Page<ReservationResponse>> restaurantReservations(
            @PathVariable UUID restaurantId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Received request to list reservations for restaurant {} (page={}, size={})", restaurantId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reservationDate"));
        Page<ReservationResponse> reservationsByRestaurant = reservationService.listReservationsByRestaurant(restaurantId, pageable);

        log.info("Found {} reservations for restaurant {} (page {} of {})", reservationsByRestaurant.getContent().size(), restaurantId, page, reservationsByRestaurant.getTotalPages());
        return ResponseEntity.ok(reservationsByRestaurant);
    }
}