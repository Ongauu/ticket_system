package com.crimson.auth.dto.request;

import jakarta.validation.constraints.Size;

public record PayRequest(@Size(max = 200) String paymentToken) {}
