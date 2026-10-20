package com.mouradeveloper.digitalwallet.models.dto.error;

import lombok.Builder;

import java.time.Instant;

@Builder
public record StandardError(
    Instant timestamp,
    Integer status,
    String error,
    String message,
    String path
) {
}
