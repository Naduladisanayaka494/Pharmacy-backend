package com.pharmacy.system.service;

import com.pharmacy.system.dto.AuthResponse;
import com.pharmacy.system.dto.LoginRequest;
import com.pharmacy.system.dto.RegisterRequest;
import com.pharmacy.system.entity.Role;
import com.pharmacy.system.entity.User;
import com.pharmacy.system.entity.UserStatus;
import com.pharmacy.system.repository.UserRepository;
import com.pharmacy.system.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests with Mockito")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("kamal_cashier")
                .passwordHash("hashed_secret_password")
                .fullName("Kamal Perera")
                .role(Role.ROLE_CASHIER)
                .branchId(1L)
                .status(UserStatus.ACTIVE)
                .build();

        registerRequest = RegisterRequest.builder()
                .username("kamal_cashier")
                .password("raw_password_123")
                .fullName("Kamal Perera")
                .role(Role.ROLE_CASHIER)
                .branchId(1L)
                .build();

        loginRequest = LoginRequest.builder()
                .username("kamal_cashier")
                .password("raw_password_123")
                .build();
    }

    @Test
    @DisplayName("Should register a new user successfully and return tokens")
    void register_Success() {

        when(userRepository.existsByUsername("kamal_cashier")).thenReturn(false);
        when(passwordEncoder.encode("raw_password_123")).thenReturn("hashed_secret_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mocked.access.token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("mocked.refresh.token");
        when(jwtService.getJwtExpiration()).thenReturn(900000L);


        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mocked.access.token", response.getAccessToken());
        assertEquals("kamal_cashier", response.getUsername());
        assertEquals(Role.ROLE_CASHIER, response.getRole());

        // Verify that password was encoded and user was saved once
        verify(passwordEncoder, times(1)).encode("raw_password_123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when registering an existing username")
    void register_ThrowsException_WhenUsernameAlreadyExists() {

        when(userRepository.existsByUsername("kamal_cashier")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest)
        );

        assertTrue(exception.getMessage().contains("already registered"));
        // Verify that save and password encoding are NEVER called
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should authenticate and return JWT tokens on valid login credentials")
    void login_Success() {

        when(userRepository.findByUsername("kamal_cashier")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(sampleUser)).thenReturn("mocked.access.token");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("mocked.refresh.token");
        when(jwtService.getJwtExpiration()).thenReturn(900000L);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mocked.access.token", response.getAccessToken());
        assertEquals("mocked.refresh.token", response.getRefreshToken());
        assertEquals("kamal_cashier", response.getUsername());

        // Verify authentication manager was triggered with credentials
        verify(authenticationManager, times(1)).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when login authentication fails")
    void login_ThrowsBadCredentialsException_WhenCredentialsInvalid() {

        doThrow(new BadCredentialsException("Invalid credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        // Token should NEVER be generated if authentication fails
        verify(jwtService, never()).generateToken(any());
    }
}