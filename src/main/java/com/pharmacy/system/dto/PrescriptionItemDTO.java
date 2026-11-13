package com.pharmacy.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemDTO {

    @NotBlank(message = "Drug code is required")
    private String drugCode;

    private String brandName;
    private String genericName;

    @NotBlank(message = "Dosage instructions are required")
    private String dosage;

    @NotNull(message = "Duration in days is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationDays;

    @NotNull(message = "Prescribed quantity is required")
    @Positive(message = "Prescribed quantity must be greater than zero")
    private Integer prescribedQty;
}