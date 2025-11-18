package com.opentable.reservation.service;
import com.opentable.reservation.exception.NotFoundException;
import com.opentable.reservation.model.Restaurant;
import com.opentable.reservation.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public Restaurant get(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant %s not found".formatted(restaurantId)));
    }

    public List<Restaurant> findByCity(String city) {
        return city == null ? restaurantRepository.findAll() : restaurantRepository.findByCityIgnoreCase(city);
    }
}