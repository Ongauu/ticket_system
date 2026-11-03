package com.crimson.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public record CreateEventRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 200) String venue,
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code") String currency,
        @NotNull Instant startsAt,
        @NotNull Instant salesOpenAt,
        @NotNull Instant salesCloseAt,
        @NotEmpty @Size(max = 20) @Valid List<TierSpec> tiers
) {
    public record TierSpec(
            @NotBlank @Size(max = 100) String name,
            @Min(0) long priceCents,
            @Min(1) @Max(1_000_000) int quantity
    ) {}
}
