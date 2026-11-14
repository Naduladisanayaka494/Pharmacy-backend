package com.pharmacy.system.service;

import com.pharmacy.system.dto.PrescriptionItemDTO;
import com.pharmacy.system.dto.PrescriptionResponseDTO;
import com.pharmacy.system.dto.PrescriptionUploadRequestDTO;
import com.pharmacy.system.dto.PrescriptionVerificationDTO;
import com.pharmacy.system.entity.Prescription;
import com.pharmacy.system.entity.PrescriptionItem;
import com.pharmacy.system.entity.PrescriptionStatus;
import com.pharmacy.system.entity.ProductCatalog;
import com.pharmacy.system.entity.User;
import com.pharmacy.system.repository.PrescriptionRepository;
import com.pharmacy.system.repository.ProductCatalogRepository;
import com.pharmacy.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final ProductCatalogRepository productCatalogRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    /**
     * Upload a new prescription with patient/doctor details and file attachment.
     * Sets initial status to PENDING for pharmacist review.
     */
    @Transactional
    public PrescriptionResponseDTO uploadPrescription(PrescriptionUploadRequestDTO requestDTO, MultipartFile file) {
        log.info("Uploading prescription for patient: {}, Doctor SLMC: {}", requestDTO.getPatientName(), requestDTO.getDoctorSlmcRegNo());

        // 1. Store the uploaded file securely
        String storedFileName = fileStorageService.storeFile(file);

        // 2. Generate unique prescription number
        String rxNumber = generatePrescriptionNumber();

        // 3. Build Prescription entity
        Prescription prescription = Prescription.builder()
                .prescriptionNumber(rxNumber)
                .patientName(requestDTO.getPatientName().trim())
                .patientAge(requestDTO.getPatientAge())
                .patientContact(requestDTO.getPatientContact().trim())
                .doctorName(requestDTO.getDoctorName().trim())
                .doctorSlmcRegNo(requestDTO.getDoctorSlmcRegNo().trim())
                .filePath(storedFileName)
                .status(PrescriptionStatus.PENDING)
                .build();

        // 4. Map initial items if provided
        if (requestDTO.getItems() != null && !requestDTO.getItems().isEmpty()) {
            for (PrescriptionItemDTO itemDTO : requestDTO.getItems()) {
                ProductCatalog drug = productCatalogRepository.findById(itemDTO.getDrugCode())
                        .orElseThrow(() -> new IllegalArgumentException("Drug not found with code: " + itemDTO.getDrugCode()));

                PrescriptionItem item = PrescriptionItem.builder()
                        .productCatalog(drug)
                        .dosage(itemDTO.getDosage())
                        .durationDays(itemDTO.getDurationDays())
                        .prescribedQty(itemDTO.getPrescribedQty())
                        .build();

                prescription.addItem(item);
            }
        }

        Prescription savedPrescription = prescriptionRepository.save(prescription);
        log.info("Prescription created successfully with number: {}", savedPrescription.getPrescriptionNumber());

        return mapToDTO(savedPrescription);
    }

    /**
     * Clinical Pharmacist Review: Approve or Reject a prescription.
     * Only licensed pharmacists are authorized to call this.
     */
    @Transactional
    public PrescriptionResponseDTO verifyPrescription(Long prescriptionId, PrescriptionVerificationDTO verificationDTO, String pharmacistUsername) {
        log.info("Pharmacist '{}' verifying prescription ID: {} with decision: {}", pharmacistUsername, prescriptionId, verificationDTO.getStatus());

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with ID: " + prescriptionId));

        // State Machine validation: Can only verify PENDING prescriptions
        if (prescription.getStatus() != PrescriptionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING prescriptions can be verified. Current status: " + prescription.getStatus());
        }

        User pharmacist = userRepository.findByUsername(pharmacistUsername)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacist user not found: " + pharmacistUsername));

        prescription.setStatus(verificationDTO.getStatus());
        prescription.setPharmacistNotes(verificationDTO.getPharmacistNotes());
        prescription.setVerifiedBy(pharmacist);
        prescription.setVerifiedAt(LocalDateTime.now());

        // If Approved, update/confirm the prescribed medicine lines
        if (verificationDTO.getStatus() == PrescriptionStatus.APPROVED) {
            if (verificationDTO.getVerifiedItems() == null || verificationDTO.getVerifiedItems().isEmpty()) {
                throw new IllegalArgumentException("At least one medicine item must be verified and approved for dispensing");
            }

            prescription.getItems().clear();
            for (PrescriptionItemDTO itemDTO : verificationDTO.getVerifiedItems()) {
                ProductCatalog drug = productCatalogRepository.findById(itemDTO.getDrugCode())
                        .orElseThrow(() -> new IllegalArgumentException("Drug not found with code: " + itemDTO.getDrugCode()));

                if (!Boolean.TRUE.equals(drug.getIsActive())) {
                    throw new IllegalStateException("Cannot prescribe an inactive drug: " + drug.getDrugCode());
                }

                PrescriptionItem item = PrescriptionItem.builder()
                        .productCatalog(drug)
                        .dosage(itemDTO.getDosage())
                        .durationDays(itemDTO.getDurationDays())
                        .prescribedQty(itemDTO.getPrescribedQty())
                        .build();

                prescription.addItem(item);
            }
        } else if (verificationDTO.getStatus() == PrescriptionStatus.REJECTED) {
            if (verificationDTO.getPharmacistNotes() == null || verificationDTO.getPharmacistNotes().trim().isEmpty()) {
                throw new IllegalArgumentException("Pharmacist notes/rejection reason is mandatory when rejecting a prescription");
            }
        }

        Prescription updatedPrescription = prescriptionRepository.save(prescription);
        log.info("Prescription '{}' updated to status: {}", updatedPrescription.getPrescriptionNumber(), updatedPrescription.getStatus());

        return mapToDTO(updatedPrescription);
    }

    /**
     * Retrieve all pending prescriptions awaiting pharmacist verification.
     * Ordered by FIFO (first submitted, first reviewed).
     */
    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getPendingPrescriptions() {
        return prescriptionRepository.findByStatusOrderByCreatedAtAsc(PrescriptionStatus.PENDING)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve prescription by unique prescription number
     */
    @Transactional(readOnly = true)
    public PrescriptionResponseDTO getPrescriptionByNumber(String prescriptionNumber) {
        Prescription prescription = prescriptionRepository.findByPrescriptionNumberWithItems(prescriptionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with number: " + prescriptionNumber));
        return mapToDTO(prescription);
    }

    /**
     * POS Dispensing Pre-validation:
     * Check if a prescription is legally valid and approved for dispensing at the POS counter.
     */
    @Transactional(readOnly = true)
    public PrescriptionResponseDTO getPrescriptionForDispensing(String prescriptionNumber) {
        Prescription prescription = prescriptionRepository.findByPrescriptionNumberWithItems(prescriptionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with number: " + prescriptionNumber));

        if (prescription.getStatus() == PrescriptionStatus.PENDING) {
            throw new IllegalStateException("Prescription " + prescriptionNumber + " is still PENDING verification by a pharmacist");
        }

        if (prescription.getStatus() == PrescriptionStatus.REJECTED) {
            throw new IllegalStateException("Prescription " + prescriptionNumber + " was REJECTED by pharmacist: " + prescription.getPharmacistNotes());
        }

        if (prescription.getStatus() == PrescriptionStatus.DISPENSED) {
            throw new IllegalStateException("Prescription " + prescriptionNumber + " has ALREADY been dispensed and cannot be reused");
        }

        return mapToDTO(prescription);
    }

    /**
     * Mark prescription as DISPENSED after successful POS checkout.
     */
    @Transactional
    public void markAsDispensed(String prescriptionNumber) {
        Prescription prescription = prescriptionRepository.findByPrescriptionNumber(prescriptionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found: " + prescriptionNumber));
        prescription.setStatus(PrescriptionStatus.DISPENSED);
        prescriptionRepository.save(prescription);
        log.info("Prescription '{}' marked as DISPENSED", prescriptionNumber);
    }

    /**
     * Download or view the prescription file attachment.
     */
    public Resource getPrescriptionDocument(String filename) {
        return fileStorageService.loadFileAsResource(filename);
    }

    private String generatePrescriptionNumber() {
        String year = String.valueOf(Year.now().getValue());
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "RX-" + year + "-" + randomSuffix;
    }

    private PrescriptionResponseDTO mapToDTO(Prescription prescription) {
        List<PrescriptionItemDTO> itemDTOs = prescription.getItems().stream()
                .map(item -> PrescriptionItemDTO.builder()
                        .drugCode(item.getProductCatalog().getDrugCode())
                        .brandName(item.getProductCatalog().getBrandName())
                        .genericName(item.getProductCatalog().getGenericName())
                        .dosage(item.getDosage())
                        .durationDays(item.getDurationDays())
                        .prescribedQty(item.getPrescribedQty())
                        .build())
                .collect(Collectors.toList());

        return PrescriptionResponseDTO.builder()
                .id(prescription.getId())
                .prescriptionNumber(prescription.getPrescriptionNumber())
                .patientName(prescription.getPatientName())
                .patientAge(prescription.getPatientAge())
                .patientContact(prescription.getPatientContact())
                .doctorName(prescription.getDoctorName())
                .doctorSlmcRegNo(prescription.getDoctorSlmcRegNo())
                .fileDownloadUrl("/api/v1/prescriptions/documents/" + prescription.getFilePath())
                .status(prescription.getStatus())
                .pharmacistNotes(prescription.getPharmacistNotes())
                .verifiedByPharmacistUsername(prescription.getVerifiedBy() != null ? prescription.getVerifiedBy().getUsername() : null)
                .verifiedAt(prescription.getVerifiedAt())
                .items(itemDTOs)
                .createdAt(prescription.getCreatedAt())
                .build();
    }
}