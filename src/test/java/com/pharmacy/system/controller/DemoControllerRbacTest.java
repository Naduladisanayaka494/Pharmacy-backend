package com.pharmacy.system.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Role-Based Access Control (RBAC) Integration Tests")
class DemoControllerRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Cashier should access cashier dashboard successfully (200 OK)")
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    void cashier_CanAccess_CashierEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/demo/cashier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access Granted: Cashier Dashboard & POS Billing"));
    }

    @Test
    @DisplayName("Cashier should be blocked from pharmacist endpoint (403 Forbidden)")
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    void cashier_CannotAccess_PharmacistEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/demo/pharmacist"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin should access cashier dashboard successfully (200 OK)")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void admin_CanAccess_CashierEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/demo/cashier"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Pharmacist should access pharmacist endpoint successfully (200 OK)")
    @WithMockUser(username = "pharmacist_user", roles = {"PHARMACIST"})
    void pharmacist_CanAccess_PharmacistEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/demo/pharmacist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Access Granted: Prescription Validation & Medicine Stock"));
    }

    @Test
    @DisplayName("Store Manager should be blocked from admin endpoint (403 Forbidden)")
    @WithMockUser(username = "manager_user", roles = {"STORE_MANAGER"})
    void storeManager_CannotAccess_AdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/demo/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request should be rejected (401/403)")
    void unauthenticatedUser_IsBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/demo/cashier"))
                .andExpect(status().isForbidden());
    }
}