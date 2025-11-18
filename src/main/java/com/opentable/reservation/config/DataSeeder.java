package com.opentable.reservation.config;

import com.opentable.reservation.model.Restaurant;
import com.opentable.reservation.model.Room;
import com.opentable.reservation.repository.RestaurantRepository;
import com.opentable.reservation.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds a sample restaurant and room at startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RestaurantRepository restaurantRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (restaurantRepository.count() > 0) {
            return;
        }

        log.info("Seeding sample restaurant and room data for local testing...");

        Restaurant restaurant = Restaurant.builder()
                .name("Mt. Everest Dining")
                .city("San Francisco")
                .state("CA")
                .timezone("America/Los_Angeles")
                .currency("USD")
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        Room.MinimumSpend minimumSpend = new Room.MinimumSpend();
        minimumSpend.setAmount(new BigDecimal("2500"));
        minimumSpend.setCurrency("USD");

        Room room = Room.builder()
                .restaurant(savedRestaurant)
                .name("Mt. Everest Rooftop")
                .roomType(Room.RoomType.ROOFTOP)
                .minCapacity(1)
                .maxCapacity(30)
                .active(true)
                .currency("USD")
                .minimumSpend(minimumSpend)
                .build();

        Room savedRoom = roomRepository.save(room);

        log.info("Seeded restaurant name = '{}' restaurant Id = '{}' with room name = '{}'  room Id = '{}'", savedRestaurant.getName(), savedRestaurant.getId()
                , savedRoom.getName(), savedRoom.getId());
    }
}