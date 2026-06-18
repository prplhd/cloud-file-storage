package com.prplhd.cloudfilestorage.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prplhd.cloudfilestorage.dao.UserDao;
import com.prplhd.cloudfilestorage.dto.auth.SignUpRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RegistrationIntegrationTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final String REGISTRATION_URL = "/api/auth/sign-up";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void clean() {
        jdbcTemplate.execute("TRUNCATE TABLE users");
    }

    @Test
    @DisplayName("Creates a user when registering with valid credentials")
    void whenUserRegisters_withValidCredentials_thenUserIsCreated() throws Exception {
        SignUpRequestDto requestDto = new SignUpRequestDto("username", "password");
        String json = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(
                post(REGISTRATION_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(requestDto.username()));

        assertThat(userDao.findByUsername(requestDto.username())).isPresent();
    }

    @Test
    @DisplayName("Returns 409 when registering with a duplicate username")
    void whenUserRegisters_withDuplicateUsername_thenReturnsConflict() throws Exception {
        SignUpRequestDto requestDto = new SignUpRequestDto("username", "password");
        String json = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(
                        post(REGISTRATION_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(requestDto.username()));

        assertThat(userDao.findByUsername(requestDto.username())).isPresent();

        SignUpRequestDto requestDtoWithDuplicateUsername = new SignUpRequestDto("username", "anotherPass");
        String jsonWithDuplicateUsername = objectMapper.writeValueAsString(requestDtoWithDuplicateUsername);


        mockMvc.perform(
                        post(REGISTRATION_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonWithDuplicateUsername)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty());

        Long usersWithSameUsernameCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM users
                WHERE username = ?
                """,
                Long.class,
                requestDto.username()
        );

        assertThat(usersWithSameUsernameCount).isEqualTo(1L);
    }

}
