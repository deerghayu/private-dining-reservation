package com.opentable.reservation.service;

import com.opentable.reservation.dto.CreateReservationRequest;
import com.opentable.reservation.dto.ReservationResponse;
import com.opentable.reservation.exception.BusinessException;
import com.opentable.reservation.exception.RoomAlreadyBookedException;
import com.opentable.reservation.exception.NotFoundException;
import com.opentable.reservation.model.Reservation;
import com.opentable.reservation.model.ReservationStatus;
import com.opentable.reservation.model.Room;
import com.opentable.reservation.repository.ReservationRepository;
import com.opentable.reservation.repository.RoomRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for creating, listing, and cancelling reservations while enforcing
 * restaurant rules (capacity, minimum spend, no double-booking) in one place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final RoomRepository roomRepository;
    private final AvailabilityService availabilityService;
    private final ReservationRepository reservationRepository;

    /**
     * Applies all business rules and creates a reservation. Throws {@link BusinessException}
     * if any validation fails and {@link RoomAlreadyBookedException} if the slot is already reserved.
     */
    @Transactional
    public ReservationResponse createReservation(@Valid CreateReservationRequest reservationRequest) {
        log.info("Creating reservation for room {} on {} ({})", reservationRequest.roomId(), reservationRequest.reservationDate(), reservationRequest.timeSlot());

        Room room = roomRepository.findById(reservationRequest.roomId())
                .orElseThrow(() -> new NotFoundException("Room %s not found".formatted(reservationRequest.roomId())));

        if (!room.isActive()) {
            log.warn("Attempt to book inactive room {}", room.getId());
            throw new BusinessException("Room is not accepting reservations");
        }
        if (!room.canHost(reservationRequest.partySize())) {
            log.warn("Party size {} outside capacity for room {}", reservationRequest.partySize(), room.getId());
            throw new BusinessException("Party size outside room capacity");
        }
        Room.MinimumSpend minimumSpend = room.getMinimumSpend();

        if (reservationRequest.estimatedSpend() != null && minimumSpend != null) {
            BigDecimal minimumSpendAmount = minimumSpend.getAmount();
            BigDecimal estimateAmount = reservationRequest.estimatedSpend().amount();
            if (!minimumSpend.getCurrency().equalsIgnoreCase(reservationRequest.estimatedSpend().currency())) {
                log.warn("Currency mismatch for room {} expected {} but got {}", room.getId(), room.getMinimumSpend().getCurrency(), reservationRequest.estimatedSpend().currency());
                throw new BusinessException("Estimated spend must be provided in %s".formatted(room.getMinimumSpend().getCurrency()));
            }
            if (estimateAmount.compareTo(minimumSpendAmount) < 0) {
                log.warn("Estimated spend {} below minimum {} for room {}", estimateAmount, minimumSpend, room.getId());
                throw new BusinessException("Estimated spend must satisfy minimum");
            }
        }
        if (!availabilityService.isSlotAvailable(room.getId(), reservationRequest.reservationDate(), reservationRequest.timeSlot())) {
            log.warn("Room {} already booked on {} {}", room.getId(), reservationRequest.reservationDate(), reservationRequest.timeSlot());
            throw new RoomAlreadyBookedException(reservationRequest.reservationDate(), reservationRequest.timeSlot());
        }

        //TODO: it's possible to extract method
        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setRestaurant(room.getRestaurant());
        reservation.setReservationDate(reservationRequest.reservationDate());
        reservation.setTimeSlot(reservationRequest.timeSlot());
        reservation.setPartySize(reservationRequest.partySize());
        reservation.setDinerName(reservationRequest.diner().name());
        reservation.setDinerEmail(reservationRequest.diner().email());
        reservation.setDinerPhone(reservationRequest.diner().phone());
        reservation.setSpecialRequests(reservationRequest.specialRequests());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        Reservation saved = reservationRepository.save(reservation);
        //TODO: Publish event
        return ReservationResponse.from(saved);
    }

    /**
     * Retrieves reservation details by ID.
     * Throws {@link NotFoundException} if the reservation does not exist.
     */
    public ReservationResponse getReservationDetails(UUID reservationId) {
        ReservationResponse reservationResponse = ReservationResponse.from(getEntity(reservationId));

        log.info("Fetched reservation details for ID {}", reservationId);
        return reservationResponse;
    }

    /**
     * Retrieves a reservation entity by its ID.
     * Throws {@link NotFoundException} if the reservation does not exist.
     */
    public Reservation getEntity(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation %s not found".formatted(reservationId)));
    }

    /**
     * Cancels an existing reservation if it's not already cancelled or in the past.
     * Throws {@link BusinessException} if cancellation is not allowed.
     */
    @Transactional
    public ReservationResponse cancelReservation(UUID reservationId, String cancelledBy, String reason) {
        Reservation reservation = getEntity(reservationId);
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            log.warn("Reservation {} already cancelled", reservationId);
            throw new BusinessException("Reservation already cancelled");
        }
        if (reservation.getReservationDate().isBefore(LocalDate.now())) {
            log.warn("Reservation {} is in the past; cannot cancel", reservationId);
            throw new BusinessException("Cannot cancel past reservations");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledBy(cancelledBy);
        reservation.setCancellationReason(reason);
        reservation.setCancelledAt(java.time.OffsetDateTime.now());
        Reservation cancelledReservation = reservationRepository.save(reservation);

        //TODO: Publish cancellation event
        return ReservationResponse.from(cancelledReservation);
    }

    /**
     * Lists all reservations for a diner, optionally filtering to only upcoming reservations.
     */
    public List<ReservationResponse> listReservationsByDiner(String dinerEmail, boolean upcomingOnly) {
        List<Reservation> reservations = upcomingOnly
                ? reservationRepository.findUpcomingReservations(dinerEmail, LocalDate.now())
                : reservationRepository.findByDinerEmailIgnoreCase(dinerEmail);

        List<ReservationResponse> reservationsByDiner = reservations.stream().map(ReservationResponse::from).toList();

        log.info("Found {} reservations for diner {} (upcomingOnly={})",
                reservationsByDiner.size(), dinerEmail, upcomingOnly);

        return reservationsByDiner;
    }

    /**
     * Lists all reservations for a restaurant. Intended for staff use.
     */
    public List<ReservationResponse> listReservationsByRestaurant(UUID restaurantId) {
        return reservationRepository.findByRestaurantId(restaurantId).stream()
                .map(ReservationResponse::from)
                .toList();
    }
}