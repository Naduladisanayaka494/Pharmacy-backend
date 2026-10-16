package com.pharmacy.system.repository;

import com.pharmacy.system.entity.Role;
import com.pharmacy.system.entity.User;
import com.pharmacy.system.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("UserRepository JPA Slice Tests")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .username("test_pharmacist")
                .passwordHash("hashed_password_123")
                .fullName("Test Pharmacist")
                .role(Role.ROLE_PHARMACIST)
                .status(UserStatus.ACTIVE)
                .build();

        // Use TestEntityManager to persist directly to in-memory DB slice
        entityManager.persistAndFlush(sampleUser);
    }

    @Test
    @DisplayName("Should find user by username when user exists in database")
    void findByUsername_ReturnsUser() {
        Optional<User> foundUser = userRepository.findByUsername("test_pharmacist");

        assertTrue(foundUser.isPresent());
        assertEquals("test_pharmacist", foundUser.get().getUsername());
        assertEquals(Role.ROLE_PHARMACIST, foundUser.get().getRole());
    }

    @Test
    @DisplayName("Should return empty Optional when user does not exist in database")
    void findByUsername_ReturnsEmpty_WhenUserNotFound() {
        Optional<User> foundUser = userRepository.findByUsername("non_existent_user");

        assertFalse(foundUser.isPresent());
    }

    @Test
    @DisplayName("Should return true for existsByUsername when username is already in database")
    void existsByUsername_ReturnsTrue_WhenUserExists() {
        boolean exists = userRepository.existsByUsername("test_pharmacist");

        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false for existsByUsername when username does not exist")
    void existsByUsername_ReturnsFalse_WhenUserDoesNotExist() {
        boolean exists = userRepository.existsByUsername("unknown_user");

        assertFalse(exists);
    }
}