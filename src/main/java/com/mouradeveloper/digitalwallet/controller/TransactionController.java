package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.TransactionDto;
import com.mouradeveloper.digitalwallet.models.dto.error.StandardError;
import com.mouradeveloper.digitalwallet.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Transactions",
        description = "Wallet deposits, transfers and transaction history"
)
public class TransactionController {

    private final TransactionService transactionService;


    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Transfer created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TransactionDto.Response.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid transfer request",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access to source wallet denied",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Source or destination wallet not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Idempotency key conflict",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Insufficient funds or transfer to the same wallet",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @Operation(summary = "Transfer funds between wallets")
    @PostMapping("/create-transfer")
    public ResponseEntity<TransactionDto.Response> createTransfer(
            @RequestBody @Valid TransactionDto.TransferRequest transferRequest,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransfer(
                        transferRequest.sourceWalletId(),
                        transferRequest.destinationWalletId(),
                        transferRequest.amount(),
                        idempotencyKey
                ));
    }

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TransactionDto.Response.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access to transaction denied",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @Operation(summary = "Get transaction by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto.Response> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction history retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = TransactionDto.Response.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access to wallet denied",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @Operation(summary = "Get wallet transaction history")
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<TransactionDto.Response>> findAllByWalletId(
            @PathVariable UUID walletId
    ) {
        return ResponseEntity.ok(transactionService.findByWalletId(walletId));
    }

    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Deposit created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TransactionDto.Response.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid deposit request",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access to destination wallet denied",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Destination wallet not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Idempotency key conflict",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @Operation(summary = "Deposit funds into a wallet")
    @PostMapping("/create-deposit")
    public ResponseEntity<TransactionDto.Response> createDeposit(
            @RequestBody @Valid TransactionDto.DepositRequest depositRequest,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createDeposit(
                        depositRequest.destinationWalletId(),
                        depositRequest.amount(),
                        idempotencyKey
                ));
    }
}
