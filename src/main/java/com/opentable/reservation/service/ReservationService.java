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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
        log.info("Reservation {} created for room {}", saved.getId(), room.getId());
        //TODO: Publish event
        return ReservationResponse.from(saved);
    }
}