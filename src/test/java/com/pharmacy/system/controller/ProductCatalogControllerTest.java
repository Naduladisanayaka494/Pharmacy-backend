package com.pharmacy.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.system.dto.DrugRequestDTO;
import com.pharmacy.system.dto.DrugResponseDTO;
import com.pharmacy.system.entity.DosageForm;
import com.pharmacy.system.entity.ScheduleType;
import com.pharmacy.system.service.ProductCatalogService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductCatalogService productCatalogService;

    private DrugRequestDTO sampleRequest;
    private DrugResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        sampleRequest = DrugRequestDTO.builder()
                .drugCode("DRG-PARA-500")
                .brandName("Panadol 500mg")
                .genericName("Paracetamol")
                .category("Analgesic")
                .dosageForm(DosageForm.TABLET)
                .scheduleType(ScheduleType.OVER_THE_COUNTER)
                .build();

        sampleResponse = DrugResponseDTO.builder()
                .drugCode("DRG-PARA-500")
                .brandName("Panadol 500mg")
                .genericName("Paracetamol")
                .category("Analgesic")
                .dosageForm(DosageForm.TABLET)
                .scheduleType(ScheduleType.OVER_THE_COUNTER)
                .isActive(true)
                .build();
    }

    @Test
    @WithMockUser(username = "manager", roles = {"STORE_MANAGER"})
    @DisplayName("POST /api/v1/drugs - STORE_MANAGER should successfully create a drug (201 Created)")
    void createDrug_AsStoreManager_Returns201() throws Exception {
        when(productCatalogService.createDrug(any(DrugRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/drugs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.drugCode").value("DRG-PARA-500"))
                .andExpect(jsonPath("$.brandName").value("Panadol 500mg"));
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    @DisplayName("POST /api/v1/drugs - CASHIER should be forbidden from creating a drug (403 Forbidden)")
    void createDrug_AsCashier_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/drugs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    @DisplayName("GET /api/v1/drugs - CASHIER should be allowed to view active drugs (200 OK)")
    void getAllDrugs_AsCashier_Returns200() throws Exception {
        when(productCatalogService.getAllActiveDrugs()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/drugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].drugCode").value("DRG-PARA-500"));
    }

    @Test
    @WithMockUser(username = "pharmacist", roles = {"PHARMACIST"})
    @DisplayName("GET /api/v1/drugs/search - PHARMACIST should be allowed to search drugs (200 OK)")
    void searchDrugs_AsPharmacist_Returns200() throws Exception {
        when(productCatalogService.searchDrugs("pana")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/drugs/search")
                        .param("keyword", "pana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brandName").value("Panadol 500mg"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/drugs/{code} - ADMIN should successfully update drug details (200 OK)")
    void updateDrug_AsAdmin_Returns200() throws Exception {
        when(productCatalogService.updateDrug(eq("DRG-PARA-500"), any(DrugRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/drugs/DRG-PARA-500")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drugCode").value("DRG-PARA-500"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("DELETE /api/v1/drugs/{code} - ADMIN should be allowed to deactivate drug (200 OK)")
    void deactivateDrug_AsAdmin_Returns200() throws Exception {
        doNothing().when(productCatalogService).deactivateDrug("DRG-PARA-500");

        mockMvc.perform(delete("/api/v1/drugs/DRG-PARA-500")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Medicine deactivated successfully: DRG-PARA-500"));
    }

    @Test
    @WithMockUser(username = "manager", roles = {"STORE_MANAGER"})
    @DisplayName("DELETE /api/v1/drugs/{code} - STORE_MANAGER should be forbidden from deleting drug (403 Forbidden)")
    void deactivateDrug_AsStoreManager_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/drugs/DRG-PARA-500")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}