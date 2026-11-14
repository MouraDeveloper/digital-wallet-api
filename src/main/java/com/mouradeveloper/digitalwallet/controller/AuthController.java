package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.auth.Login;
import com.mouradeveloper.digitalwallet.models.dto.error.StandardError;
import com.mouradeveloper.digitalwallet.security.CustomUserDetails;
import com.mouradeveloper.digitalwallet.security.TokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "User authentication and JWT issuance"
)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;


    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful, JWT generated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Login.Response.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid login request data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password",
                    content = @Content()
            )
    })
    @Operation(
            summary = "Authenticate user",
            description = "Validates user credentials and returns a JWT"
    )
    @PostMapping("/login")
    public ResponseEntity<Login.Response> login(
            @RequestBody @Valid Login.Request loginRequest
    ) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                );

        Authentication authentication =
                authenticationManager.authenticate(authenticationToken);

        CustomUserDetails customUserDetails =
                (CustomUserDetails) authentication.getPrincipal();

        String token = tokenProvider.generateToken(customUserDetails);

        return ResponseEntity.ok(
                new Login.Response(token)
        );
    }
}
