package com.crimson.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = " username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "password is required")
    private String password;
}
