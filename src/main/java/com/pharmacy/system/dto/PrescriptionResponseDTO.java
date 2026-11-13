package com.pharmacy.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pharmacy.system.entity.PrescriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponseDTO {

    private Long id;
    private String prescriptionNumber;
    private String patientName;
    private Integer patientAge;
    private String patientContact;
    private String doctorName;
    private String doctorSlmcRegNo;
    private String fileDownloadUrl;
    private PrescriptionStatus status;
    private String pharmacistNotes;
    private String verifiedByPharmacistUsername;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verifiedAt;

    private List<PrescriptionItemDTO> items;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}