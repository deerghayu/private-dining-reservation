package com.opentable.reservation.controller;

import com.opentable.reservation.dto.CreateReservationRequest;
import com.opentable.reservation.dto.ReservationResponse;
import com.opentable.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reservation created",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Room already booked"),
            @ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        log.info("Received create reservation request for room {} on {}", request.roomId(), request.reservationDate());

        ReservationResponse reservation = reservationService.createReservation(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }
}