package com.opentable.reservation.controller;

import com.opentable.reservation.dto.RoomSummaryResponse;
import com.opentable.reservation.model.Restaurant;
import com.opentable.reservation.service.RestaurantService;
import com.opentable.reservation.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * APIs for restaurant discovery, room listing, and availability.
 */
@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurants", description = "Restaurant and room discovery APIs")
@Slf4j
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final RoomService roomService;

    @GetMapping
    @Operation(summary = "List all restaurants", description = "Optionally filter restaurants by city.")
    public List<Restaurant> list(@RequestParam(required = false) String city) {
        log.debug("Listing restaurants (city filter: {})", city);
        return restaurantService.findByCity(city);
    }

    @GetMapping("/{restaurantId}/rooms")
    @Operation(summary = "Rooms for a restaurant", description = "Returns active rooms for a restaurant.")
    public List<RoomSummaryResponse> rooms(@PathVariable UUID restaurantId) {
        log.debug("Fetching rooms for restaurant {}", restaurantId);
        Restaurant restaurant = restaurantService.get(restaurantId);
        return roomService.findByRestaurant(restaurant).stream()
                .map(RoomSummaryResponse::from)
                .toList();
    }
}