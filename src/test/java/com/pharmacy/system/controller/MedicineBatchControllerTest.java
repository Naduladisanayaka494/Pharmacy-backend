package com.pharmacy.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.system.dto.BatchRequestDTO;
import com.pharmacy.system.dto.BatchResponseDTO;
import com.pharmacy.system.dto.StockSummaryDTO;
import com.pharmacy.system.service.MedicineBatchService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MedicineBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MedicineBatchService batchService;

    private BatchRequestDTO validRequest;
    private BatchResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = BatchRequestDTO.builder()
                .drugCode("DRG-AMOX-500")
                .batchNumber("BAT-2026-001")
                .mfgDate(LocalDate.now().minusMonths(1))
                .expDate(LocalDate.now().plusYears(2))
                .unitCost(new BigDecimal("15.00"))
                .retailPrice(new BigDecimal("22.50"))
                .stockQty(100)
                .build();

        sampleResponse = BatchResponseDTO.builder()
                .id(1L)
                .drugCode("DRG-AMOX-500")
                .brandName("Amoxil 500mg")
                .genericName("Amoxicillin")
                .batchNumber("BAT-2026-001")
                .mfgDate(validRequest.getMfgDate())
                .expDate(validRequest.getExpDate())
                .unitCost(validRequest.getUnitCost())
                .retailPrice(validRequest.getRetailPrice())
                .stockQty(100)
                .isExpired(false)
                .build();
    }

    @Test
    @WithMockUser(username = "manager", roles = {"STORE_MANAGER"})
    @DisplayName("POST /api/v1/batches - STORE_MANAGER can create a new batch (201 Created)")
    void addBatch_AsStoreManager_Returns201() throws Exception {
        when(batchService.addBatch(any(BatchRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/batches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchNumber").value("BAT-2026-001"))
                .andExpect(jsonPath("$.drugCode").value("DRG-AMOX-500"));
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    @DisplayName("POST /api/v1/batches - CASHIER cannot create a new batch (403 Forbidden)")
    void addBatch_AsCashier_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/batches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    @DisplayName("GET /api/v1/batches/dispense/{drugCode} - CASHIER can retrieve FEFO batches for POS (200 OK)")
    void getBatchesForDispensing_AsCashier_Returns200() throws Exception {
        when(batchService.getAvailableBatchesForDispensing("DRG-AMOX-500")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/batches/dispense/DRG-AMOX-500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchNumber").value("BAT-2026-001"));
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("GET /api/v1/batches/near-expiry - PHARMACIST can view near-expiry batches (200 OK)")
    void getNearExpiryBatches_AsPharmacist_Returns200() throws Exception {
        when(batchService.getNearExpiryBatches(anyInt())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/batches/near-expiry").param("daysThreshold", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchNumber").value("BAT-2026-001"));
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    @DisplayName("GET /api/v1/batches/low-stock - CASHIER is forbidden from viewing low stock (403 Forbidden)")
    void getLowStock_AsCashier_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/batches/low-stock"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager", roles = {"STORE_MANAGER"})
    @DisplayName("GET /api/v1/batches/low-stock - STORE_MANAGER can view low-stock medicines (200 OK)")
    void getLowStock_AsStoreManager_Returns200() throws Exception {
        StockSummaryDTO summary = StockSummaryDTO.builder()
                .drugCode("DRG-AMOX-500")
                .brandName("Amoxil 500mg")
                .totalStock(15L)
                .build();

        when(batchService.getLowStockOverview(anyInt())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/batches/low-stock").param("threshold", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].drugCode").value("DRG-AMOX-500"));
    }
}