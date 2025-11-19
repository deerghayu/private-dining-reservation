package com.opentable.reservation.service;

import com.opentable.reservation.exception.NotFoundException;
import com.opentable.reservation.model.Restaurant;
import com.opentable.reservation.model.Room;
import com.opentable.reservation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing rooms within restaurants.
 */
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    /**
     * Finds all rooms for a given restaurant.
     */
    public List<Room> findByRestaurant(Restaurant restaurant) {
        return roomRepository.findByRestaurant(restaurant);
    }

    /**
     * Retrieves a specific room by its ID and associated restaurant ID.
     * Throws NotFoundException if the room does not exist.
     */
    public Room get(UUID restaurantId, UUID roomId) {
        return roomRepository.findByIdAndRestaurantId(roomId, restaurantId)
                .orElseThrow(() -> new NotFoundException("Room %s not found".formatted(roomId)));
    }
}