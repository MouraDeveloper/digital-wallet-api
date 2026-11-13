package com.mouradeveloper.digitalwallet.service;

import com.mouradeveloper.digitalwallet.exception.badRequest.InvalidIdempotencyKeyException;
import com.mouradeveloper.digitalwallet.exception.business.InsufficientFundsException;
import com.mouradeveloper.digitalwallet.exception.business.InvalidAmountException;
import com.mouradeveloper.digitalwallet.exception.business.SameWalletTransferException;
import com.mouradeveloper.digitalwallet.exception.conflict.IdempotencyConflictException;
import com.mouradeveloper.digitalwallet.exception.notFound.WalletNotFoundException;
import com.mouradeveloper.digitalwallet.models.dto.TransactionDto;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.models.enums.TypeEnum;
import com.mouradeveloper.digitalwallet.repository.AppUserRepository;
import com.mouradeveloper.digitalwallet.repository.TransactionRepository;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import com.mouradeveloper.digitalwallet.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

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

        registry.add(
                "security.jwt.secret",
                () -> "transaction-service-integration-test-key"
        );
        registry.add("security.jwt.expiration", () -> "3600000");
    }

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

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

        mockAuthenticatedUser(sourceUser);

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

        Assertions.assertEquals(
                sourceWallet.getId(),
                transaction.getSourceWallet().getId()
        );

        Assertions.assertEquals(
                destinationWallet.getId(),
                transaction.getDestinationWallet().getId()
        );
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

        mockAuthenticatedUser(sourceUser);

        Assertions.assertThrows(
                InsufficientFundsException.class,
                () -> transactionService.createTransfer(
                        sourceWallet.getId(),
                        destinationWallet.getId(),
                        amount,
                        "test-idempotency-key"
                )
        );

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(50.00).compareTo(sourceWallet.getBalance())
        );

        Assertions.assertEquals(
                0,
                BigDecimal.ZERO.compareTo(destinationWallet.getBalance())
        );

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

        mockAuthenticatedUser(sourceUser);

        Assertions.assertThrows(
                SameWalletTransferException.class,
                () -> transactionService.createTransfer(
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

        mockAuthenticatedUser(destinationUser);

        Assertions.assertThrows(
                WalletNotFoundException.class,
                () -> transactionService.createTransfer(
                        sourceWalletId,
                        wallet.getId(),
                        BigDecimal.valueOf(100.00),
                        "test-idempotency-key"
                )
        );

        Assertions.assertEquals(
                0,
                BigDecimal.ZERO.compareTo(wallet.getBalance())
        );

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

        mockAuthenticatedUser(sourceUser);

        Assertions.assertThrows(
                WalletNotFoundException.class,
                () -> transactionService.createTransfer(
                        wallet.getId(),
                        destinationWalletId,
                        BigDecimal.valueOf(100.00),
                        "test-idempotency-key"
                )
        );

        Assertions.assertEquals(
                0,
                BigDecimal.ZERO.compareTo(wallet.getBalance())
        );

        Assertions.assertEquals(0, transactionRepository.count());
    }

    @Test
    void shouldPreventConcurrentTransfersFromOverspending() throws Exception {
        var sourceUser = appUserRepository.saveAndFlush(
                createAppUser("source.concurrent@gmail.com")
        );

        var destinationUserA = appUserRepository.saveAndFlush(
                createAppUser("destination.a@gmail.com")
        );

        var destinationUserB = appUserRepository.saveAndFlush(
                createAppUser("destination.b@gmail.com")
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

        mockAuthenticatedUser(sourceUser);

        var executor = Executors.newFixedThreadPool(2);

        var readyLatch = new CountDownLatch(2);
        var startLatch = new CountDownLatch(1);

        int successCount = 0;
        int insufficientFundsCount = 0;

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
                BigDecimal.valueOf(80).compareTo(destinationTotal)
        );
    }

    @Test
    void shouldReturnSameTransactionWhenIdempotencyKeyIsRepeated() {
        var idempotencyKey = "unique-idempotency-key";
        var amount = BigDecimal.valueOf(30.00);

        var sourceUser = appUserRepository.saveAndFlush(
                createAppUser("test-source@example.com")
        );

        var destinationUser = appUserRepository.saveAndFlush(
                createAppUser("test-destination@example.com")
        );

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(100.00));
        walletRepository.saveAndFlush(sourceWallet);

        var destinationWallet = walletRepository.saveAndFlush(
                Wallet.create(destinationUser)
        );

        mockAuthenticatedUser(sourceUser);

        var firstResponse = transactionService.createTransfer(
                sourceWallet.getId(),
                destinationWallet.getId(),
                amount,
                idempotencyKey
        );

        var secondResponse = transactionService.createTransfer(
                sourceWallet.getId(),
                destinationWallet.getId(),
                amount,
                idempotencyKey
        );

        Assertions.assertEquals(
                firstResponse.id(),
                secondResponse.id()
        );

        Assertions.assertEquals(1, transactionRepository.count());

        var sourceUpdated = walletRepository
                .findById(sourceWallet.getId())
                .orElseThrow();

        var destinationUpdated = walletRepository
                .findById(destinationWallet.getId())
                .orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(70.00)
                        .compareTo(sourceUpdated.getBalance())
        );

        Assertions.assertEquals(
                0,
                amount.compareTo(destinationUpdated.getBalance())
        );
    }

    @Test
    @Transactional
    void shouldThrowConflictWhenIdempotencyKeyIsReusedWithDifferentPayload() {
        var idempotencyKey = "conflict-idempotency-key";

        var sourceUser = appUserRepository.saveAndFlush(
                createAppUser("test123@example.com")
        );

        var destinationUser = appUserRepository.saveAndFlush(
                createAppUser("test132@example.com")
        );

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(100.00));
        walletRepository.saveAndFlush(sourceWallet);

        var destinationWallet = walletRepository.saveAndFlush(
                Wallet.create(destinationUser)
        );

        mockAuthenticatedUser(sourceUser);

        transactionService.createTransfer(
                sourceWallet.getId(),
                destinationWallet.getId(),
                BigDecimal.valueOf(30.00),
                idempotencyKey
        );

        Assertions.assertThrows(
                IdempotencyConflictException.class,
                () -> transactionService.createTransfer(
                        sourceWallet.getId(),
                        destinationWallet.getId(),
                        BigDecimal.valueOf(40.00),
                        idempotencyKey
                )
        );

        Assertions.assertEquals(1, transactionRepository.count());

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(70.00)
                        .compareTo(sourceWallet.getBalance())
        );

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(30.00)
                        .compareTo(destinationWallet.getBalance())
        );
    }

    @Test
    @Transactional
    void shouldThrowWhenIdempotencyKeyIsBlank() {
        assertInvalidIdempotencyKey(" ");
    }

    @Test
    @Transactional
    void shouldThrowWhenIdempotencyKeyIsNull() {
        assertInvalidIdempotencyKey(null);
    }

    @Test
    void shouldHandleConcurrentRetriesWithSameIdempotencyKey()
            throws ExecutionException, InterruptedException {

        var idempotencyKey = "concurrent-idempotency-key";
        var amount = BigDecimal.valueOf(30.00);

        var sourceUser = appUserRepository.saveAndFlush(
                createAppUser("test123@example.com")
        );

        var destinationUser = appUserRepository.saveAndFlush(
                createAppUser("test132@example.com")
        );

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(100.00));
        walletRepository.saveAndFlush(sourceWallet);

        var destinationWallet = walletRepository.saveAndFlush(
                Wallet.create(destinationUser)
        );

        mockAuthenticatedUser(sourceUser);

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        TransactionDto.Response firstResponse;
        TransactionDto.Response secondResponse;

        try {
            var firstFuture = executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                return transactionService.createTransfer(
                        sourceWallet.getId(),
                        destinationWallet.getId(),
                        amount,
                        idempotencyKey
                );
            });

            var secondFuture = executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                return transactionService.createTransfer(
                        sourceWallet.getId(),
                        destinationWallet.getId(),
                        amount,
                        idempotencyKey
                );
            });

            readyLatch.await();
            startLatch.countDown();

            firstResponse = firstFuture.get();
            secondResponse = secondFuture.get();
        } finally {
            executor.shutdown();
        }

        Assertions.assertEquals(
                firstResponse.id(),
                secondResponse.id()
        );

        Assertions.assertEquals(1, transactionRepository.count());

        var sourceUpdated = walletRepository
                .findById(sourceWallet.getId())
                .orElseThrow();

        var destinationUpdated = walletRepository
                .findById(destinationWallet.getId())
                .orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(70.00)
                        .compareTo(sourceUpdated.getBalance())
        );

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(30.00)
                        .compareTo(destinationUpdated.getBalance())
        );
    }

    @Test
    @Transactional
    void shouldCreateDepositAndUpdateWalletBalance() {
        var user = appUserRepository.saveAndFlush(
                createAppUser("teste123@gmail.com")
        );

        var wallet = walletRepository.saveAndFlush(Wallet.create(user));

        mockAuthenticatedUser(user);

        var transaction = transactionService.createDeposit(
                wallet.getId(),
                BigDecimal.valueOf(100.00),
                "deposit-idempotency-key"
        );

        var walletUpdated = walletRepository
                .findById(wallet.getId())
                .orElseThrow();

        Assertions.assertEquals(TypeEnum.DEPOSIT, transaction.type());

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(100.00)
                        .compareTo(walletUpdated.getBalance())
        );

        Assertions.assertEquals(1, transactionRepository.count());

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(100.00)
                        .compareTo(transaction.amount())
        );

        Assertions.assertEquals(
                walletUpdated.getId(),
                transaction.destinationWalletId()
        );

        Assertions.assertNull(transaction.sourceWalletId());
    }

    @Test
    @Transactional
    void shouldThrowWhenDepositDestinationWalletDoesNotExist() {
        var walletId = UUID.randomUUID();

        var user = appUserRepository.saveAndFlush(
                createAppUser("missing-deposit-wallet@example.com")
        );

        mockAuthenticatedUser(user);

        Assertions.assertThrows(
                WalletNotFoundException.class,
                () -> transactionService.createDeposit(
                        walletId,
                        BigDecimal.valueOf(100.00),
                        "deposit-idempotency-key"
                )
        );

        Assertions.assertEquals(0, transactionRepository.count());
    }

    @Test
    @Transactional
    void shouldThrowWhenDepositAmountIsZero() {
        assertInvalidDepositAmount(BigDecimal.ZERO);
    }

    @Test
    @Transactional
    void shouldThrowWhenDepositAmountIsNegative() {
        assertInvalidDepositAmount(BigDecimal.valueOf(-50.00));
    }

    @Test
    @Transactional
    void shouldReturnSameDepositWhenIdempotencyKeyIsRepeated() {
        var idempotencyKey = "deposit-idempotency-key";

        var user = appUserRepository.saveAndFlush(
                createAppUser("test123@gmail.com")
        );

        var wallet = walletRepository.saveAndFlush(Wallet.create(user));

        mockAuthenticatedUser(user);

        var firstDeposit = transactionService.createDeposit(
                wallet.getId(),
                BigDecimal.valueOf(100.00),
                idempotencyKey
        );

        var secondDeposit = transactionService.createDeposit(
                wallet.getId(),
                BigDecimal.valueOf(100.00),
                idempotencyKey
        );

        Assertions.assertEquals(firstDeposit.id(), secondDeposit.id());
        Assertions.assertEquals(1, transactionRepository.count());

        var walletUpdated = walletRepository
                .findById(wallet.getId())
                .orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(100.00)
                        .compareTo(walletUpdated.getBalance())
        );
    }

    @Test
    @Transactional
    void shouldThrowConflictWhenDepositIdempotencyKeyIsReusedWithDifferentPayload() {
        var idempotencyKey = "deposit-idempotency-key";

        var user = appUserRepository.saveAndFlush(
                createAppUser("test123@gmail.com")
        );

        var wallet = walletRepository.saveAndFlush(Wallet.create(user));

        mockAuthenticatedUser(user);

        transactionService.createDeposit(
                wallet.getId(),
                BigDecimal.valueOf(100.00),
                idempotencyKey
        );

        Assertions.assertThrows(
                IdempotencyConflictException.class,
                () -> transactionService.createDeposit(
                        wallet.getId(),
                        BigDecimal.valueOf(200.00),
                        idempotencyKey
                )
        );

        Assertions.assertEquals(1, transactionRepository.count());

        var walletUpdated = walletRepository
                .findById(wallet.getId())
                .orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(100.00)
                        .compareTo(walletUpdated.getBalance())
        );
    }

    private void assertInvalidDepositAmount(BigDecimal amount) {
        var user = appUserRepository.saveAndFlush(
                createAppUser("test123@example.com")
        );

        var wallet = walletRepository.saveAndFlush(Wallet.create(user));

        mockAuthenticatedUser(user);

        Assertions.assertThrows(
                InvalidAmountException.class,
                () -> transactionService.createDeposit(
                        wallet.getId(),
                        amount,
                        "deposit-idempotency-key"
                )
        );

        Assertions.assertEquals(0, transactionRepository.count());

        var updatedWallet = walletRepository
                .findById(wallet.getId())
                .orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.ZERO.compareTo(updatedWallet.getBalance())
        );
    }

    private void assertInvalidIdempotencyKey(String idempotencyKey) {
        var sourceUser = appUserRepository.saveAndFlush(
                createAppUser("test123@example.com")
        );

        var destinationUser = appUserRepository.saveAndFlush(
                createAppUser("test321@example.com")
        );

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(100.00));
        walletRepository.saveAndFlush(sourceWallet);

        var destinationWallet = walletRepository.saveAndFlush(
                Wallet.create(destinationUser)
        );

        mockAuthenticatedUser(sourceUser);

        Assertions.assertThrows(
                InvalidIdempotencyKeyException.class,
                () -> transactionService.createTransfer(
                        sourceWallet.getId(),
                        destinationWallet.getId(),
                        BigDecimal.valueOf(30.00),
                        idempotencyKey
                )
        );

        Assertions.assertEquals(0, transactionRepository.count());

        Assertions.assertEquals(
                0,
                BigDecimal.ZERO.compareTo(destinationWallet.getBalance())
        );

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(100.00)
                        .compareTo(sourceWallet.getBalance())
        );
    }

    private void mockAuthenticatedUser(AppUser user) {
        Mockito.when(authenticatedUserProvider.getUserId())
                .thenReturn(user.getId());
    }

    private AppUser createAppUser(String email) {
        return AppUser.create(
                "test user",
                email,
                "hashedPassword"
        );
    }
}
