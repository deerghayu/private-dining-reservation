package com.opentable.reservation.repository;

import com.opentable.reservation.model.Restaurant;
import com.opentable.reservation.model.Room;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    @EntityGraph(attributePaths = "restaurant")
    List<Room> findByRestaurant(Restaurant restaurant);

    Optional<Room> findByIdAndRestaurantId(UUID roomId, UUID restaurantId);
}