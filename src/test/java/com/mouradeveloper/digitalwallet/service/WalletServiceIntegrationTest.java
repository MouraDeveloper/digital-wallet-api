package com.mouradeveloper.digitalwallet.service;


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

@Testcontainers
@SpringBootTest
public class WalletServiceIntegrationTest {

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
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AppUserRepository appUserRepository;


    @Test
    @Transactional
    void shouldDepositAmountAndPersistTransaction() {
        var user = createAppUser("test123@gmail.com");
        appUserRepository.save(user);

        var wallet = Wallet.create(user);
        walletRepository.save(wallet);

        walletService.deposit(
                wallet.getId(),
                BigDecimal.valueOf(100.00)
        );

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(100.00).compareTo(wallet.getBalance())
        );

        var transactions = transactionRepository.findBySourceWallet_IdOrDestinationWallet_Id(
                wallet.getId(),
                wallet.getId()
        );

        Assertions.assertEquals(1, transactions.size());
        var transaction = transactions.getFirst();

        Assertions.assertEquals(TypeEnum.DEPOSIT, transaction.getType());
        Assertions.assertEquals(0, BigDecimal.valueOf(100.00).compareTo(transaction.getAmount()));
        Assertions.assertNull(transaction.getSourceWallet());
        Assertions.assertEquals(wallet.getId(), transaction.getDestinationWallet().getId());
    }


    private AppUser createAppUser(String email) {
        return AppUser.create("test user", email, "hashedPassword");
    }
}
