package com.crimson.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckInRequest(
        @NotNull Long eventId,
        @NotBlank @Size(max = 200) String qrPayload) {
}
