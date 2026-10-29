package com.mouradeveloper.digitalwallet.repository;

import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Transaction;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

@Testcontainers
@DataJpaTest
public class TransactionRepositoryIntegrationTest {

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
    private TransactionRepository transactionRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Test
    void shouldFindTransactionWhenWalletIsSource() {
        var firstUser = createAppUser("firstUser@gmail.com");
        appUserRepository.save(firstUser);
        var secondUser = createAppUser("secondUser@gmail.com");
        appUserRepository.save(secondUser);

        var firstWallet = Wallet.create(firstUser);
        walletRepository.save(firstWallet);
        var secondWallet = Wallet.create(secondUser);
        walletRepository.save(secondWallet);

        var transaction = Transaction.transfer(
                firstWallet,
                secondWallet,
                BigDecimal.valueOf(100.00)
        );

        transactionRepository.save(transaction);

        var transactions = transactionRepository.findBySourceWallet_IdOrDestinationWallet_Id(
                firstWallet.getId(),
                firstWallet.getId()
        );

        Assertions.assertEquals(1, transactions.size());
        Assertions.assertEquals(transaction.getId(), transactions.getFirst().getId());
    }

    @Test
    void shouldFindTransactionWhenWalletIsDestination() {
        var firstUser = createAppUser("email01@gmail.com");
        appUserRepository.save(firstUser);
        var secondUser = createAppUser("email02@gmail.com");
        appUserRepository.save(secondUser);

        var firstWallet = Wallet.create(firstUser);
        walletRepository.save(firstWallet);
        var secondWallet = Wallet.create(secondUser);
        walletRepository.save(secondWallet);

        var transaction = Transaction.transfer(
                firstWallet,
                secondWallet,
                BigDecimal.valueOf(100.00)
        );

        transactionRepository.save(transaction);

        var transactions = transactionRepository.findBySourceWallet_IdOrDestinationWallet_Id(
                secondWallet.getId(), secondWallet.getId());

        Assertions.assertEquals(1, transactions.size());

        Assertions.assertEquals(transaction.getId(), transactions.getFirst().getId());
    }

    @Test
    void shouldNotAllowTransactionWithZeroAmount() {
        var firstUser = createAppUser("test1@gmail.com");
        appUserRepository.save(firstUser);
        var secondUser = createAppUser("test2@gmail.com");
        appUserRepository.save(secondUser);

        var sourceWallet = Wallet.create(firstUser);
        walletRepository.save(sourceWallet);
        var destinationWallet = Wallet.create(secondUser);
        walletRepository.save(destinationWallet);

        Assertions.assertThrows(
                DataIntegrityViolationException.class, () -> {
                    jdbcTemplate.update(
                            "INSERT INTO transactions (type, source_wallet_id, destination_wallet_id, amount) "
                                    + "VALUES (?, ?, ?, ?)",
                            "TRANSFER",
                            sourceWallet.getId(),
                            destinationWallet.getId(),
                            BigDecimal.ZERO);
                }
        );
    }

    private AppUser createAppUser(String email) {
        return AppUser.create(
                "teste",
                email,
                "teste123"
        );
    }
}
