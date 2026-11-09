package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;


    private static AppUser createAppUser() {
        return AppUser.create(
                "Lucas Almeida",
                "lucasalmeida@example.com",
                "hashed_password"
        );
    }

    private void mockWalletRepository(UUID walletId, Wallet wallet) {
        Mockito.when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));
    }

    private void mockWalletRepositoryNotFound(UUID walletId) {
        Mockito.when(walletRepository.findById(walletId))
                .thenReturn(Optional.empty());
    }

    private static Wallet createWallet(AppUser appUser) {
        return Wallet.create(appUser);
    }
}