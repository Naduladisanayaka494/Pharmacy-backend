package com.pharmacy.system.controller;

import com.pharmacy.system.dto.BatchRequestDTO;
import com.pharmacy.system.dto.BatchResponseDTO;
import com.pharmacy.system.dto.StockSummaryDTO;
import com.pharmacy.system.service.MedicineBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
@Tag(name = "Medicine Batch & Inventory", description = "Endpoints for managing batches, stock levels, and FEFO inventory")
public class MedicineBatchController {

    private final MedicineBatchService batchService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN')")
    @Operation(summary = "Add a new batch (STORE_MANAGER, ADMIN only)")
    public ResponseEntity<BatchResponseDTO> addBatch(@Valid @RequestBody BatchRequestDTO requestDTO) {
        BatchResponseDTO createdBatch = batchService.addBatch(requestDTO);
        return new ResponseEntity<>(createdBatch, HttpStatus.CREATED);
    }

    @GetMapping("/drug/{drugCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PHARMACIST', 'CASHIER')")
    @Operation(summary = "Get all batches for a specific drug")
    public ResponseEntity<List<BatchResponseDTO>> getBatchesByDrug(@PathVariable String drugCode) {
        return ResponseEntity.ok(batchService.getBatchesByDrugCode(drugCode));
    }

    @GetMapping("/dispense/{drugCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PHARMACIST', 'CASHIER')")
    @Operation(summary = "Get available non-expired batches sorted by FEFO for POS dispensing")
    public ResponseEntity<List<BatchResponseDTO>> getBatchesForDispensing(@PathVariable String drugCode) {
        return ResponseEntity.ok(batchService.getAvailableBatchesForDispensing(drugCode));
    }

    @GetMapping("/near-expiry")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'PHARMACIST', 'ADMIN')")
    @Operation(summary = "Get batches nearing expiry within threshold days (default 30 days)")
    public ResponseEntity<List<BatchResponseDTO>> getNearExpiryBatches(
            @RequestParam(defaultValue = "30") int daysThreshold) {
        return ResponseEntity.ok(batchService.getNearExpiryBatches(daysThreshold));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN')")
    @Operation(summary = "Get medicines at or below reorder threshold (default 50 units)")
    public ResponseEntity<List<StockSummaryDTO>> getLowStockMedicines(
            @RequestParam(defaultValue = "50") int threshold) {
        return ResponseEntity.ok(batchService.getLowStockOverview(threshold));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER', 'PHARMACIST', 'CASHIER')")
    @Operation(summary = "Get complete stock summary overview for all medicines")
    public ResponseEntity<List<StockSummaryDTO>> getStockSummaryOverview() {
        return ResponseEntity.ok(batchService.getStockSummaryOverview());
    }
}