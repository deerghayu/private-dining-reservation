package com.opentable.reservation.testutil;

import com.opentable.reservation.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Utility class for building test data entities.
 * Provides builder methods with sensible defaults for testing.
 */
public class TestDataBuilder {

    public static RestaurantBuilder restaurant() {
        return new RestaurantBuilder();
    }

    public static RoomBuilder room() {
        return new RoomBuilder();
    }

    public static ReservationBuilder reservation() {
        return new ReservationBuilder();
    }

    public static class RestaurantBuilder {
        private String name = "Test Restaurant";
        private String city = "San Francisco";
        private String state = "CA";
        private String timezone = "America/Los_Angeles";
        private String currency = "USD";

        public RestaurantBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RestaurantBuilder city(String city) {
            this.city = city;
            return this;
        }

        public RestaurantBuilder state(String state) {
            this.state = state;
            return this;
        }

        public RestaurantBuilder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public RestaurantBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Restaurant build() {
            Restaurant restaurant = new Restaurant();
            restaurant.setName(name);
            restaurant.setCity(city);
            restaurant.setState(state);
            restaurant.setTimezone(timezone);
            restaurant.setCurrency(currency);
            return restaurant;
        }
    }

    public static class RoomBuilder {
        private Restaurant restaurant;
        private String name = "Private Room A";
        private Room.RoomType roomType = Room.RoomType.PRIVATE_ROOM;
        private int minCapacity = 2;
        private int maxCapacity = 10;
        private BigDecimal minimumSpendAmount = new BigDecimal("500.00");
        private String minimumSpendCurrency = "USD";
        private boolean active = true;

        public RoomBuilder restaurant(Restaurant restaurant) {
            this.restaurant = restaurant;
            return this;
        }

        public RoomBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RoomBuilder roomType(Room.RoomType roomType) {
            this.roomType = roomType;
            return this;
        }

        public RoomBuilder minCapacity(int minCapacity) {
            this.minCapacity = minCapacity;
            return this;
        }

        public RoomBuilder maxCapacity(int maxCapacity) {
            this.maxCapacity = maxCapacity;
            return this;
        }

        public RoomBuilder minimumSpend(BigDecimal amount, String currency) {
            this.minimumSpendAmount = amount;
            this.minimumSpendCurrency = currency;
            return this;
        }

        public RoomBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public Room build() {
            Room room = new Room();
            room.setRestaurant(restaurant);
            room.setName(name);
            room.setRoomType(roomType);
            room.setMinCapacity(minCapacity);
            room.setMaxCapacity(maxCapacity);

            Room.MinimumSpend minimumSpend = new Room.MinimumSpend();
            minimumSpend.setAmount(minimumSpendAmount);
            minimumSpend.setCurrency(minimumSpendCurrency);
            room.setMinimumSpend(minimumSpend);

            room.setActive(active);
            return room;
        }
    }

    public static class ReservationBuilder {
        private Restaurant restaurant;
        private Room room;
        private LocalDate reservationDate = LocalDate.now().plusDays(7);
        private TimeSlot timeSlot = TimeSlot.DINNER;
        private int partySize = 4;
        private String dinerName = "Sam Smith";
        private String dinerEmail = "sam.smith@example.com";
        private String dinerPhone = "+1-308-7469999";
        private ReservationStatus status = ReservationStatus.CONFIRMED;
        private String specialRequests = null;

        public ReservationBuilder restaurant(Restaurant restaurant) {
            this.restaurant = restaurant;
            return this;
        }

        public ReservationBuilder room(Room room) {
            this.room = room;
            return this;
        }

        public ReservationBuilder reservationDate(LocalDate reservationDate) {
            this.reservationDate = reservationDate;
            return this;
        }

        public ReservationBuilder timeSlot(TimeSlot timeSlot) {
            this.timeSlot = timeSlot;
            return this;
        }

        public ReservationBuilder partySize(int partySize) {
            this.partySize = partySize;
            return this;
        }

        public ReservationBuilder dinerName(String dinerName) {
            this.dinerName = dinerName;
            return this;
        }

        public ReservationBuilder dinerEmail(String dinerEmail) {
            this.dinerEmail = dinerEmail;
            return this;
        }

        public ReservationBuilder dinerPhone(String dinerPhone) {
            this.dinerPhone = dinerPhone;
            return this;
        }

        public ReservationBuilder status(ReservationStatus status) {
            this.status = status;
            return this;
        }

        public ReservationBuilder specialRequests(String specialRequests) {
            this.specialRequests = specialRequests;
            return this;
        }

        public Reservation build() {
            Reservation reservation = new Reservation();
            reservation.setRestaurant(restaurant);
            reservation.setRoom(room);
            reservation.setReservationDate(reservationDate);
            reservation.setTimeSlot(timeSlot);
            reservation.setPartySize(partySize);
            reservation.setDinerName(dinerName);
            reservation.setDinerEmail(dinerEmail);
            reservation.setDinerPhone(dinerPhone);
            reservation.setStatus(status);
            reservation.setSpecialRequests(specialRequests);
            return reservation;
        }
    }
}