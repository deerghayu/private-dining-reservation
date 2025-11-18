package com.opentable.reservation.dto;


import com.opentable.reservation.model.Room;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Room summary with parent restaurant information")
public record RoomSummaryResponse(
        @Schema(description = "Room identifier") UUID roomId,
        @Schema(description = "Room name") String name,
        @Schema(description = "Type of the room") Room.RoomType roomType,
        @Schema(description = "Minimum capacity") int minCapacity,
        @Schema(description = "Maximum capacity") int maxCapacity,
        @Schema(description = "Minimum spend currency") String currency,
        @Schema(description = "Minimum spend amount") String minimumSpendAmount,
        @Schema(description = "Restaurant information") RestaurantSummary restaurant
) {
    public static RoomSummaryResponse from(Room room) {
        return new RoomSummaryResponse(
                room.getId(),
                room.getName(),
                room.getRoomType(),
                room.getMinCapacity(),
                room.getMaxCapacity(),
                room.getMinimumSpend().getCurrency(),
                room.getMinimumSpend().getAmount().toPlainString(),
                new RestaurantSummary(
                        room.getRestaurant().getId(),
                        room.getRestaurant().getName(),
                        room.getRestaurant().getCity(),
                        room.getRestaurant().getTimezone()
                )
        );
    }

    @Schema(description = "Parent restaurant summary")
    public record RestaurantSummary(
            @Schema(description = "Restaurant identifier") UUID restaurantId,
            @Schema(description = "Restaurant name") String name,
            @Schema(description = "City") String city,
            @Schema(description = "Timezone") String timezone
    ) {
    }
}