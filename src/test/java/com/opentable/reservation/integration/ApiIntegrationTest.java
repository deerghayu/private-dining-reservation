package com.opentable.reservation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentable.reservation.dto.CreateReservationRequest;
import com.opentable.reservation.dto.CreateReservationRequest.Diner;
import com.opentable.reservation.dto.CreateReservationRequest.MonetaryAmount;
import com.opentable.reservation.model.*;
import com.opentable.reservation.repository.ReservationRepository;
import com.opentable.reservation.repository.RestaurantRepository;
import com.opentable.reservation.repository.RoomRepository;
import com.opentable.reservation.testutil.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Reservation API endpoints, covering reservation creation,
 * retrieval, double-booking prevention, availability checks, and cancellation flows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.opentable.reservation.TestContainersConfiguration.class)
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Restaurant restaurant;
    private Room room;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        restaurantRepository.deleteAll();

        restaurant = TestDataBuilder.restaurant().name("API Test Restaurant").build();
        restaurant = restaurantRepository.save(restaurant);

        room = TestDataBuilder.room()
                .restaurant(restaurant)
                .name("API Test Room")
                .minCapacity(2)
                .maxCapacity(10)
                .minimumSpend(new BigDecimal("500.00"), "USD")
                .build();
        room = roomRepository.save(room);
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    @Test
    void createReservation_HappyPath_ShouldReturn201() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest(
                room.getId(),
                LocalDate.now().plusDays(7),
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                "Window seat please",
                new Diner("Sam Smith", "sam.smith@example.com", "+1-555-1234")
        );

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.dinerEmail").value("sam.smith@example.com"));
    }

    @Test
    void getReservation_ExistingId_ShouldReturn200() throws Exception {
        Reservation reservation = createTestReservation();

        mockMvc.perform(get("/api/v1/reservations/" + reservation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservation.getId().toString()));
    }

    @Test
    void getReservation_NonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReservation_DoubleBooking_ShouldReturn409Conflict() throws Exception {
        LocalDate testDate = LocalDate.now().plusDays(7);

        // Create first reservation
        CreateReservationRequest request1 = new CreateReservationRequest(
                room.getId(),
                testDate,
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                null,
                new Diner("Sam Smith", "sam.smith@example.com", "+1-555-1234")
        );
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Attempt second reservation for same slot - should fail with 409
        CreateReservationRequest request2 = new CreateReservationRequest(
                room.getId(),
                testDate,
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                null,
                new Diner("Jane Doe", "jane@example.com", "+130874699")
        );
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already booked")));
    }

    @Test
    void checkAvailability_ThroughAPI_ShouldShowBookedSlotUnavailable() throws Exception {
        LocalDate testDate = LocalDate.now().plusDays(7);

        // Create a reservation
        createTestReservationForDate(testDate, TimeSlot.DINNER);

        // Check availability - DINNER should be unavailable
        mockMvc.perform(get("/api/v1/restaurants/" + restaurant.getId() + "/rooms/" + room.getId() + "/availability")
                        .param("startDate", testDate.toString())
                        .param("endDate", testDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].slots[?(@.slot == 'DINNER')].available").value(false))
                .andExpect(jsonPath("$.days[0].slots[?(@.slot == 'DINNER')].reason").value("Already booked"));
    }

    @Test
    void createReservation_AfterCancellation_ShouldAllowRebooking() throws Exception {
        LocalDate testDate = LocalDate.now().plusDays(7);

        // Create first reservation
        CreateReservationRequest request1 = new CreateReservationRequest(
                room.getId(),
                testDate,
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                null,
                new Diner("Sam Smith", "sam.smith@example.com", "+1-555-1234")
        );
        String response1 = mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID reservation1Id = UUID.fromString(objectMapper.readTree(response1).get("id").asText());

        // Cancel the first reservation
        String cancelRequest = """
                {
                    "cancelledBy": "sam.smith@example.com",
                    "reason": "Change of plans"
                }
                """;
        mockMvc.perform(post("/api/v1/reservations/" + reservation1Id + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Create second reservation for the SAME slot - should succeed now
        CreateReservationRequest request2 = new CreateReservationRequest(
                room.getId(),
                testDate,
                TimeSlot.DINNER,
                4,
                new MonetaryAmount(new BigDecimal("600.00"), "USD"),
                null,
                new Diner("Jane Smith", "jane@example.com", "+130874699")
        );
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.dinerEmail").value("jane@example.com"));
    }

    private Reservation createTestReservation() {
        return createTestReservationForDate(LocalDate.now().plusDays(7), TimeSlot.DINNER);
    }

    private Reservation createTestReservationForDate(LocalDate date, TimeSlot timeSlot) {
        Reservation reservation = TestDataBuilder.reservation()
                .room(room)
                .restaurant(restaurant)
                .reservationDate(date)
                .timeSlot(timeSlot)
                .build();
        return reservationRepository.save(reservation);
    }
}