package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.TransactionDto;
import com.mouradeveloper.digitalwallet.models.dto.WalletDto;
import com.mouradeveloper.digitalwallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;


    @GetMapping("/{id}")
    public ResponseEntity<WalletDto.Response> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(walletService.findById(id));
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletDto.Response> deposit(
            @RequestBody @Valid TransactionDto.DepositRequest depositRequest
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                walletService.deposit(
                        depositRequest.destinationWalletId(),
                        depositRequest.amount()
                )
        );
    }
}
