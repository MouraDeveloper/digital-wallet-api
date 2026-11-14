package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.TransactionDto;
import com.mouradeveloper.digitalwallet.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Get transaction by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto.Response> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @Operation(summary = "Get wallet transaction history")
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<TransactionDto.Response>> findAllByWalletId(
            @PathVariable UUID walletId
    ) {
        return ResponseEntity.ok(transactionService.findByWalletId(walletId));
    }

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
