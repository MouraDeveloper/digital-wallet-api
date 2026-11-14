package com.mouradeveloper.digitalwallet.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class WalletDto {

    @Schema(name = "WalletResponse")
    public record Response(
            UUID id,
            UUID userId,
            BigDecimal balance,
            Instant createdAt
    ) {
    }
}
