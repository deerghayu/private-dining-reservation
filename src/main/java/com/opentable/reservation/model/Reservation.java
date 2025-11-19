package com.opentable.reservation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "reservations")
// Note: Double-booking prevention is handled by a partial unique index in PostgreSQL:
// CREATE UNIQUE INDEX uk_room_date_slot_active ON reservations (room_id, reservation_date, time_slot)
// WHERE status IN ('PENDING', 'CONFIRMED');
// This allows rebooking of cancelled slots while preventing double-booking of active reservations.
// Partial indexes cannot be expressed in JPA annotations, so it's defined in the migration file.
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotNull
    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_slot", nullable = false)
    private TimeSlot timeSlot;

    @Min(1)
    @Column(name = "party_size")
    private int partySize;

    @NotBlank
    @Column(name = "diner_name")
    private String dinerName;

    @Email
    @NotBlank
    @Column(name = "diner_email")
    private String dinerEmail;

    @NotBlank
    @Column(name = "diner_phone")
    private String dinerPhone;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "special_requests", length = 500)
    private String specialRequests;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Version
    private long version;
}