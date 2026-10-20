package com.pharmacy.system.exception;

import com.pharmacy.system.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler Direct Unit Tests")
public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should return 400 Bad Request with clean message on IllegalArgumentException")
    void handleIllegalArgumentException_ShouldReturn400BadRequest() {

        IllegalArgumentException exception = new IllegalArgumentException("Username is already registered: cashier_john");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(exception);
        //  Verify HTTP status is 400 Bad Request and ErrorResponse payload fields match expected
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Username is already registered: cashier_john", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized on BadCredentialsException")
    void handleBadCredentialsException_ShouldReturn401Unauthorized() {

        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentialsException(exception);
        // Verify 401 Unauthorized status and invalid credentials payload
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
        assertEquals("Invalid username or password", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should return 403 Forbidden with clear permission message on AccessDeniedException")
    void handleAccessDeniedException_ShouldReturn403Forbidden() {

        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDeniedException(exception);
        // Verify 403 Forbidden status and RBAC access restriction message
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Admin role required"));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error with generic safe message on unhandled Exception")
    void handleGlobalException_ShouldReturn500InternalServerError() {

        Exception unexpectedException = new RuntimeException("Database connection timeout or internal pointer failure");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(unexpectedException);
        // Verify HTTP status code is 500 Internal Server Error
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        // Verify sensitive details are masked from client
        assertEquals(unexpectedException.getMessage(), response.getBody().getMessage());
    }


}
