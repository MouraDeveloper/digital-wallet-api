package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.AppUserDto;
import com.mouradeveloper.digitalwallet.models.dto.auth.Login;
import com.mouradeveloper.digitalwallet.models.entity.AppUser;
import com.mouradeveloper.digitalwallet.repository.AppUserRepository;
import org.junit.jupiter.api.Assertions;
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
public class AppUserControllerIntegrationTest {

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
                () -> "user-integration-test-secret-key-123456");

        registry.add("security.jwt.expiration",
                () -> "3600000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void shouldCreateUserAndReturnCreated() throws Exception {
        String email = "test123@gmail.com";

        AppUserDto.Request appUser = createAppUserDtoRequest(email);

        String json = objectMapper.writeValueAsString(appUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value(appUser.fullName()))
                .andExpect(jsonPath("$.email").value(appUser.email()))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        AppUser savedUser = appUserRepository.findByEmail(email);

        Assertions.assertNotNull(savedUser);
        Assertions.assertNotNull(savedUser.getId());
        Assertions.assertEquals(email, savedUser.getEmail());
        Assertions.assertEquals(appUser.fullName(), savedUser.getFullName());
    }

    @Test
    @Transactional
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        String email = "test123@gmail.com";

        AppUserDto.Request appUser = createAppUserDtoRequest(email);

        String json = objectMapper.writeValueAsString(appUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(appUser.email()))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(post("/users")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.path").value("/users"))
                .andExpect(jsonPath("$.message")
                        .value("Email " + email + " already exists."))
                .andExpect(jsonPath("$.timestamp").exists());

        AppUser savedUser = appUserRepository.findByEmail(email);

        Assertions.assertNotNull(savedUser);
        Assertions.assertEquals(email, savedUser.getEmail());
        Assertions.assertEquals(appUser.fullName(), savedUser.getFullName());
    }

    @Test
    @Transactional
    void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        AppUserDto.Request appUser = createAppUserDtoRequest("abc");

        String json = objectMapper.writeValueAsString(appUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/users"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @Transactional
    void shouldFindUserByIdAndReturnOk() throws Exception {
        AppUser appUser = createAppUser("test123@gmail.com");

        AppUser savedUser = appUserRepository.saveAndFlush(appUser);

        String token = login(savedUser.getEmail());

        mockMvc.perform(get("/users/{id}", savedUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.fullName")
                        .value(savedUser.getFullName()))
                .andExpect(jsonPath("$.email")
                        .value(savedUser.getEmail()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @Transactional
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        UUID uuid = UUID.randomUUID();

        AppUser user = createAppUser("notfound@gmail.com");
        appUserRepository.saveAndFlush(user);

        String token = login(user.getEmail());

        mockMvc.perform(get("/users/{id}", uuid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/users/" + uuid))
                .andExpect(jsonPath("$.message")
                        .value("User with ID " + uuid + " not found."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private String login(String email) throws Exception {
        Login.Request request = new Login.Request(
                email,
                "password123"
        );

        String json = objectMapper.writeValueAsString(request);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Login.Response loginResponse = objectMapper.readValue(
                response,
                Login.Response.class
        );

        return loginResponse.token();
    }

    private AppUser createAppUser(String email) {
        return AppUser.create(
                "test",
                email,
                passwordEncoder.encode("password123")
        );
    }

    private AppUserDto.Request createAppUserDtoRequest(String email) {
        return new AppUserDto.Request(
                "test",
                email,
                "password123"
        );
    }
}
