package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.business.InsufficientFundsException;
import com.mouradeveloper.digitalwallet.exception.business.SameWalletTransferException;
import com.mouradeveloper.digitalwallet.exception.notFound.WalletNotFoundException;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.models.enums.TypeEnum;
import com.mouradeveloper.digitalwallet.repository.AppUserRepository;
import com.mouradeveloper.digitalwallet.repository.TransactionRepository;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Testcontainers
@SpringBootTest
public class TransactionServiceIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configuraProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;


    @Test
    @Transactional
    void shouldTransferBalanceBetweenWalletsAndPersistTransaction() {
        var amount = BigDecimal.valueOf(50.00);

        var sourceUser = createAppUser("test1@gmail.com");
        var destinationUser = createAppUser("test2@gmail.com");

        appUserRepository.saveAll(List.of(sourceUser, destinationUser));

        var sourceWallet = Wallet.create(sourceUser);
        var destinationWallet = Wallet.create(destinationUser);
        walletRepository.saveAll(List.of(sourceWallet, destinationWallet));

        sourceWallet.deposit(BigDecimal.valueOf(100.00));

        transactionService.createTransfer(
                sourceWallet.getId(),
                destinationWallet.getId(),
                amount
        );

        Assertions.assertEquals(amount, sourceWallet.getBalance());
        Assertions.assertEquals(amount, destinationWallet.getBalance());

        var transactions = transactionRepository
                .findBySourceWallet_IdOrDestinationWallet_Id(
                        sourceWallet.getId(),
                        sourceWallet.getId()
                );

        Assertions.assertEquals(1, transactions.size());

        var transaction = transactions.getFirst();

        Assertions.assertEquals(TypeEnum.TRANSFER, transaction.getType());
        Assertions.assertEquals(amount, transaction.getAmount());
        Assertions.assertEquals(sourceWallet.getId(), transaction.getSourceWallet().getId());
        Assertions.assertEquals(destinationWallet.getId(), transaction.getDestinationWallet().getId());
    }

    @Test
    @Transactional
    void shouldNotTransferWhenSourceHasInsufficientBalance() {
        var amount = BigDecimal.valueOf(100.00);

        var sourceUser = createAppUser("test1@gmail.com");
        var destinationUser = createAppUser("test2@gmail.com");

        appUserRepository.saveAll(List.of(sourceUser, destinationUser));

        var sourceWallet = Wallet.create(sourceUser);
        var destinationWallet = Wallet.create(destinationUser);
        walletRepository.saveAll(List.of(sourceWallet, destinationWallet));

        sourceWallet.deposit(BigDecimal.valueOf(50.00));

        Assertions.assertThrows(
                InsufficientFundsException.class, () -> transactionService.createTransfer(
                        sourceWallet.getId(),
                        destinationWallet.getId(),
                        amount
                )
        );

        Assertions.assertEquals(0, BigDecimal.valueOf(50.00).compareTo(sourceWallet.getBalance()));
        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(destinationWallet.getBalance()));
        Assertions.assertEquals(0, transactionRepository.count());
    }

    @Test
    @Transactional
    void shouldNotTransferWhenSourceAndDestinationAreSameWallet() {
        var amount = BigDecimal.valueOf(100.00);

        var sourceUser = createAppUser("test1@gmail.com");
        appUserRepository.save(sourceUser);

        var sourceWallet = Wallet.create(sourceUser);
        walletRepository.save(sourceWallet);

        sourceWallet.deposit(BigDecimal.valueOf(200.00));

        Assertions.assertThrows(
                SameWalletTransferException.class, () -> transactionService.createTransfer(
                        sourceWallet.getId(),
                        sourceWallet.getId(),
                        amount
                )
        );

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(200.00).compareTo(sourceWallet.getBalance())
        );
        Assertions.assertEquals(0, transactionRepository.count());
    }

    @Test
    @Transactional
    void shouldNotTransferWhenSourceWalletDoesNotExist() {
        var destinationUser = createAppUser("test1@gmail.com");
        appUserRepository.save(destinationUser);

        var wallet = Wallet.create(destinationUser);
        walletRepository.save(wallet);

        var sourceWalletId = UUID.randomUUID();

        Assertions.assertThrows(
                WalletNotFoundException.class, () -> transactionService.createTransfer(
                        sourceWalletId,
                        wallet.getId(),
                        BigDecimal.valueOf(100.00)
                )
        );

        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
        Assertions.assertEquals(0, transactionRepository.count());
    }


    private AppUser createAppUser(String email) {
        return AppUser.create("test user", email, "hashedPassword");
    }
}
