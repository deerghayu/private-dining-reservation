package com.opentable.reservation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@Entity
@Table(name = "restaurants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_restaurant_name_city", columnNames = {"name", "city"})
})
@AllArgsConstructor
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String name;

    @NotBlank
    private String city;

    private String state;

    @NotBlank
    private String timezone;

    @NotBlank
    private String currency;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}