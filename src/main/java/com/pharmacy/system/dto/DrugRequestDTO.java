package com.pharmacy.system.dto;

import com.pharmacy.system.entity.DosageForm;
import com.pharmacy.system.entity.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DrugRequestDTO {

    @NotBlank(message = "Drug code is required")
    @Size(min = 3, max = 50, message = "Drug code must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Drug code must contain only uppercase alphanumeric characters, hyphens or underscores")
    private String drugCode;

    @NotBlank(message = "Brand name is required")
    @Size(min = 2, max = 100, message = "Brand name must be between 2 and 100 characters")
    private String brandName;

    @NotBlank(message = "Generic name is required")
    @Size(min = 2, max = 100, message = "Generic name must be between 2 and 100 characters")
    private String genericName;

    @NotBlank(message = "Category is required")
    @Size(min = 2, max = 50, message = "Category must be between 2 and 50 characters")
    private String category;

    @NotNull(message = "Dosage form is required (e.g. TABLET, CAPSULE, SYRUP, INJECTION, OINTMENT, DROPS)")
    private DosageForm dosageForm;

    @NotNull(message = "Schedule type is required (e.g. OVER_THE_COUNTER, PRESCRIPTION_ONLY, NARCOTICS)")
    private ScheduleType scheduleType;
}
