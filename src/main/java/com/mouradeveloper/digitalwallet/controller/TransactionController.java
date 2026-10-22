package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.TransactionDto;
import com.mouradeveloper.digitalwallet.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;


    @PostMapping("/create-transfer")
    public ResponseEntity<TransactionDto.Response> createTransfer(
            @RequestBody @Valid TransactionDto.TransferRequest transferRequest
    ) {
        return ResponseEntity.ok(
                transactionService.createTransfer(
                        transferRequest.sourceWalletId(),
                        transferRequest.destinationWalletId(),
                        transferRequest.amount()
                )
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto.Response> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<TransactionDto.Response>> findAllByWalletId(
            @PathVariable UUID walletId
    ) {
        return ResponseEntity.ok(transactionService.findByWalletId(walletId));
    }
}
