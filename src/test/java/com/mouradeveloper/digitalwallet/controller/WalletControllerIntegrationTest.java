package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.TransactionDto;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.repository.AppUserRepository;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
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
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private WalletRepository walletRepository;


    @Test
    @Transactional
    void shouldFindWalletByIdAndReturnOk() throws Exception {
        var appUser = createAppUser("test123@gmail.com");
        var savedUser = appUserRepository.saveAndFlush(appUser);

        var wallet = Wallet.create(savedUser);
        var savedWallet = walletRepository.saveAndFlush(wallet);

        mockMvc.perform(get("/wallets/{id}", savedWallet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedWallet.getId().toString()))
                .andExpect(jsonPath("$.userId").value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.balance").value(savedWallet.getBalance().doubleValue()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Transactional
    void shouldReturnNotFoundWhenWalletDoesNotExist() throws Exception {
        var uuid = UUID.randomUUID();

        mockMvc.perform(get("/wallets/{id}", uuid))
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
    void shouldDepositAmountAndReturnOk() throws Exception {
        var user = createAppUser("test123@gmail.com");
        var savedUser = appUserRepository.saveAndFlush(user);
        var wallet = Wallet.create(savedUser);
        var savedWallet = walletRepository.saveAndFlush(wallet);

        var depositRequest = new TransactionDto.DepositRequest(
                savedWallet.getId(),
                BigDecimal.valueOf(100)
        );

        var json = objectMapper.writeValueAsString(depositRequest);

        mockMvc.perform(post("/wallets/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.balance").value(100))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.id").value(savedWallet.getId().toString()));
    }

    @Test
    @Transactional
    void shouldReturnNotFoundWhenDepositingIntoNonexistentWallet() throws Exception {
        var uuid = UUID.randomUUID();

        var depositRequest = new TransactionDto.DepositRequest(
                uuid,
                BigDecimal.valueOf(100)
        );

        var json = objectMapper.writeValueAsString(depositRequest);

        mockMvc.perform(post("/wallets/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/wallets/deposit"))
                .andExpect(jsonPath("$.message")
                        .value("Wallet with ID " + uuid + " not found."))
                .andExpect(jsonPath("$.timestamp").exists());
    }


    private AppUser createAppUser(String email) {
        return AppUser.create(
                "Test",
                email,
                "password123"
        );
    }
}
