package com.opentable.reservation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @NotBlank
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type")
    private RoomType roomType;

    @Min(1)
    @Column(name = "min_capacity")
    private int minCapacity;

    @Min(1)
    @Column(name = "max_capacity")
    private int maxCapacity;

    @NotNull
    @Embedded
    private MinimumSpend minimumSpend;

    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public boolean canHost(int partySize) {
        return partySize >= minCapacity && partySize <= maxCapacity;
    }

    public enum RoomType {
        ROOFTOP,
        HALL,
        PRIVATE_ROOM,
        CHEF_TABLE
    }

    @Data
    @Embeddable
    public static class MinimumSpend {

        @NotNull
        @Column(name = "minimum_spend_amount")
        private BigDecimal amount;

        @NotBlank
        @Column(name = "minimum_spend_currency")
        private String currency;
    }
}