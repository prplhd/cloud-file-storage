package com.prplhd.cloudfilestorage.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prplhd.cloudfilestorage.dto.auth.SignInRequestDto;
import com.prplhd.cloudfilestorage.service.RegistrationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginIntegrationTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final String LOGIN_URL = "/api/auth/sign-in";
    private static final String VALID_USERNAME = "username";
    private static final String VALID_PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    void prepare() {
        registrationService.register(VALID_USERNAME, VALID_PASSWORD);
    }

    @AfterAll
    void clean() {
        jdbcTemplate.execute("TRUNCATE TABLE users");
    }

    @Test
    @DisplayName("Returns 200 when signing in with valid credentials")
    void whenUserSignsIn_withValidCredentials_thenReturnsOk() throws Exception {
        SignInRequestDto requestDto = new SignInRequestDto(VALID_USERNAME, VALID_PASSWORD);
        String json = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(requestDto.username()));
    }

    @Test
    @DisplayName("Returns 400 when signin with invalid credentials")
    void whenUserSignsIn_withInvalidCredentials_thenReturnsBadRequest() throws Exception {
        SignInRequestDto requestDtoWithInvalidUsername = new SignInRequestDto("!!", VALID_PASSWORD);
        String jsonWithInvalidUsername = objectMapper.writeValueAsString(requestDtoWithInvalidUsername);

        mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonWithInvalidUsername)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty());

        SignInRequestDto requestDtoWithInvalidPassword = new SignInRequestDto(VALID_USERNAME, "??");
        String jsonWithInvalidPassword = objectMapper.writeValueAsString(requestDtoWithInvalidPassword);

        mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonWithInvalidPassword)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Returns 401 when signing in with incorrect credentials")
    void whenUserSignsIn_withInvalidCredentials_thenReturnsUnauthorized() throws Exception {
        SignInRequestDto requestDtoWithUnknownUsername = new SignInRequestDto("rand0mus3rnaMe", VALID_PASSWORD);
        String jsonWithUnknownUsername = objectMapper.writeValueAsString(requestDtoWithUnknownUsername);

        mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonWithUnknownUsername)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty());

        SignInRequestDto requestDtoWithIncorrectPassword = new SignInRequestDto(VALID_USERNAME, "rand0mpa$$worD");
        String jsonWithIncorrectPassword = objectMapper.writeValueAsString(requestDtoWithIncorrectPassword);

        mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonWithIncorrectPassword)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
