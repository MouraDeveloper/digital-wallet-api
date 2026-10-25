package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.mapper.AppUserMapper;
import com.mouradeveloper.digitalwallet.models.dto.AppUserDto;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.repository.AppUserRepository;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AppUserMapper appUserMapper;

    @InjectMocks
    private AppUserService appUserService;


    @Test
    void shouldCreateUserAndWalletWhenEmailDoesNotExist() {
        var fullName = "Olivia";
        var email = "olivia@gmail.com";
        var password = "password123";

        var request = new AppUserDto.Request(
                fullName,
                email,
                password
        );

        Mockito.when(appUserRepository.existsByEmail(request.email()))
                .thenReturn(false);

        var encodedPassword = "encodedPassword";

        Mockito.when(passwordEncoder.encode(request.password()))
                .thenReturn(encodedPassword);

        var user = AppUser.create(request.fullName(), request.email(), encodedPassword);

        Mockito.when(appUserRepository.save(Mockito.any(AppUser.class)))
                .thenReturn(user);


        var wallet = Wallet.create(user);

        Mockito.when(walletRepository.save(Mockito.any(Wallet.class)))
                .thenReturn(wallet);

        var expectedResponse = new AppUserDto.Response(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getCreatedAt()
        );

        Mockito.when(appUserMapper.toResponse(Mockito.same(user)))
                .thenReturn(expectedResponse);


        Assertions.assertEquals(expectedResponse, appUserService.createUser(request));

        Mockito.verify(walletRepository)
                .save(Mockito.any(Wallet.class));

        Mockito.verify(appUserRepository)
                .save(Mockito.any(AppUser.class));
    }
}
