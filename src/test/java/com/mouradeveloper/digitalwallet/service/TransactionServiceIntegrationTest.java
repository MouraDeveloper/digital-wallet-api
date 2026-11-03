package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.business.InsufficientFundsException;
import com.mouradeveloper.digitalwallet.exception.business.SameWalletTransferException;
import com.mouradeveloper.digitalwallet.exception.conflict.IdempotencyConflictException;
import com.mouradeveloper.digitalwallet.exception.notFound.WalletNotFoundException;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.models.enums.TypeEnum;
import com.mouradeveloper.digitalwallet.repository.AppUserRepository;
import com.mouradeveloper.digitalwallet.repository.TransactionRepository;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

@Testcontainers
@SpringBootTest
public class TransactionServiceIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
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

    @AfterEach
    void cleanUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        appUserRepository.deleteAll();
    }


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
                amount,
                "test-idempotency-key"
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
                        amount,
                        "test-idempotency-key"
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
                        amount,
                        "test-idempotency-key"
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
                        BigDecimal.valueOf(100.00),
                        "test-idempotency-key"
                )
        );

        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
        Assertions.assertEquals(0, transactionRepository.count());
    }

    @Test
    @Transactional
    void shouldNotTransferWhenDestinationWalletDoesNotExist() {
        var sourceUser = createAppUser("test1@gmail.com");
        appUserRepository.save(sourceUser);

        var wallet = Wallet.create(sourceUser);
        walletRepository.save(wallet);

        var destinationWalletId = UUID.randomUUID();

        Assertions.assertThrows(
                WalletNotFoundException.class, () -> transactionService.createTransfer(
                        wallet.getId(),
                        destinationWalletId,
                        BigDecimal.valueOf(100.00),
                        "test-idempotency-key"
                )
        );

        Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
        Assertions.assertEquals(0, transactionRepository.count());
    }

    @Test
    void shouldPreventConcurrentTransfersFromOverspending() throws Exception {
        var sourceUser = appUserRepository.saveAndFlush(
                AppUser.create("Source User", "source.concurrent@gmail.com", "password123")
        );

        var destinationUserA = appUserRepository.saveAndFlush(
                AppUser.create("Destination A", "destination.a@gmail.com", "password123")
        );

        var destinationUserB = appUserRepository.saveAndFlush(
                AppUser.create("Destination B", "destination.b@gmail.com", "password123")
        );

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(100));

        var destinationWalletA = Wallet.create(destinationUserA);
        var destinationWalletB = Wallet.create(destinationUserB);

        var savedWallets = walletRepository.saveAllAndFlush(
                List.of(sourceWallet, destinationWalletA, destinationWalletB)
        );

        var savedSourceWallet = savedWallets.get(0);
        var savedDestinationWalletA = savedWallets.get(1);
        var savedDestinationWalletB = savedWallets.get(2);

        var executor = Executors.newFixedThreadPool(2);

        var readyLatch = new CountDownLatch(2);
        var startLatch = new CountDownLatch(1);

        var successCount = 0;
        var insufficientFundsCount = 0;

        try {
            var transferA = executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                return transactionService.createTransfer(
                        savedSourceWallet.getId(),
                        savedDestinationWalletA.getId(),
                        BigDecimal.valueOf(80),
                        "concurrent-transfer-a"
                );
            });

            var transferB = executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                return transactionService.createTransfer(
                        savedSourceWallet.getId(),
                        savedDestinationWalletB.getId(),
                        BigDecimal.valueOf(80),
                        "concurrent-transfer-b"
                );
            });

            readyLatch.await();

            startLatch.countDown();

            try {
                transferA.get();
                successCount++;
            } catch (ExecutionException e) {
                Assertions.assertInstanceOf(
                        InsufficientFundsException.class,
                        e.getCause()
                );
                insufficientFundsCount++;
            }

            try {
                transferB.get();
                successCount++;
            } catch (ExecutionException e) {
                Assertions.assertInstanceOf(
                        InsufficientFundsException.class,
                        e.getCause()
                );
                insufficientFundsCount++;
            }
        } finally {
            executor.shutdown();
        }

        Assertions.assertEquals(1, successCount);
        Assertions.assertEquals(1, insufficientFundsCount);
        Assertions.assertEquals(1, transactionRepository.count());

        var updatedSourceWallet = walletRepository
                .findById(savedSourceWallet.getId())
                .orElseThrow();

        var updatedDestinationWalletA = walletRepository
                .findById(savedDestinationWalletA.getId())
                .orElseThrow();

        var updatedDestinationWalletB = walletRepository
                .findById(savedDestinationWalletB.getId())
                .orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(20)
                        .compareTo(updatedSourceWallet.getBalance())
        );

        var destinationTotal = updatedDestinationWalletA.getBalance()
                .add(updatedDestinationWalletB.getBalance());

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(80)
                        .compareTo(destinationTotal)
        );
    }

    @Test
    void shouldReturnSameTransactionWhenIdempotencyKeyIsRepeated() {
        var idempotencyKey = "unique-idempotency-key";
        var amount = BigDecimal.valueOf(30.00);

        var appUserSource = appUserRepository.saveAndFlush(
                createAppUser("test-source@example.com")
        );
        var appUserDestination = appUserRepository.saveAndFlush(
                createAppUser("test-destination@example.com")
        );

        var walletSource = Wallet.create(appUserSource);
        walletSource.deposit(BigDecimal.valueOf(100.00));
        walletRepository.saveAndFlush(walletSource);
        var walletDestination = walletRepository.saveAndFlush(Wallet.create(appUserDestination));

        var firstResponse = transactionService.createTransfer(
                walletSource.getId(),
                walletDestination.getId(),
                amount,
                idempotencyKey
        );

        var secondResponse = transactionService.createTransfer(
                walletSource.getId(),
                walletDestination.getId(),
                amount,
                idempotencyKey
        );


        Assertions.assertEquals(firstResponse.id(), secondResponse.id());
        Assertions.assertEquals(1, transactionRepository.count());

        var walletSourceUpdated = walletRepository.findById(walletSource.getId()).orElseThrow();
        var walletDestinationUpdated = walletRepository.findById(walletDestination.getId()).orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(70.00).compareTo(walletSourceUpdated.getBalance())
        );

        Assertions.assertEquals(
                0,
                amount.compareTo(walletDestinationUpdated.getBalance())
        );
    }

    @Test
    @Transactional
    void shouldThrowConflictWhenIdempotencyKeyIsReusedWithDifferentPayload() {
        var idempotencyKey = "conflict-idempotency-key";

        var sourceAppUser = appUserRepository.saveAndFlush(
                createAppUser("test123@example.com")
        );
        var destinationAppUser = appUserRepository.saveAndFlush(
                createAppUser("test132@example.com")
        );

        var walletSource = Wallet.create(sourceAppUser);
        walletSource.deposit(BigDecimal.valueOf(100.00));
        walletRepository.saveAndFlush(walletSource);
        var walletDestination = walletRepository.saveAndFlush(Wallet.create(destinationAppUser));

        transactionService.createTransfer(
                walletSource.getId(),
                walletDestination.getId(),
                BigDecimal.valueOf(30.00),
                idempotencyKey
        );

        Assertions.assertThrows(
                IdempotencyConflictException.class, () -> transactionService.createTransfer(
                        walletSource.getId(),
                        walletDestination.getId(),
                        BigDecimal.valueOf(40.00),
                        idempotencyKey
                )
        );

        Assertions.assertEquals(1, transactionRepository.count());

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(70.00).compareTo(walletSource.getBalance())
        );

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(30.00).compareTo(walletDestination.getBalance())
        );
    }


    private AppUser createAppUser(String email) {
        return AppUser.create("test user", email, "hashedPassword");
    }
}
