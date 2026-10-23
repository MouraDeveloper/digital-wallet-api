package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.mapper.TransactionMapper;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Transaction;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.repository.TransactionRepository;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;


    @Test
    void shouldTransferAmountBetweenWallets() {
        var walletSourceId = UUID.randomUUID();
        var walletDestinationId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(150.00);

        var user1 = createAppUser();
        var user2 = createAppUser();

        var walletSource = createWallet(user1);
        var walletDestination = createWallet(user2);

        mockWalletSource(walletSourceId, walletSource);
        mockWalletDestination(walletDestinationId, walletDestination);

        walletSource.deposit(BigDecimal.valueOf(500.00));

        transactionService.createTransfer(walletSourceId, walletDestinationId, amount);

        Assertions.assertEquals(BigDecimal.valueOf(350.00), walletSource.getBalance());
        Assertions.assertEquals(BigDecimal.valueOf(150.00), walletDestination.getBalance());

        Mockito.verify(transactionRepository)
                .save(Mockito.any(Transaction.class));
    }


    private void mockWalletDestination(
            UUID walletDestinationId,
            Wallet walletDestination
    ) {
        Mockito.when(walletRepository.findById(walletDestinationId))
                .thenReturn(Optional.of(walletDestination));
    }

    private void mockWalletSource(
            UUID walletSourceId,
            Wallet walletSource
    ) {
        Mockito.when(walletRepository.findById(walletSourceId))
                .thenReturn(Optional.of(walletSource));
    }

    private static AppUser createAppUser() {
        return AppUser.create(
                "John Doe",
                "john.doe@gmail.com",
                "hashed_password"
        );
    }

    private static Wallet createWallet(AppUser appUser) {
        return Wallet.create(appUser);
    }
}
