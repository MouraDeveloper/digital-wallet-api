package com.mouradeveloper.digitalwallet.controller;

import com.mouradeveloper.digitalwallet.models.dto.AppUserDto;
import com.mouradeveloper.digitalwallet.repository.AppUserRepository;
import org.junit.jupiter.api.Assertions;
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
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    @Transactional
    void shouldCreateUserAndReturnCreated() throws Exception {
        var email = "test123@gmail.com";

        var appUser = createAppUserDtoRequest(email);

        var json = objectMapper.writeValueAsString(appUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value(appUser.fullName()))
                .andExpect(jsonPath("$.email").value(appUser.email()))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        var savedUser = appUserRepository.findByEmail(email);

        Assertions.assertNotNull(savedUser);
        Assertions.assertNotNull(savedUser.getId());
        Assertions.assertEquals(email, savedUser.getEmail());
        Assertions.assertEquals(appUser.fullName(), savedUser.getFullName());
    }

    @Test
    @Transactional
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        var email = "test123@gmail.com";
        var appUser = createAppUserDtoRequest(email);

        var json = objectMapper.writeValueAsString(appUser);

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

        var savedUser = appUserRepository.findByEmail(email);

        Assertions.assertNotNull(savedUser);
        Assertions.assertEquals(email, savedUser.getEmail());
        Assertions.assertEquals(appUser.fullName(), savedUser.getFullName());
    }

    @Test
    @Transactional
    void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        var appUser = createAppUserDtoRequest("abc");

        var json = objectMapper.writeValueAsString(appUser);

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


    private AppUserDto.Request createAppUserDtoRequest(String email) {
        var fullName = "test";
        var password = "password123";

        return new AppUserDto.Request(
                fullName,
                email,
                password
        );
    }
}
