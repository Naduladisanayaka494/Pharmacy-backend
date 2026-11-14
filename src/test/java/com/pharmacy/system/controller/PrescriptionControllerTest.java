package com.pharmacy.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.system.dto.PrescriptionItemDTO;
import com.pharmacy.system.dto.PrescriptionResponseDTO;
import com.pharmacy.system.dto.PrescriptionUploadRequestDTO;
import com.pharmacy.system.dto.PrescriptionVerificationDTO;
import com.pharmacy.system.entity.PrescriptionStatus;
import com.pharmacy.system.service.PrescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PrescriptionService prescriptionService;

    private PrescriptionResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = PrescriptionResponseDTO.builder()
                .id(1L)
                .prescriptionNumber("RX-2026-TEST01")
                .patientName("John Doe")
                .patientAge(45)
                .patientContact("0771234567")
                .doctorName("Dr. Nimal Perera")
                .doctorSlmcRegNo("SLMC-12345")
                .status(PrescriptionStatus.PENDING)
                .items(List.of())
                .build();
    }

    @Test
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    @DisplayName("POST /api/v1/prescriptions/upload - CASHIER can upload a prescription document (201 Created)")
    void uploadPrescription_AsCashier_Returns201() throws Exception {
        PrescriptionUploadRequestDTO uploadDTO = PrescriptionUploadRequestDTO.builder()
                .patientName("John Doe")
                .patientAge(45)
                .patientContact("0771234567")
                .doctorName("Dr. Nimal Perera")
                .doctorSlmcRegNo("SLMC-12345")
                .build();

        MockMultipartFile filePart = new MockMultipartFile(
                "file", "prescription.jpg", "image/jpeg", "dummy image content".getBytes());

        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "", "application/json", objectMapper.writeValueAsBytes(uploadDTO));

        when(prescriptionService.uploadPrescription(any(PrescriptionUploadRequestDTO.class), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(multipart("/api/v1/prescriptions/upload")
                        .file(filePart)
                        .file(dataPart)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prescriptionNumber").value("RX-2026-TEST01"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "pharmacist_jane", roles = {"PHARMACIST"})
    @DisplayName("GET /api/v1/prescriptions/pending - PHARMACIST can view pending verification queue (200 OK)")
    void getPendingPrescriptions_AsPharmacist_Returns200() throws Exception {
        when(prescriptionService.getPendingPrescriptions()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/prescriptions/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prescriptionNumber").value("RX-2026-TEST01"));
    }

    @Test
    @WithMockUser(username = "pharmacist_jane", roles = {"PHARMACIST"})
    @DisplayName("PUT /api/v1/prescriptions/{id}/verify - PHARMACIST can approve a prescription (200 OK)")
    void verifyPrescription_AsPharmacist_Returns200() throws Exception {
        PrescriptionVerificationDTO verificationDTO = PrescriptionVerificationDTO.builder()
                .status(PrescriptionStatus.APPROVED)
                .pharmacistNotes("Prescription verified")
                .verifiedItems(List.of(
                        PrescriptionItemDTO.builder()
                                .drugCode("DRG-AMOX-500")
                                .dosage("500mg, TDS")
                                .durationDays(5)
                                .prescribedQty(15)
                                .build()
                ))
                .build();

        sampleResponse.setStatus(PrescriptionStatus.APPROVED);
        sampleResponse.setVerifiedByPharmacistUsername("pharmacist_jane");

        when(prescriptionService.verifyPrescription(eq(1L), any(PrescriptionVerificationDTO.class), eq("pharmacist_jane")))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/prescriptions/1/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verificationDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.verifiedByPharmacistUsername").value("pharmacist_jane"));
    }

    @Test
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    @DisplayName("PUT /api/v1/prescriptions/{id}/verify - CASHIER cannot verify prescription (403 Forbidden)")
    void verifyPrescription_AsCashier_Returns403() throws Exception {
        PrescriptionVerificationDTO verificationDTO = PrescriptionVerificationDTO.builder()
                .status(PrescriptionStatus.APPROVED)
                .build();

        mockMvc.perform(put("/api/v1/prescriptions/1/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verificationDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "store_manager_user", roles = {"STORE_MANAGER"})
    @DisplayName("PUT /api/v1/prescriptions/{id}/verify - STORE_MANAGER cannot verify prescription (403 Forbidden)")
    void verifyPrescription_AsStoreManager_Returns403() throws Exception {
        PrescriptionVerificationDTO verificationDTO = PrescriptionVerificationDTO.builder()
                .status(PrescriptionStatus.APPROVED)
                .build();

        mockMvc.perform(put("/api/v1/prescriptions/1/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verificationDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    @DisplayName("GET /api/v1/prescriptions/dispense/{rxNumber} - CASHIER can retrieve approved prescription for POS (200 OK)")
    void getPrescriptionForDispensing_AsCashier_Returns200() throws Exception {
        sampleResponse.setStatus(PrescriptionStatus.APPROVED);

        when(prescriptionService.getPrescriptionForDispensing("RX-2026-TEST01")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/prescriptions/dispense/RX-2026-TEST01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionNumber").value("RX-2026-TEST01"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}