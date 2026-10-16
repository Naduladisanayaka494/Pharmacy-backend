package com.pharmacy.system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    // Cashier Access Endpoint
    @GetMapping("/cashier")
    @PreAuthorize("hasRole('CASHIER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> cashierEndpoint() {
        return ResponseEntity.ok(Map.of("message", "Access Granted: Cashier Dashboard & POS Billing"));
    }

    // Pharmacist Access Endpoint
    @GetMapping("/pharmacist")
    @PreAuthorize("hasRole('PHARMACIST') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> pharmacistEndpoint() {
        return ResponseEntity.ok(Map.of("message", "Access Granted: Prescription Validation & Medicine Stock"));
    }

    // Store Manager
    @GetMapping("/inventory")
    @PreAuthorize("hasRole('STORE_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> inventoryEndpoint() {
        return ResponseEntity.ok(Map.of("message", "Access Granted: Inventory Batches & Stock Management"));
    }

    // Admin Only Access Endpoint
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminEndpoint() {
        return ResponseEntity.ok(Map.of("message", "Access Granted: Super Admin System Configurations & User Management"));
    }
}