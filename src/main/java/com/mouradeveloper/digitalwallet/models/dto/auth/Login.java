package com.mouradeveloper.digitalwallet.models.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class Login {
    private Login() {
    }

    @Schema(name = "LoginRequest")
    public record Request(
            @NotBlank(message = "Email cannot be blank")
            @Email(message = "Email should be valid")
            @Size(
                    max = 120,
                    message = "Email must not exceed 120 characters"
            )
            String email,

            @NotBlank(message = "Password cannot be blank")
            @Size(
                    min = 8,
                    message = "Password must be at least 8 characters long"
            )
            String password
    ) {
    }

    @Schema(name = "LoginResponse")
    public record Response(
            String token
    ) {
    }
}