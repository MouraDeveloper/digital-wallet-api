package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.AppUserDto;
import com.mouradeveloper.digitalwallet.models.dto.error.StandardError;
import com.mouradeveloper.digitalwallet.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(
        name = "Users",
        description = "User registration and profile inquiries"
)
public class AppUserController {

    private final AppUserService appUserService;


    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppUserDto.Response.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid user registration data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already registered",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @Operation(
            summary = "Register user",
            description = "Creates a new user and their digital wallet"
    )
    @PostMapping
    public ResponseEntity<AppUserDto.Response> createUser(
            @RequestBody @Valid AppUserDto.Request request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                appUserService.createUser(request)
        );
    }

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppUserDto.Response.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid user ID format",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access to user denied",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @Operation(
            summary = "Get user by ID",
            description = "Returns the authenticated user's profile by ID. Access to other users is forbidden."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<AppUserDto.Response> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(
                appUserService.findById(id)
        );
    }
}
