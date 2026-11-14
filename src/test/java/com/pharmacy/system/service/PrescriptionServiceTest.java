package com.pharmacy.system.service;

import com.pharmacy.system.dto.PrescriptionItemDTO;
import com.pharmacy.system.dto.PrescriptionResponseDTO;
import com.pharmacy.system.dto.PrescriptionUploadRequestDTO;
import com.pharmacy.system.dto.PrescriptionVerificationDTO;
import com.pharmacy.system.entity.DosageForm;
import com.pharmacy.system.entity.Prescription;
import com.pharmacy.system.entity.PrescriptionItem;
import com.pharmacy.system.entity.PrescriptionStatus;
import com.pharmacy.system.entity.ProductCatalog;
import com.pharmacy.system.entity.Role;
import com.pharmacy.system.entity.ScheduleType;
import com.pharmacy.system.entity.User;
import com.pharmacy.system.repository.PrescriptionRepository;
import com.pharmacy.system.repository.ProductCatalogRepository;
import com.pharmacy.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private ProductCatalogRepository productCatalogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private PrescriptionService prescriptionService;

    private ProductCatalog sampleDrug;
    private User pharmacistUser;
    private Prescription pendingPrescription;
    private PrescriptionUploadRequestDTO uploadRequestDTO;

    @BeforeEach
    void setUp() {
        sampleDrug = ProductCatalog.builder()
                .drugCode("DRG-AMOX-500")
                .brandName("Amoxil 500mg")
                .genericName("Amoxicillin")
                .category("Antibiotics")
                .dosageForm(DosageForm.CAPSULE)
                .scheduleType(ScheduleType.PRESCRIPTION_ONLY)
                .isActive(true)
                .build();

        pharmacistUser = User.builder()
                .id(1L)
                .username("pharmacist_jane")
                .role(Role.ROLE_PHARMACIST)
                .build();

        pendingPrescription = Prescription.builder()
                .id(1L)
                .prescriptionNumber("RX-2026-TEST01")
                .patientName("John Doe")
                .patientAge(45)
                .patientContact("0771234567")
                .doctorName("Dr. Nimal Perera")
                .doctorSlmcRegNo("SLMC-12345")
                .filePath("uuid-file.jpg")
                .status(PrescriptionStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        uploadRequestDTO = PrescriptionUploadRequestDTO.builder()
                .patientName("John Doe")
                .patientAge(45)
                .patientContact("0771234567")
                .doctorName("Dr. Nimal Perera")
                .doctorSlmcRegNo("SLMC-12345")
                .items(List.of())
                .build();
    }

    @Test
    @DisplayName("Should successfully upload a prescription and set status to PENDING")
    void uploadPrescription_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "rx.jpg", "image/jpeg", "image content".getBytes());

        when(fileStorageService.storeFile(file)).thenReturn("safe-uuid.jpg");
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(pendingPrescription);

        PrescriptionResponseDTO response = prescriptionService.uploadPrescription(uploadRequestDTO, file);

        assertThat(response).isNotNull();
        assertThat(response.getPrescriptionNumber()).isEqualTo("RX-2026-TEST01");
        assertThat(response.getStatus()).isEqualTo(PrescriptionStatus.PENDING);
        verify(fileStorageService).storeFile(file);
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    @DisplayName("Pharmacist Approval: Should successfully approve a PENDING prescription with verified items")
    void verifyPrescription_Approve_Success() {
        PrescriptionItemDTO itemDTO = PrescriptionItemDTO.builder()
                .drugCode("DRG-AMOX-500")
                .dosage("500mg, TDS")
                .durationDays(5)
                .prescribedQty(15)
                .build();

        PrescriptionVerificationDTO verificationDTO = PrescriptionVerificationDTO.builder()
                .status(PrescriptionStatus.APPROVED)
                .pharmacistNotes("Dosage verified and appropriate for patient age")
                .verifiedItems(List.of(itemDTO))
                .build();

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(userRepository.findByUsername("pharmacist_jane")).thenReturn(Optional.of(pharmacistUser));
        when(productCatalogRepository.findById("DRG-AMOX-500")).thenReturn(Optional.of(sampleDrug));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrescriptionResponseDTO response = prescriptionService.verifyPrescription(1L, verificationDTO, "pharmacist_jane");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PrescriptionStatus.APPROVED);
        assertThat(response.getVerifiedByPharmacistUsername()).isEqualTo("pharmacist_jane");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getDrugCode()).isEqualTo("DRG-AMOX-500");
    }

    @Test
    @DisplayName("Pharmacist Rejection: Should successfully reject prescription when rejection reason is provided")
    void verifyPrescription_Reject_Success() {
        PrescriptionVerificationDTO verificationDTO = PrescriptionVerificationDTO.builder()
                .status(PrescriptionStatus.REJECTED)
                .pharmacistNotes("Doctor SLMC seal missing on physical paper")
                .build();

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(userRepository.findByUsername("pharmacist_jane")).thenReturn(Optional.of(pharmacistUser));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrescriptionResponseDTO response = prescriptionService.verifyPrescription(1L, verificationDTO, "pharmacist_jane");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PrescriptionStatus.REJECTED);
        assertThat(response.getPharmacistNotes()).contains("seal missing");
    }

    @Test
    @DisplayName("Pharmacist Rejection: Should throw exception if pharmacist notes/reason is missing")
    void verifyPrescription_RejectWithoutNotes_ThrowsException() {
        PrescriptionVerificationDTO verificationDTO = PrescriptionVerificationDTO.builder()
                .status(PrescriptionStatus.REJECTED)
                .pharmacistNotes("   ") // Blank
                .build();

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));
        when(userRepository.findByUsername("pharmacist_jane")).thenReturn(Optional.of(pharmacistUser));

        assertThatThrownBy(() -> prescriptionService.verifyPrescription(1L, verificationDTO, "pharmacist_jane"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mandatory when rejecting");

        verify(prescriptionRepository, never()).save(any(Prescription.class));
    }

    @Test
    @DisplayName("State Machine: Should prevent re-verifying a prescription that is not in PENDING state")
    void verifyPrescription_NonPendingState_ThrowsException() {
        pendingPrescription.setStatus(PrescriptionStatus.APPROVED);

        PrescriptionVerificationDTO verificationDTO = PrescriptionVerificationDTO.builder()
                .status(PrescriptionStatus.APPROVED)
                .build();

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(pendingPrescription));

        assertThatThrownBy(() -> prescriptionService.verifyPrescription(1L, verificationDTO, "pharmacist_jane"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING prescriptions can be verified");
    }

    @Test
    @DisplayName("POS Dispensing Check: Should successfully allow dispensing for an APPROVED prescription")
    void getPrescriptionForDispensing_Approved_Success() {
        pendingPrescription.setStatus(PrescriptionStatus.APPROVED);

        PrescriptionItem item = PrescriptionItem.builder()
                .id(1L)
                .prescription(pendingPrescription)
                .productCatalog(sampleDrug)
                .dosage("500mg, TDS")
                .durationDays(5)
                .prescribedQty(15)
                .build();
        pendingPrescription.getItems().add(item);

        when(prescriptionRepository.findByPrescriptionNumberWithItems("RX-2026-TEST01"))
                .thenReturn(Optional.of(pendingPrescription));

        PrescriptionResponseDTO response = prescriptionService.getPrescriptionForDispensing("RX-2026-TEST01");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PrescriptionStatus.APPROVED);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("POS Dispensing Check: Should block dispensing if prescription is still PENDING")
    void getPrescriptionForDispensing_Pending_ThrowsException() {
        pendingPrescription.setStatus(PrescriptionStatus.PENDING);

        when(prescriptionRepository.findByPrescriptionNumberWithItems("RX-2026-TEST01"))
                .thenReturn(Optional.of(pendingPrescription));

        assertThatThrownBy(() -> prescriptionService.getPrescriptionForDispensing("RX-2026-TEST01"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is still PENDING verification");
    }

    @Test
    @DisplayName("POS Dispensing Check: Should hard-block re-dispensing an already DISPENSED prescription")
    void getPrescriptionForDispensing_AlreadyDispensed_ThrowsException() {
        pendingPrescription.setStatus(PrescriptionStatus.DISPENSED);

        when(prescriptionRepository.findByPrescriptionNumberWithItems("RX-2026-TEST01"))
                .thenReturn(Optional.of(pendingPrescription));

        assertThatThrownBy(() -> prescriptionService.getPrescriptionForDispensing("RX-2026-TEST01"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ALREADY been dispensed and cannot be reused");
    }
}