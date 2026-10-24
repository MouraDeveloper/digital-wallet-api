package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.business.InsufficientFundsException;
import com.mouradeveloper.digitalwallet.exception.business.InvalidAmountException;
import com.mouradeveloper.digitalwallet.exception.business.SameWalletTransferException;
import com.mouradeveloper.digitalwallet.exception.notFound.WalletNotFoundException;
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

import static org.mockito.Mockito.never;

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

    @Test
    void shouldFailTransferWhenBalanceIsInsufficient() {
        var walletSourceId = UUID.randomUUID();
        var walletDestinationId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(150.00);

        var user1 = createAppUser();
        var user2 = createAppUser();

        var walletSource = createWallet(user1);
        var walletDestination = createWallet(user2);

        walletSource.deposit(BigDecimal.valueOf(100.00));

        mockWalletSource(walletSourceId, walletSource);
        mockWalletDestination(walletDestinationId, walletDestination);

        Assertions.assertThrows(InsufficientFundsException.class, () -> transactionService.createTransfer(
                walletSourceId, walletDestinationId, amount)
        );

        Assertions.assertEquals(BigDecimal.valueOf(100.00), walletSource.getBalance());

        Assertions.assertEquals(BigDecimal.ZERO, walletDestination.getBalance());

        Mockito.verify(transactionRepository, never())
                .save(Mockito.any(Transaction.class));
    }

    @Test
    void shouldThrowSameWalletTransferExceptionWhenSourceAndDestinationAreSame() {
        var walletId = UUID.randomUUID();

        var user = createAppUser();
        var wallet = createWallet(user);

        wallet.deposit(BigDecimal.valueOf(100.00));

        Assertions.assertThrows(SameWalletTransferException.class, () -> transactionService.createTransfer(
                walletId, walletId, BigDecimal.valueOf(100.00))
        );

        Assertions.assertEquals(BigDecimal.valueOf(100.00), wallet.getBalance());

        Mockito.verify(transactionRepository, never())
                .save(Mockito.any(Transaction.class));
    }

    @Test
    void shouldThrowWalletNotFoundExceptionWhenSourceWalletDoesNotExist() {
        var sourceWalletId = UUID.randomUUID();
        var destinationWalletId = UUID.randomUUID();

        mockWalletNotFound(sourceWalletId);
        Assertions.assertThrows(WalletNotFoundException.class, () -> transactionService.createTransfer(
                sourceWalletId, destinationWalletId, BigDecimal.valueOf(100.00))
        );

        Mockito.verify(transactionRepository, never())
                .save(Mockito.any(Transaction.class));
    }

    @Test
    void shouldThrowWalletNotFoundExceptionWhenDestinationWalletDoesNotExist() {
        var sourceWalletId = UUID.randomUUID();
        var destinationWalletId = UUID.randomUUID();

        mockWalletSource(sourceWalletId, createWallet(createAppUser()));
        mockWalletNotFound(destinationWalletId);

        Assertions.assertThrows(WalletNotFoundException.class, () -> transactionService.createTransfer(
                sourceWalletId, destinationWalletId, BigDecimal.valueOf(100.00)
        ));

        Mockito.verify(transactionRepository, never())
                .save(Mockito.any(Transaction.class));

    }

    @Test
    void shouldThrowInvalidAmountExceptionWhenTransferAmountIsInvalid() {
        var sourceWalletId = UUID.randomUUID();
        var destinationWalletId = UUID.randomUUID();
        var amount = BigDecimal.ZERO;

        var walletSource = createWallet(createAppUser());
        var walletDestination = createWallet(createAppUser());

        mockWalletSource(sourceWalletId, walletSource);
        mockWalletDestination(destinationWalletId, walletDestination);

        Assertions.assertThrows(InvalidAmountException.class, () -> transactionService.createTransfer(
                sourceWalletId, destinationWalletId, amount
        ));

        Assertions.assertEquals(BigDecimal.ZERO, walletDestination.getBalance());
        Assertions.assertEquals(BigDecimal.ZERO, walletSource.getBalance());

        Mockito.verify(transactionRepository, never())
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

    private void mockWalletNotFound(
            UUID walletSourceId
    ) {
        Mockito.when(walletRepository.findById(walletSourceId))
                .thenReturn(Optional.empty());
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
