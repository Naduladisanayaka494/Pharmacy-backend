package com.pharmacy.system.controller;

import com.pharmacy.system.dto.ReceiptDTO;
import com.pharmacy.system.dto.SaleRequestDTO;
import com.pharmacy.system.dto.SaleResponseDTO;
import com.pharmacy.system.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@Tag(name = "POS Sales & Billing", description = "Endpoints for processing POS checkout transactions, invoice lookup, and thermal receipts")
public class SaleController {

    private final SaleService saleService;

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('CASHIER', 'ADMIN')")
    @Operation(summary = "Process a new POS sale checkout (CASHIER, ADMIN only)")
    public ResponseEntity<SaleResponseDTO> processCheckout(
            @Valid @RequestBody SaleRequestDTO requestDTO,
            Authentication authentication) {

        String cashierUsername = authentication.getName();
        SaleResponseDTO response = saleService.processCheckout(requestDTO, cashierUsername);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{invoiceNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'PHARMACIST', 'STORE_MANAGER')")
    @Operation(summary = "Get completed sale details by invoice number")
    public ResponseEntity<SaleResponseDTO> getSaleByInvoiceNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(saleService.getSaleByInvoiceNumber(invoiceNumber));
    }

    @GetMapping("/{invoiceNumber}/receipt")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'PHARMACIST', 'STORE_MANAGER')")
    @Operation(summary = "Get printable thermal receipt for a specific invoice")
    public ResponseEntity<ReceiptDTO> getReceiptByInvoiceNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(saleService.getReceiptByInvoiceNumber(invoiceNumber));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
    @Operation(summary = "Get all sales transactions within a date-time range (ADMIN, STORE_MANAGER only)")
    public ResponseEntity<List<SaleResponseDTO>> getSalesBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        return ResponseEntity.ok(saleService.getSalesBetween(startDate, endDate));
    }
}