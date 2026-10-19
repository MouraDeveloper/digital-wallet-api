package com.mouradeveloper.digitalwallet.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AppUserDto {
    public record Request(
            @NotBlank(message = "Full name cannot be blank")
            @Size(
                    max = 120,
                    message = "Full name must not exceed 120 characters"
            )
            String fullName,

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

    public record Response(
            UUID id,
            String fullName,
            String email,
            Instant createdAt
    ) {

    }
}
