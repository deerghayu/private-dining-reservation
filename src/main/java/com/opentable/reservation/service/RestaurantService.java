package com.opentable.reservation.service;

import com.opentable.reservation.exception.NotFoundException;
import com.opentable.reservation.model.Restaurant;
import com.opentable.reservation.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing restaurants.
 */
@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    /**
     * Retrieves a restaurant by its ID.
     * Throws NotFoundException if the restaurant does not exist.
     */
    @Cacheable(value = "restaurants", key = "#restaurantId")
    public Restaurant getRestaurantById(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant %s not found".formatted(restaurantId)));
    }

    /**
     * Finds restaurants by city. If city is null, returns all restaurants.
     */
    @Cacheable(value = "restaurants", key = "#city != null ? #city : 'all'")
    public List<Restaurant> findByCity(String city) {
        return city == null ? restaurantRepository.findAll() : restaurantRepository.findByCityIgnoreCase(city);
    }
}