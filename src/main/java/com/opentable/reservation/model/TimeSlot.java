package com.opentable.reservation.model;

import lombok.Getter;

import java.time.LocalTime;

@Getter
public enum TimeSlot {
    BREAKFAST(LocalTime.of(8, 30), LocalTime.of(10, 30)),
    LUNCH(LocalTime.of(11, 30), LocalTime.of(14, 30)),
    DINNER(LocalTime.of(17, 30), LocalTime.of(21, 30)),
    LATE_NIGHT(LocalTime.of(21, 30), LocalTime.of(23, 30));

    private final LocalTime start;
    private final LocalTime end;

    TimeSlot(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }
}