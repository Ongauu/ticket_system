package com.crimson.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateReservationRequest(
        @NotNull Long eventId,
        @NotEmpty @Size(max = 10) @Valid List<Item> items

) {
    public record Item(@NotNull Long tierId,
                       @Min(1) @Max(20) int quantity) {}
}
