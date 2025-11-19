package com.opentable.reservation.service;

import com.opentable.reservation.dto.CreateReservationRequest;
import com.opentable.reservation.dto.ReservationResponse;
import com.opentable.reservation.event.ReservationCancelledEvent;
import com.opentable.reservation.event.ReservationCreatedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service responsible for creating, listing, and cancelling reservations while enforcing
 * restaurant rules (capacity, minimum spend, no double-booking) in one place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        // Check for existing active reservations for this slot
        // This application-level check works with the database constraints to prevent double-booking
        boolean hasActiveReservation = reservationRepository.existsByRoomIdAndReservationDateAndTimeSlotAndStatusIn(
                reservationRequest.roomId(),
                reservationRequest.reservationDate(),
                reservationRequest.timeSlot(),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
        );

        if (hasActiveReservation) {
            log.warn("Slot already booked: room={}, date={}, slot={}", room.getId(), reservationRequest.reservationDate(), reservationRequest.timeSlot());
            throw new RoomAlreadyBookedException(reservationRequest.reservationDate(), reservationRequest.timeSlot());
        }

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
        Reservation savedReservation = reservationRepository.save(reservation);

        // Publish event for asynchronous processing (notifications, analytics, etc.)
        publishReservationCreatedEvent(savedReservation);

        log.info("Successfully created reservation {} for room {} on {}", savedReservation.getId(), room.getId(), savedReservation.getReservationDate());
        return ReservationResponse.from(savedReservation);
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

        // Publish cancellation event for asynchronous processing
        publishReservationCancelledEvent(cancelledReservation);

        return ReservationResponse.from(cancelledReservation);
    }

    /**
     * Lists all reservations for a diner, optionally filtering to only upcoming reservations.
     * Returns paginated results sorted by reservation date (descending).
     */
    public Page<ReservationResponse> listReservationsByDiner(String dinerEmail, boolean upcomingOnly, Pageable pageable) {
        Page<Reservation> reservations = upcomingOnly
                ? reservationRepository.findUpcomingReservations(dinerEmail, LocalDate.now(), pageable)
                : reservationRepository.findByDinerEmailIgnoreCase(dinerEmail, pageable);

        Page<ReservationResponse> reservationsByDiner = reservations.map(ReservationResponse::from);

        log.info("Found {} reservations for diner {} (upcomingOnly={}, page={}/{})",
                reservationsByDiner.getContent().size(), dinerEmail, upcomingOnly,
                reservationsByDiner.getNumber(), reservationsByDiner.getTotalPages());

        return reservationsByDiner;
    }

    /**
     * Lists all reservations for a restaurant. Intended for staff use.
     * Returns paginated results sorted by reservation date (descending).
     */
    public Page<ReservationResponse> listReservationsByRestaurant(UUID restaurantId, Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findByRestaurantId(restaurantId, pageable);
        return reservations.map(ReservationResponse::from);
    }

    /**
     * Publishes a ReservationCreatedEvent for asynchronous processing.
     * Listeners can use this event to send confirmation emails, update caches, or track analytics.
     */
    private void publishReservationCreatedEvent(Reservation reservation) {
        ReservationCreatedEvent event = new ReservationCreatedEvent(
                reservation.getId(),
                reservation.getRestaurant().getId(),
                reservation.getRoom().getId(),
                reservation.getRoom().getName(),
                reservation.getReservationDate(),
                reservation.getTimeSlot(),
                reservation.getPartySize(),
                reservation.getDinerName(),
                reservation.getDinerEmail(),
                reservation.getDinerPhone(),
                reservation.getSpecialRequests(),
                reservation.getCreatedAt()
        );

        log.debug("Publishing ReservationCreatedEvent for reservation {}", reservation.getId());
        eventPublisher.publishEvent(event);
    }

    /**
     * Publishes a ReservationCancelledEvent for asynchronous processing.
     * Listeners can use this event to send cancellation notifications or update availability caches.
     */
    private void publishReservationCancelledEvent(Reservation reservation) {
        ReservationCancelledEvent event = new ReservationCancelledEvent(
                reservation.getId(),
                reservation.getRestaurant().getId(),
                reservation.getRoom().getId(),
                reservation.getRoom().getName(),
                reservation.getReservationDate(),
                reservation.getTimeSlot(),
                reservation.getDinerName(),
                reservation.getDinerEmail(),
                reservation.getCancelledBy(),
                reservation.getCancellationReason(),
                reservation.getCancelledAt()
        );

        log.debug("Publishing ReservationCancelledEvent for reservation {}", reservation.getId());
        eventPublisher.publishEvent(event);
    }
}