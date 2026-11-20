package com.pharmacy.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.system.dto.ReceiptDTO;
import com.pharmacy.system.dto.SaleItemRequestDTO;
import com.pharmacy.system.dto.SaleRequestDTO;
import com.pharmacy.system.dto.SaleResponseDTO;
import com.pharmacy.system.entity.PaymentMethod;
import com.pharmacy.system.entity.SaleStatus;
import com.pharmacy.system.service.SaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SaleService saleService;

    private SaleRequestDTO validSaleRequest;
    private SaleResponseDTO sampleResponse;
    private ReceiptDTO sampleReceipt;

    @BeforeEach
    void setUp() {
        validSaleRequest = SaleRequestDTO.builder()
                .items(List.of(
                        SaleItemRequestDTO.builder()
                                .drugCode("DRG-PARA-500")
                                .quantity(10)
                                .discount(BigDecimal.ZERO)
                                .build()
                ))
                .paymentMethod(PaymentMethod.CASH)
                .paidAmount(new BigDecimal("100.00"))
                .customerName("Amal Perera")
                .build();

        sampleResponse = SaleResponseDTO.builder()
                .id(1L)
                .invoiceNumber("INV-2026-TEST01")
                .cashierUsername("cashier_user")
                .subtotal(new BigDecimal("100.00"))
                .netTotal(new BigDecimal("100.00"))
                .paidAmount(new BigDecimal("100.00"))
                .balanceAmount(BigDecimal.ZERO)
                .paymentMethod(PaymentMethod.CASH)
                .status(SaleStatus.COMPLETED)
                .items(List.of())
                .build();

        sampleReceipt = ReceiptDTO.builder()
                .invoiceNumber("INV-2026-TEST01")
                .cashierName("Kamal Silva")
                .netTotal(new BigDecimal("100.00"))
                .paidAmount(new BigDecimal("100.00"))
                .balance(BigDecimal.ZERO)
                .paymentMethod(PaymentMethod.CASH)
                .items(List.of())
                .build();
    }

    @Test
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    @DisplayName("POST /api/v1/sales/checkout - CASHIER should successfully process sale checkout (201 Created)")
    void processCheckout_AsCashier_Returns201() throws Exception {
        when(saleService.processCheckout(any(SaleRequestDTO.class), eq("cashier_user")))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/sales/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSaleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-TEST01"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "store_manager_user", roles = {"STORE_MANAGER"})
    @DisplayName("POST /api/v1/sales/checkout - STORE_MANAGER should be forbidden from billing at POS (403 Forbidden)")
    void processCheckout_AsStoreManager_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/sales/checkout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSaleRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    @DisplayName("GET /api/v1/sales/{invoiceNumber} - Any authenticated staff can view invoice details (200 OK)")
    void getSaleByInvoiceNumber_Returns200() throws Exception {
        when(saleService.getSaleByInvoiceNumber("INV-2026-TEST01")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/sales/INV-2026-TEST01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-TEST01"));
    }

    @Test
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    @DisplayName("GET /api/v1/sales/{invoiceNumber}/receipt - Should return printable thermal receipt (200 OK)")
    void getReceiptByInvoiceNumber_Returns200() throws Exception {
        when(saleService.getReceiptByInvoiceNumber("INV-2026-TEST01")).thenReturn(sampleReceipt);

        mockMvc.perform(get("/api/v1/sales/INV-2026-TEST01/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-TEST01"))
                .andExpect(jsonPath("$.pharmacyName").value("MediCare Enterprise Pharmacy"));
    }

    @Test
    @WithMockUser(username = "manager_user", roles = {"STORE_MANAGER"})
    @DisplayName("GET /api/v1/sales - STORE_MANAGER can view sales history report (200 OK)")
    void getSalesBetween_AsStoreManager_Returns200() throws Exception {
        when(saleService.getSalesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/sales")
                        .param("startDate", "2026-11-01T00:00:00")
                        .param("endDate", "2026-11-20T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-2026-TEST01"));
    }

    @Test
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    @DisplayName("GET /api/v1/sales - CASHIER is forbidden from viewing sales history report (403 Forbidden)")
    void getSalesBetween_AsCashier_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/sales")
                        .param("startDate", "2026-11-01T00:00:00")
                        .param("endDate", "2026-11-20T23:59:59"))
                .andExpect(status().isForbidden());
    }
}