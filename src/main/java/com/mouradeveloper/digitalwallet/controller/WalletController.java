package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.WalletDto;
import com.mouradeveloper.digitalwallet.models.dto.error.StandardError;
import com.mouradeveloper.digitalwallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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


    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wallet retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WalletDto.Response.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid wallet ID format",
                    content = @Content()
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

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authenticated user's wallet retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WalletDto.Response.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found for authenticated user",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @Operation(
            summary = "Get authenticated user's wallet",
            description = "Returns the wallet associated with the authenticated user"
    )
    @GetMapping("/me")
    public ResponseEntity<WalletDto.Response> findMyWallet() {
        return ResponseEntity.ok(walletService.findMyWallet());
    }
}
