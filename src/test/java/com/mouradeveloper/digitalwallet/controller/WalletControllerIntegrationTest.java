
package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.auth.Login;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.repository.AppUserRepository;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class WalletControllerIntegrationTest {

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

        registry.add("security.jwt.secret",
                () -> "wallet-integration-test-secret-key-123456");

        registry.add("security.jwt.expiration",
                () -> "3600000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @Transactional
    void shouldFindWalletByIdAndReturnOk() throws Exception {
        AppUser appUser = createAppUser("test123@gmail.com");

        AppUser savedUser = appUserRepository.saveAndFlush(appUser);

        Wallet wallet = Wallet.create(savedUser);
        Wallet savedWallet = walletRepository.saveAndFlush(wallet);

        String token = login(savedUser.getEmail());

        mockMvc.perform(get("/wallets/{id}", savedWallet.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(savedWallet.getId().toString()))
                .andExpect(jsonPath("$.userId")
                        .value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.balance")
                        .value(savedWallet.getBalance().doubleValue()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Transactional
    void shouldReturnNotFoundWhenWalletDoesNotExist() throws Exception {
        UUID uuid = UUID.randomUUID();

        AppUser user = createAppUser("notfound@gmail.com");
        appUserRepository.saveAndFlush(user);

        String token = login(user.getEmail());

        mockMvc.perform(get("/wallets/{id}", uuid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Wallet with ID " + uuid + " not found."))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/wallets/" + uuid))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @Transactional
    void shouldReturnAuthenticatedUserWallet() throws Exception {
        AppUser user = createAppUser("wallet-me@example.com");
        AppUser savedUser = appUserRepository.saveAndFlush(user);

        Wallet wallet = Wallet.create(savedUser);
        Wallet savedWallet = walletRepository.saveAndFlush(wallet);

        String token = login(savedUser.getEmail());

        mockMvc.perform(get("/wallets/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(savedWallet.getId().toString()))
                .andExpect(jsonPath("$.userId")
                        .value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.balance")
                        .value(savedWallet.getBalance().doubleValue()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessingMyWalletWithoutToken()
            throws Exception {

        mockMvc.perform(get("/wallets/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void shouldReturnNotFoundWhenAuthenticatedUserHasNoWallet()
            throws Exception {

        AppUser user = createAppUser("no-wallet@example.com");
        AppUser savedUser = appUserRepository.saveAndFlush(user);

        String token = login(savedUser.getEmail());

        mockMvc.perform(get("/wallets/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Wallet not found for authenticated user."))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/wallets/me"))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    private String login(String email) throws Exception {
        String json = objectMapper.writeValueAsString(
                new Login.Request(email, "password123")
        );

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response)
                .get("token")
                .asText();
    }

    private AppUser createAppUser(String email) {
        return AppUser.create(
                "Test",
                email,
                passwordEncoder.encode("password123")
        );
    }
}
