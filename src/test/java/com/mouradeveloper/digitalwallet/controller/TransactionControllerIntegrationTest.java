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
                        .header("Idempotency-Key", "test-idempotency-key")
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
                BigDecimal.valueOf(100), "test-idempotency-key"
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
                BigDecimal.valueOf(100), "test-idempotency-key"
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

    @Test
    @Transactional
    void shouldReturnNotFoundWhenTransactionDoesNotExist() throws Exception {
        var transactionId = 999999L;

        mockMvc.perform(get("/transactions/{id}", transactionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/transactions/" + transactionId))
                .andExpect(jsonPath("$.message")
                        .value("Transaction with ID " + transactionId + " not found."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @Transactional
    void shouldReturnUnprocessableContentWhenSourceHasInsufficientFunds() throws Exception {
        var sourceUser = appUserRepository.saveAndFlush(
                createUser("source@gmail.com")
        );

        var destinationUser = appUserRepository.saveAndFlush(
                createUser("destination@gmail.com")
        );

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(50));

        var destinationWallet = Wallet.create(destinationUser);

        var savedWallets = walletRepository.saveAllAndFlush(
                List.of(sourceWallet, destinationWallet)
        );

        var savedSourceWallet = savedWallets.getFirst();
        var savedDestinationWallet = savedWallets.getLast();

        var transferRequest = new TransactionDto.TransferRequest(
                savedSourceWallet.getId(),
                savedDestinationWallet.getId(),
                BigDecimal.valueOf(100)
        );

        var json = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transactions/create-transfer")
                        .header("Idempotency-Key", "test-idempotency-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Content"))
                .andExpect(jsonPath("$.path").value("/transactions/create-transfer"))
                .andExpect(jsonPath("$.message").value("Insufficient funds."))
                .andExpect(jsonPath("$.timestamp").exists());

        Assertions.assertEquals(0, transactionRepository.count());

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(50).compareTo(savedSourceWallet.getBalance())
        );

        Assertions.assertEquals(
                0,
                BigDecimal.ZERO.compareTo(savedDestinationWallet.getBalance())
        );
    }

    @Test
    void shouldReturnSameTransactionWhenIdempotencyKeyIsRepeated() throws Exception {
        var idempotencyKey = "test-idempotency-key";

        var sourceUser = appUserRepository.saveAndFlush(
                createUser("test123@example.com"));

        var destinationUser = appUserRepository.saveAndFlush(
                createUser("test454@example.com"));

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(100));
        walletRepository.saveAndFlush(sourceWallet);
        var destinationWallet = walletRepository.saveAndFlush(Wallet.create(destinationUser));

        var transferRequest = new TransactionDto.TransferRequest(
                sourceWallet.getId(),
                destinationWallet.getId(),
                BigDecimal.valueOf(50)
        );

        var json = objectMapper.writeValueAsString(transferRequest);

        var mockPrimary = mockMvc.perform(post("/transactions/create-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        var mockSec = mockMvc.perform(post("/transactions/create-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(json))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(status().isCreated());

        String mockBodyPrimary = mockPrimary.andReturn().getResponse().getContentAsString();
        String mockBodySec = mockSec.andReturn().getResponse().getContentAsString();

        var firstResponse = objectMapper.readValue(mockBodyPrimary, TransactionDto.Response.class);
        var secondResponse = objectMapper.readValue(mockBodySec, TransactionDto.Response.class);

        Assertions.assertEquals(
                firstResponse.id(),
                secondResponse.id()
        );

        Assertions.assertEquals(1, transactionRepository.count());

        var walletSource = walletRepository.findById(sourceWallet.getId()).orElseThrow();
        var walletDestination = walletRepository.findById(destinationWallet.getId()).orElseThrow();

        Assertions.assertEquals(
                0, BigDecimal.valueOf(50).compareTo(walletSource.getBalance())
        );

        Assertions.assertEquals(
                0, BigDecimal.valueOf(50).compareTo(walletDestination.getBalance())
        );
    }

    @Test
    void shouldReturnConflictWhenIdempotencyKeyIsReusedWithDifferentPayload() throws Exception {
        var idempotencyKey = "test-idempotency-key";

        var sourceUser = appUserRepository.saveAndFlush(
                createUser("test123@example.com")
        );

        var destinationUser = appUserRepository.saveAndFlush(
                createUser("test454@example.com")
        );

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(100));
        walletRepository.saveAndFlush(sourceWallet);

        var destinationWallet = walletRepository.saveAndFlush(
                Wallet.create(destinationUser)
        );

        var transferRequestOne = new TransactionDto.TransferRequest(
                sourceWallet.getId(),
                destinationWallet.getId(),
                BigDecimal.valueOf(50)
        );

        var transferRequestSec = new TransactionDto.TransferRequest(
                sourceWallet.getId(),
                destinationWallet.getId(),
                BigDecimal.valueOf(60)
        );

        var json = objectMapper.writeValueAsString(transferRequestOne);
        var jsonSec = objectMapper.writeValueAsString(transferRequestSec);

        mockMvc.perform(post("/transactions/create-transfer")
                        .header("Idempotency-Key", idempotencyKey)
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/transactions/create-transfer")
                        .header("Idempotency-Key", idempotencyKey)
                        .content(jsonSec)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Idempotency key already used with different request data."))
                .andExpect(jsonPath("$.path")
                        .value("/transactions/create-transfer"))
                .andExpect(jsonPath("$.timestamp").exists());

        Assertions.assertEquals(
                1,
                transactionRepository.count()
        );

        var walletSource = walletRepository
                .findById(sourceWallet.getId())
                .orElseThrow();

        var walletDestination = walletRepository
                .findById(destinationWallet.getId())
                .orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(50).compareTo(walletSource.getBalance())
        );

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(50).compareTo(walletDestination.getBalance())
        );
    }

    @Test
    void shouldReturnBadRequestWhenIdempotencyKeyIsBlank() throws Exception {
        var sourceUser = appUserRepository.saveAndFlush(
                createUser("blank-key-source@example.com")
        );

        var destinationUser = appUserRepository.saveAndFlush(
                createUser("blank-key-destination@example.com")
        );

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(100));
        walletRepository.saveAndFlush(sourceWallet);

        var destinationWallet = walletRepository.saveAndFlush(
                Wallet.create(destinationUser)
        );

        var transferRequest = new TransactionDto.TransferRequest(
                sourceWallet.getId(),
                destinationWallet.getId(),
                BigDecimal.valueOf(30)
        );

        var json = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transactions/create-transfer")
                        .header("Idempotency-Key", " ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Idempotency key must not be blank or null"))
                .andExpect(jsonPath("$.path")
                        .value("/transactions/create-transfer"))
                .andExpect(jsonPath("$.timestamp").exists());

        Assertions.assertEquals(
                0,
                transactionRepository.count()
        );

        var updatedSourceWallet = walletRepository
                .findById(sourceWallet.getId())
                .orElseThrow();

        var updatedDestinationWallet = walletRepository
                .findById(destinationWallet.getId())
                .orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(100)
                        .compareTo(updatedSourceWallet.getBalance())
        );

        Assertions.assertEquals(
                0,
                BigDecimal.ZERO
                        .compareTo(updatedDestinationWallet.getBalance())
        );
    }

    @Test
    void shouldReturnBadRequestWhenIdempotencyKeyHeaderIsMissing() throws Exception {
        var sourceUser = appUserRepository.saveAndFlush(
                createUser("missing-key-source@example.com")
        );

        var destinationUser = appUserRepository.saveAndFlush(
                createUser("missing-key-destination@example.com")
        );

        var sourceWallet = Wallet.create(sourceUser);
        sourceWallet.deposit(BigDecimal.valueOf(100));
        walletRepository.saveAndFlush(sourceWallet);

        var destinationWallet = walletRepository.saveAndFlush(
                Wallet.create(destinationUser)
        );

        var transferRequest = new TransactionDto.TransferRequest(
                sourceWallet.getId(),
                destinationWallet.getId(),
                BigDecimal.valueOf(30)
        );

        var json = objectMapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transactions/create-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        Assertions.assertEquals(
                0,
                transactionRepository.count()
        );

        var updatedSourceWallet = walletRepository
                .findById(sourceWallet.getId())
                .orElseThrow();

        var updatedDestinationWallet = walletRepository
                .findById(destinationWallet.getId())
                .orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(100)
                        .compareTo(updatedSourceWallet.getBalance())
        );

        Assertions.assertEquals(
                0,
                BigDecimal.ZERO
                        .compareTo(updatedDestinationWallet.getBalance())
        );
    }

    @Test
    @Transactional
    void shouldCreateDepositAndReturnCreated() throws Exception {
        var appUser = appUserRepository.saveAndFlush(createUser("test123@example.com"));
        var wallet = walletRepository.saveAndFlush(Wallet.create(appUser));

        var depositRequest = new TransactionDto.DepositRequest(
                wallet.getId(),
                BigDecimal.valueOf(200)
        );

        var json = objectMapper.writeValueAsString(depositRequest);

        mockMvc.perform(post("/transactions/create-deposit")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.destinationWalletId").value(wallet.getId().toString()))
                .andExpect(jsonPath("$.amount").value(200))
                .andExpect(jsonPath("$.sourceWalletId").isEmpty())
                .andExpect(jsonPath("$.createdAt").exists());

        Assertions.assertEquals(1, transactionRepository.count());

        var walletAfterDeposit = walletRepository.findById(wallet.getId()).orElseThrow();

        Assertions.assertEquals(
                0,
                BigDecimal.valueOf(200).compareTo(walletAfterDeposit.getBalance())
        );
    }


    private AppUser createUser(String email) {
        return AppUser.create(
                "test",
                email,
                "password123"
        );
    }

}
