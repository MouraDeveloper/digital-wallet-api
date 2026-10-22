package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.AppUserDto;
import com.mouradeveloper.digitalwallet.service.AppUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;


    @PostMapping
    public ResponseEntity<AppUserDto.Response> createUser(
            @RequestBody @Valid AppUserDto.Request request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                appUserService.createUser(request)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppUserDto.Response> findById(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(
                appUserService.findById(id)
        );
    }
}
