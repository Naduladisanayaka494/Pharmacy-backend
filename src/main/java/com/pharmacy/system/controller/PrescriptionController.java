package com.pharmacy.system.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.system.dto.PrescriptionResponseDTO;
import com.pharmacy.system.dto.PrescriptionUploadRequestDTO;
import com.pharmacy.system.dto.PrescriptionVerificationDTO;
import com.pharmacy.system.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescription Verification", description = "Endpoints for uploading, verifying, and dispensing medical prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER', 'STORE_MANAGER')")
    @Operation(summary = "Upload a new prescription document with patient/doctor metadata")
    public ResponseEntity<PrescriptionResponseDTO> uploadPrescription(
            @RequestPart("data") String dataJson,
            @RequestPart("file") MultipartFile file) throws JsonProcessingException {

        PrescriptionUploadRequestDTO requestDTO = objectMapper.readValue(dataJson, PrescriptionUploadRequestDTO.class);
        PrescriptionResponseDTO response = prescriptionService.uploadPrescription(requestDTO, file);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    @Operation(summary = "Get all pending prescriptions for pharmacist review (FIFO order)")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPendingPrescriptions() {
        return ResponseEntity.ok(prescriptionService.getPendingPrescriptions());
    }

    @PutMapping("/{id}/verify")
    @PreAuthorize("hasRole('PHARMACIST')")
    @Operation(summary = "Approve or Reject a prescription (PHARMACIST only)")
    public ResponseEntity<PrescriptionResponseDTO> verifyPrescription(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionVerificationDTO verificationDTO,
            Authentication authentication) {

        String pharmacistUsername = authentication.getName();
        PrescriptionResponseDTO response = prescriptionService.verifyPrescription(id, verificationDTO, pharmacistUsername);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{prescriptionNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER', 'STORE_MANAGER')")
    @Operation(summary = "Get prescription details by prescription number")
    public ResponseEntity<PrescriptionResponseDTO> getPrescriptionByNumber(@PathVariable String prescriptionNumber) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionByNumber(prescriptionNumber));
    }

    @GetMapping("/dispense/{prescriptionNumber}")
    @PreAuthorize("hasAnyRole('CASHIER', 'PHARMACIST', 'ADMIN')")
    @Operation(summary = "Validate prescription for POS billing (Must be APPROVED and not DISPENSED)")
    public ResponseEntity<PrescriptionResponseDTO> getPrescriptionForDispensing(@PathVariable String prescriptionNumber) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionForDispensing(prescriptionNumber));
    }

    @GetMapping("/documents/{filename:.+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'CASHIER', 'STORE_MANAGER')")
    @Operation(summary = "Download or view prescription document")
    public ResponseEntity<Resource> getDocument(@PathVariable String filename) {
        Resource fileResource = prescriptionService.getPrescriptionDocument(filename);

        String contentType = "application/octet-stream";
        if (filename.toLowerCase().endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (filename.toLowerCase().endsWith(".png")) {
            contentType = "image/png";
        } else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
            contentType = "image/jpeg";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(fileResource);
    }
}