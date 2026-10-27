package com.mouradeveloper.digitalwallet.repository;

import com.mouradeveloper.digitalwallet.models.entity.AppUser;
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
public class AppUserRepositoryIntegrationTest {

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
    private AppUserRepository appUserRepository;


    @Test
    void shouldSaveAndFindUserByEmail() {
        var appUser = createValidAppUser();

        appUserRepository.save(appUser);

        AppUser user = appUserRepository.findByEmail(appUser.getEmail());

        Assertions.assertNotNull(user);
        Assertions.assertEquals(appUser.getEmail(), user.getEmail());
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        var appUser = createValidAppUser();
        appUserRepository.save(appUser);

        var exists = appUserRepository.existsByEmail(appUser.getEmail());

        Assertions.assertTrue(exists);
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        var appUser = createValidAppUser();

        Assertions.assertFalse(appUserRepository.existsByEmail(appUser.getEmail()));
    }

    @Test
    void shouldNotAllowDuplicateEmail() {
        var appUser = createValidAppUser();
        appUserRepository.save(appUser);

        var appuser2 = createValidAppUser();
        Assertions.assertThrows(
                DataIntegrityViolationException.class, () -> appUserRepository.saveAndFlush(appuser2)
        );
    }


    private AppUser createValidAppUser() {
        return  AppUser.create(
                "teste",
                "teste123@gmail.com",
                "teste123"
        );
    }
}
