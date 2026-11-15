package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.forbidden.WalletAccessDeniedException;
import com.mouradeveloper.digitalwallet.exception.notFound.WalletNotFoundException;
import com.mouradeveloper.digitalwallet.mapper.WalletMapper;
import com.mouradeveloper.digitalwallet.models.dto.WalletDto;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import com.mouradeveloper.digitalwallet.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    private WalletService walletService;

    @Test
    void shouldReturnAuthenticatedUserWallet() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = createWallet(createAppUser());

        WalletDto.Response expected = new WalletDto.Response(
                UUID.randomUUID(),
                userId,
                BigDecimal.ZERO,
                Instant.now()
        );

        Mockito.when(authenticatedUserProvider.getUserId())
                .thenReturn(userId);

        Mockito.when(walletRepository.findByUser_Id(userId))
                .thenReturn(Optional.of(wallet));

        Mockito.when(walletMapper.toResponse(wallet))
                .thenReturn(expected);

        WalletDto.Response actual = walletService.findMyWallet();

        assertEquals(expected, actual);

        Mockito.verify(walletRepository).findByUser_Id(userId);
    }

    @Test
    void shouldThrowWhenAuthenticatedUserHasNoWallet() {
        UUID userId = UUID.randomUUID();

        Mockito.when(authenticatedUserProvider.getUserId())
                .thenReturn(userId);

        Mockito.when(walletRepository.findByUser_Id(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                WalletNotFoundException.class,
                () -> walletService.findMyWallet()
        );
    }

    @Test
    void shouldDenyAccessToAnotherUsersWallet() {
        UUID walletOwnerId = UUID.randomUUID();
        UUID authenticatedUserId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        AppUser walletOwner = createAppUser();

        ReflectionTestUtils.setField(
                walletOwner,
                "id",
                walletOwnerId
        );

        Wallet wallet = createWallet(walletOwner);

        Mockito.when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        Mockito.when(authenticatedUserProvider.getUserId())
                .thenReturn(authenticatedUserId);

        assertThrows(
                WalletAccessDeniedException.class,
                () -> walletService.findById(walletId)
        );

        Mockito.verify(walletRepository).findById(walletId);

        Mockito.verify(authenticatedUserProvider).getUserId();

        Mockito.verifyNoInteractions(walletMapper);
    }

    private static AppUser createAppUser() {
        return AppUser.create(
                "Lucas Almeida",
                "lucasalmeida@example.com",
                "hashed_password"
        );
    }

    private static Wallet createWallet(AppUser appUser) {
        return Wallet.create(appUser);
    }
}
