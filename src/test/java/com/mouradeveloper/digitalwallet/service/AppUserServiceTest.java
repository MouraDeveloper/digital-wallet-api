package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.conflict.EmailAlreadyExistsException;
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

        var request = createAppUserRequest(fullName, email, password);

        mockEmailDoesNotExist(request.email());

        var encodedPassword = "encodedPassword";

        Mockito.when(passwordEncoder.encode(request.password()))
                .thenReturn(encodedPassword);

        var user = createAppUser(request, encodedPassword);

        mockAppUserSaved(user);


        var wallet = Wallet.create(user);

        mockWalletSaved(wallet);

        var expectedResponse = createAppUserResponse(user);

        Mockito.when(appUserMapper.toResponse(Mockito.same(user)))
                .thenReturn(expectedResponse);


        Assertions.assertEquals(expectedResponse, appUserService.createUser(request));

        Mockito.verify(walletRepository)
                .save(Mockito.any(Wallet.class));

        Mockito.verify(appUserRepository)
                .save(Mockito.any(AppUser.class));
    }

    @Test
    void shouldThrowEmailAlreadyExistsExceptionWhenEmailAlreadyExists() {
        var request = createAppUserRequest("John Doe", "john.doe@example.com", "password123");

        mockEmailDoesExist(request.email());

        Assertions.assertThrows(
                EmailAlreadyExistsException.class, () -> appUserService.createUser(request)
        );

        Mockito.verify(passwordEncoder, Mockito.never())
                .encode(Mockito.anyString());

        Mockito.verify(appUserRepository, Mockito.never())
                .save(Mockito.any(AppUser.class));

        Mockito.verify(walletRepository, Mockito.never())
                .save(Mockito.any(Wallet.class));
    }


    private void mockWalletSaved(Wallet wallet) {
        Mockito.when(walletRepository.save(Mockito.any(Wallet.class)))
                .thenReturn(wallet);
    }

    private void mockAppUserSaved(AppUser appUser) {
        Mockito.when(appUserRepository.save(Mockito.any(AppUser.class)))
                .thenReturn(appUser);
    }

    private AppUserDto.Request createAppUserRequest(String fullName, String email, String password) {
        return new AppUserDto.Request(
                fullName,
                email,
                password
        );
    }

    private AppUser createAppUser(AppUserDto.Request request, String encodedPassword) {
        return AppUser.create(
                request.fullName(),
                request.email(),
                encodedPassword
        );
    }

    private AppUserDto.Response createAppUserResponse(AppUser user) {
        return new AppUserDto.Response(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }

    private void mockEmailDoesNotExist(String email) {
        Mockito.when(appUserRepository.existsByEmail(email))
                .thenReturn(false);
    }

    private void mockEmailDoesExist(String email) {
        Mockito.when(appUserRepository.existsByEmail(email))
                .thenReturn(true);
    }
}
