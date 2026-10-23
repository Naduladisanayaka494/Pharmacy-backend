package com.pharmacy.system.controller;

import com.pharmacy.system.dto.DrugRequestDTO;
import com.pharmacy.system.dto.DrugResponseDTO;
import com.pharmacy.system.entity.ScheduleType;
import com.pharmacy.system.service.ProductCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/drugs")
@RequiredArgsConstructor
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;


    // Register a new medicine into catalog (Admin & Store Manager only)

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    public ResponseEntity<DrugResponseDTO> createDrug(@Valid @RequestBody DrugRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productCatalogService.createDrug(request));
    }


    // Get all active medicines in catalog (All staff members)

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PHARMACIST', 'CASHIER')")
    public ResponseEntity<List<DrugResponseDTO>> getAllActiveDrugs() {
        return ResponseEntity.ok(productCatalogService.getAllActiveDrugs());
    }


    // Get single medicine details by drug code (All staff members)

    @GetMapping("/{drugCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PHARMACIST', 'CASHIER')")
    public ResponseEntity<DrugResponseDTO> getDrugByCode(@PathVariable String drugCode) {
        return ResponseEntity.ok(productCatalogService.getDrugByCode(drugCode));
    }


    // Fast Search: Search by brand name OR generic name (All staff members)

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PHARMACIST', 'CASHIER')")
    public ResponseEntity<List<DrugResponseDTO>> searchDrugs(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(productCatalogService.searchDrugs(keyword));
    }


    // Filter medicines by Schedule Type

    @GetMapping("/schedule/{scheduleType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PHARMACIST', 'CASHIER')")
    public ResponseEntity<List<DrugResponseDTO>> getDrugsBySchedule(@PathVariable ScheduleType scheduleType) {
        return ResponseEntity.ok(productCatalogService.getDrugsBySchedule(scheduleType));
    }


    // Filter medicines by Category

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PHARMACIST', 'CASHIER')")
    public ResponseEntity<List<DrugResponseDTO>> getDrugsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productCatalogService.getDrugsByCategory(category));
    }


    // Update an existing medicine (Admin & Store Manager only)

    @PutMapping("/{drugCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    public ResponseEntity<DrugResponseDTO> updateDrug(
            @PathVariable String drugCode,
            @Valid @RequestBody DrugRequestDTO request
    ) {
        return ResponseEntity.ok(productCatalogService.updateDrug(drugCode, request));
    }


    // Delete: Deactivate a medicine (Admin only)

    @DeleteMapping("/{drugCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deactivateDrug(@PathVariable String drugCode) {
        productCatalogService.deactivateDrug(drugCode);
        return ResponseEntity.ok(Map.of("message", "Medicine deactivated successfully: " + drugCode));
    }
}