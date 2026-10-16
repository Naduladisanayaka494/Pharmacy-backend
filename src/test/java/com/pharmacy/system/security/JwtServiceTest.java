package com.pharmacy.system.security;

import com.pharmacy.system.entity.Role;
import com.pharmacy.system.entity.User;
import com.pharmacy.system.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;


    private final String testSecretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long testExpiration = 3600000;
    private final long testRefreshExpiration = 86400000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", testExpiration);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", testRefreshExpiration);

        // Dummy user setup
        testUser = User.builder()
                .id(1L)
                .username("test_pharmacist")
                .passwordHash("hashed_password")
                .fullName("Test Pharmacist")
                .role(Role.ROLE_PHARMACIST)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should generate a valid, non-empty JWT Access Token")
    void shouldGenerateValidAccessToken() {
        String token = jwtService.generateToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        // JWT standard has 3 parts separated by dots (header.payload.signature)
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("Should generate a valid Refresh Token")
    void shouldGenerateValidRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(testUser);

        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertEquals(3, refreshToken.split("\\.").length);
    }

    @Test
    @DisplayName("Should extract username accurately from token")
    void shouldExtractUsernameCorrectly() {
        String token = jwtService.generateToken(testUser);

        String extractedUsername = jwtService.extractUsername(token);

        assertEquals("test_pharmacist", extractedUsername);
    }

    @Test
    @DisplayName("Should validate token successfully for matching user")
    void shouldValidateTokenSuccessfully() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should fail validation when token is evaluated against another user")
    void shouldFailValidationForDifferentUser() {
        String token = jwtService.generateToken(testUser);

        User differentUser = User.builder()
                .username("another_user")
                .role(Role.ROLE_CASHIER)
                .build();

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertFalse(isValid);
    }
}