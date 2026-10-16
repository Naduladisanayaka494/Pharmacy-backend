package com.pharmacy.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.system.dto.LoginRequest;
import com.pharmacy.system.dto.RegisterRequest;
import com.pharmacy.system.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("AuthController REST Endpoints Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/register - Should register user and return 201 Created with JWT tokens")
    void registerEndpoint_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("test_integration_user")
                .password("password123")
                .fullName("Integration Test User")
                .role(Role.ROLE_CASHIER)
                .branchId(1L)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("test_integration_user"))
                .andExpect(jsonPath("$.role").value("ROLE_CASHIER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should authenticate and return 200 OK with JWT tokens")
    void loginEndpoint_Success() throws Exception {
        // First register the user
        RegisterRequest registerReq = RegisterRequest.builder()
                .username("login_test_user")
                .password("password123")
                .fullName("Login Test User")
                .role(Role.ROLE_PHARMACIST)
                .branchId(1L)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Then attempt login with valid credentials
        LoginRequest loginReq = LoginRequest.builder()
                .username("login_test_user")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value("login_test_user"))
                .andExpect(jsonPath("$.role").value("ROLE_PHARMACIST"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should return 400 Bad Request when validation fails (Blank password)")
    void registerEndpoint_ValidationFailure() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .username("invalid_user")
                .password("") // Empty password violates @NotBlank
                .fullName("Invalid User")
                .role(Role.ROLE_CASHIER)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}