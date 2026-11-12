package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.auth.Login;
import com.mouradeveloper.digitalwallet.security.CustomUserDetails;
import com.mouradeveloper.digitalwallet.security.TokenProvider;
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
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;


    @PostMapping("/login")
    public ResponseEntity<Login.Response> login(
            @RequestBody @Valid Login.Request loginRequest
    ) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
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
