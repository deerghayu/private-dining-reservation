package com.opentable.reservation.exception;

import com.opentable.reservation.model.TimeSlot;

import java.time.LocalDate;

public class RoomAlreadyBookedException extends BusinessException {
    public RoomAlreadyBookedException(LocalDate date, TimeSlot slot) {
        super("Room already booked for %s (%s)".formatted(date, slot));
    }
}