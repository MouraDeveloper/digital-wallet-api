package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.mapper.WalletMapper;
import com.mouradeveloper.digitalwallet.models.dto.WalletDto;
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
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletService walletService;


    @Test
    void shouldDepositAmountIntoWallet() {

        UUID walletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        AppUser appUser = AppUser.create(
                "Lucas Almeida",
                "lucasalmeida@example.com",
                "hashed_password"
        );

        Wallet wallet = Wallet.create(appUser);

        Mockito.when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        walletService.deposit(walletId, amount);

        Assertions.assertEquals(amount, wallet.getBalance());

        Mockito.verify(transactionRepository)
                .save(Mockito.any(Transaction.class));
    }
}
