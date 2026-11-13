package com.pharmacy.system.dto;

import com.pharmacy.system.entity.PrescriptionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionVerificationDTO {

    @NotNull(message = "Verification status is required (APPROVED or REJECTED)")
    private PrescriptionStatus status;

    @Size(max = 500, message = "Pharmacist notes cannot exceed 500 characters")
    private String pharmacistNotes; // Required if REJECTED

    // Confirmed medicine items approved for dispensing at POS
    @Valid
    private List<PrescriptionItemDTO> verifiedItems;
}