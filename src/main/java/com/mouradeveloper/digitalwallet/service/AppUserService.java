package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.conflict.EmailAlreadyExistsException;
import com.mouradeveloper.digitalwallet.exception.notFound.AppUserNotFoundException;
import com.mouradeveloper.digitalwallet.mapper.AppUserMapper;
import com.mouradeveloper.digitalwallet.models.dto.AppUserDto;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.repository.AppUserRepository;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;
    private final AppUserMapper appUserMapper;


    @Transactional
    public AppUserDto.Response createUser(AppUserDto.Request request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        var passwordHash = passwordEncoder.encode(request.password());

        var appUser = AppUser.create(
                request.fullName(),
                request.email(),
                passwordHash
        );

        var savedUser = appUserRepository.save(appUser);

        var wallet = Wallet.create(savedUser);

        walletRepository.save(wallet);

        return appUserMapper.toResponse(savedUser);
    }


    @Transactional(readOnly = true)
    public AppUserDto.Response findById(UUID id) {
        var appUser = appUserRepository.findById(id)
                .orElseThrow(
                        () -> new AppUserNotFoundException(id)
                );

        return appUserMapper.toResponse(appUser);
    }
}
