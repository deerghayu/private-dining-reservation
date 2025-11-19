package com.opentable.reservation.controller;

import com.opentable.reservation.dto.AvailabilityResponse;
import com.opentable.reservation.dto.RoomSummaryResponse;
import com.opentable.reservation.model.Restaurant;
import com.opentable.reservation.model.Room;
import com.opentable.reservation.service.AvailabilityService;
import com.opentable.reservation.service.RestaurantService;
import com.opentable.reservation.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * APIs for restaurant discovery, room listing, and availability.
 */
@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurants", description = "Restaurant and room catalog APIs")
@Slf4j
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final RoomService roomService;
    private final AvailabilityService availabilityService;

    @GetMapping
    @Operation(summary = "List all restaurants", description = "Optionally filter restaurants by city.")
    @ApiResponse(responseCode = "200", description = "List of restaurants")
    public ResponseEntity<List<Restaurant>> listAllRestaurants(@RequestParam(required = false) String city) {
        log.info("Received request to list restaurants with city filter: {}", city);

        List<Restaurant> restaurants = restaurantService.findByCity(city);

        log.info("Returning {} restaurants", restaurants.size());
        return ResponseEntity.ok(restaurants);
    }

    @GetMapping("/{restaurantId}/rooms")
    @Operation(summary = "Rooms for a restaurant", description = "Returns active rooms for a restaurant.")
    @ApiResponse(responseCode = "200", description = "List of rooms for the restaurant")
    public ResponseEntity<List<RoomSummaryResponse>> listRoomsByRestaurant(@PathVariable UUID restaurantId) {
        log.info("Received request to fetch rooms for restaurant {}", restaurantId);

        Restaurant restaurant = restaurantService.getRestaurantById(restaurantId);
        List<RoomSummaryResponse> roomsByRestaurant = roomService.findByRestaurant(restaurant).stream()
                .map(RoomSummaryResponse::from)
                .toList();

        log.info("Returning {} rooms for restaurant {}", roomsByRestaurant.size(), restaurantId);
        return ResponseEntity.ok(roomsByRestaurant);
    }

    @GetMapping("/{restaurantId}/rooms/{roomId}/availability")
    @Operation(
            summary = "Room availability calendar",
            description = "Generates availability for every time slot between startDate and endDate.",
            responses = @ApiResponse(responseCode = "200", description = "Availability grid",
                    content = @Content(schema = @Schema(implementation = AvailabilityResponse.class)))
    )
    @ApiResponse(responseCode = "200", description = "Availability calendar for the room")
    public ResponseEntity<AvailabilityResponse> getRoomAvailability(
            @Parameter(description = "Restaurant identifier", in = ParameterIn.PATH) @PathVariable UUID restaurantId,
            @Parameter(description = "Room identifier", in = ParameterIn.PATH) @PathVariable UUID roomId,
            @Parameter(description = "Start date (inclusive)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.debug("Fetching availability for room {} ({}) from {} to {}", roomId, restaurantId, startDate, endDate);

        Room room = roomService.get(restaurantId, roomId);
        AvailabilityResponse roomAvailability = availabilityService.getAvailability(room.getId(), startDate, endDate);

        log.info("Returning availability for room {} from {} to {}", roomId, startDate, endDate);
        return ResponseEntity.ok(roomAvailability);
    }
}