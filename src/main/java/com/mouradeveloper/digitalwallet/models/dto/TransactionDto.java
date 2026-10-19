package com.mouradeveloper.digitalwallet.models.dto;

import com.mouradeveloper.digitalwallet.models.enums.TypeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransactionDto {

    public record DepositRequest(
            @NotNull(message = "destination cannot be null")
            UUID destinationWalletId,

            @NotNull(message = "amount cannot be null")
            @DecimalMin(value = "0.01", message = "amount must be greater than zero")
            @Digits(integer = 10, fraction = 2, message = "amount must have up to 10 digits and 2 decimal places")
            BigDecimal amount
    ){}


    public record TransferRequest(
            @NotNull(message = "source cannot be null")
            UUID sourceWalletId,

            @NotNull(message = "destination cannot be null")
            UUID destinationWalletId,

            @NotNull(message = "amount cannot be null")
            @DecimalMin(value = "0.01", message = "amount must be greater than zero")
            @Digits(integer = 10, fraction = 2, message = "amount must have up to 10 digits and 2 decimal places")
            BigDecimal amount
    ){}


    public record Response(
            Long id,
            TypeEnum type,
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount,
            Instant createdAt

    ){}


}
