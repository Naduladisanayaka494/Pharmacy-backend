package com.pharmacy.system.config;

import com.pharmacy.system.entity.Role;
import com.pharmacy.system.entity.User;
import com.pharmacy.system.entity.UserStatus;
import com.pharmacy.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataInitializer Unit Tests with Mockito")
public class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        // set @Value private fields using ReflectionTestUtils
        ReflectionTestUtils.setField(dataInitializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(dataInitializer, "adminPassword", "admin123");
        ReflectionTestUtils.setField(dataInitializer, "adminFullName", "Default System Administrator");
        ReflectionTestUtils.setField(dataInitializer, "defaultBranchId", 1L);
    }


    @Test
    @DisplayName("Should create default super admin when admin does not exist in database")
    void run_ShouldCreateDefaultSuperAdmin_WhenAdminDoesNotExist() {

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("hashed_admin123_password");

        dataInitializer.run();

        // Verify save was called and capture the saved User object
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser);
        assertEquals("admin", savedUser.getUsername());
        assertEquals("hashed_admin123_password", savedUser.getPasswordHash());
        assertEquals(Role.ROLE_ADMIN, savedUser.getRole());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertEquals("Default System Administrator", savedUser.getFullName());
        assertEquals(1L, savedUser.getBranchId());
        verify(passwordEncoder, times(1)).encode("admin123");
    }

    @Test
    @DisplayName("Should skip initialization when super admin already exists in database")
    void run_ShouldSkipInitialization_WhenAdminAlreadyExists() {

        when(userRepository.existsByUsername("admin")).thenReturn(true);

        dataInitializer.run();
        // Verify that save and password encoding are NEVER called
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

}
