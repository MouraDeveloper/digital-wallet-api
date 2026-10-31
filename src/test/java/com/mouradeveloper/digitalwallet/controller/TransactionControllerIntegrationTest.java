package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.TransactionDto;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.models.entity.Transaction;
import com.mouradeveloper.digitalwallet.models.entity.Wallet;
import com.mouradeveloper.digitalwallet.repository.AppUserRepository;
import com.mouradeveloper.digitalwallet.repository.TransactionRepository;
import com.mouradeveloper.digitalwallet.repository.WalletRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class TransactionControllerIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private WalletRepository walletRepository;


    @Test
    @Transactional
    void shouldCreateTransferAndReturnCreated() throws Exception {
        var sourceAppUser = createUser("test123@gmail.com");
        var destinationAppUser = createUser("test456@gmail.com");
        var savedAppUsers = appUserRepository.saveAllAndFlush(List.of(sourceAppUser, destinationAppUser));

        var sourceWallet = Wallet.create(savedAppUsers.getFirst());
        sourceWallet.deposit(BigDecimal.valueOf(1000));
        var destinationWallet = Wallet.create(savedAppUsers.getLast());
        var savedWallets = walletRepository.saveAllAndFlush(List.of(sourceWallet, destinationWallet));

        var savedSourceWallet = savedWallets.getFirst();
        var savedDestinationWallet = savedWallets.getLast();

        var transferRequest = new TransactionDto.TransferRequest(
                savedSourceWallet.getId(),
                savedDestinationWallet.getId(),
                BigDecimal.valueOf(100)
        );

        var json = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transactions/create-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.sourceWalletId")
                        .value(savedSourceWallet.getId().toString()))
                .andExpect(jsonPath("$.destinationWalletId")
                        .value(savedDestinationWallet.getId().toString()))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.createdAt").exists());

        var updatedSourceWallet = walletRepository.findById(savedSourceWallet.getId())
                .orElseThrow();

        var updatedDestinationWallet = walletRepository.findById(savedDestinationWallet.getId())
                .orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(900).compareTo(updatedSourceWallet.getBalance())
        );

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(100).compareTo(updatedDestinationWallet.getBalance())
        );

        Assertions.assertEquals(1, transactionRepository.count());
    }

    @Test
    @Transactional
    void shouldFindTransactionByIdAndReturnOk() throws Exception {
        var sourceUser = appUserRepository.saveAndFlush(
                createUser("source@gmail.com")
        );

        var destinationUser = appUserRepository.saveAndFlush(
                createUser("destination@gmail.com")
        );

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(500));

        var destinationWallet = Wallet.create(destinationUser);

        var savedWallets = walletRepository.saveAllAndFlush(
                List.of(sourceWallet, destinationWallet)
        );

        var savedSourceWallet = savedWallets.getFirst();
        var savedDestinationWallet = savedWallets.getLast();

        var transaction = Transaction.transfer(
                savedSourceWallet,
                savedDestinationWallet,
                BigDecimal.valueOf(100)
        );

        var savedTransaction = transactionRepository.saveAndFlush(transaction);

        mockMvc.perform(get("/transactions/{id}", savedTransaction.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTransaction.getId()))
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.sourceWalletId")
                        .value(savedSourceWallet.getId().toString()))
                .andExpect(jsonPath("$.destinationWalletId")
                        .value(savedDestinationWallet.getId().toString()))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Transactional
    void shouldFindTransactionsByWalletIdAndReturnOk() throws Exception {
        var sourceUser = appUserRepository.saveAndFlush(
                createUser("source@gmail.com")
        );

        var destinationUser = appUserRepository.saveAndFlush(
                createUser("destination@gmail.com")
        );

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(500));

        var destinationWallet = Wallet.create(destinationUser);

        var savedWallets = walletRepository.saveAllAndFlush(
                List.of(sourceWallet, destinationWallet)
        );

        var savedSourceWallet = savedWallets.getFirst();
        var savedDestinationWallet = savedWallets.getLast();

        var transaction = Transaction.transfer(
                savedSourceWallet,
                savedDestinationWallet,
                BigDecimal.valueOf(100)
        );

        transactionRepository.saveAndFlush(transaction);

        mockMvc.perform(get("/transactions/wallet/{walletId}", savedSourceWallet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].type").value("TRANSFER"))
                .andExpect(jsonPath("$[0].sourceWalletId")
                        .value(savedSourceWallet.getId().toString()))
                .andExpect(jsonPath("$[0].destinationWalletId")
                        .value(savedDestinationWallet.getId().toString()))
                .andExpect(jsonPath("$[0].amount").value(100))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }


    private AppUser createUser(String email) {
        return AppUser.create(
                "test",
                email,
                "password123"
        );
    }

}
