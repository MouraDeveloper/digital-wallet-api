package com.mouradeveloper.digitalwallet.repository;

import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@DataJpaTest
public class WalletRepositoryIntegrationTest {

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
    private AppUserRepository appUserRepository;


    @Test
    void shouldSaveAndFindWalletById() {
        var appUser = createValidAppUser();
        appUserRepository.save(appUser);

        var wallet = Wallet.create(appUser);
        walletRepository.save(wallet);

        var wallet1 = walletRepository.findById(wallet.getId());

        Assertions.assertTrue(wallet1.isPresent());
        Assertions.assertEquals(wallet.getId(), wallet1.get().getId());
        Assertions.assertEquals(appUser.getId(), wallet1.get().getUser().getId());
    }

    @Test
    void shouldNotAllowMoreThanOneWalletForSameUser() {
        var appUser = createValidAppUser();
        appUserRepository.save(appUser);

        var firstWallet = Wallet.create(appUser);
        walletRepository.save(firstWallet);

        var secondWallet = Wallet.create(appUser);

        Assertions.assertThrows(
                DataIntegrityViolationException.class, () -> walletRepository.saveAndFlush(secondWallet)
        );
    }


    private AppUser createValidAppUser() {
        return AppUser.create(
                "test",
                "test123@gmail.com",
                "test123"
        );
    }
}
