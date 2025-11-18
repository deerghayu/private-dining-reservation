package com.opentable.reservation.repository;

import com.opentable.reservation.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    List<Restaurant> findByCityIgnoreCase(String city);
}