package com.mouradeveloper.digitalwallet.models.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class WalletDto {
    public record Response(
            UUID id,
            UUID userId,
            BigDecimal balance,
            Instant createdAt
    ) {
    }
}
