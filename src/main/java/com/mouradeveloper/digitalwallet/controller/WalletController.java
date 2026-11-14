package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.WalletDto;
import com.mouradeveloper.digitalwallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Wallets",
        description = "Wallet management and balance inquiries"
)
public class WalletController {

    private final WalletService walletService;


    @Operation(
            summary = "Get wallet by ID",
            description = "Returns a wallet by ID if it belongs to the authenticated user"
    )
    @GetMapping("/{id}")
    public ResponseEntity<WalletDto.Response> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(walletService.findById(id));
    }

    @Operation(
            summary = "Get authenticated user's wallet",
            description = "Returns the wallet associated with the authenticated user"
    )
    @GetMapping("/me")
    public ResponseEntity<WalletDto.Response> findMyWallet() {
        return ResponseEntity.ok(walletService.findMyWallet());
    }
}