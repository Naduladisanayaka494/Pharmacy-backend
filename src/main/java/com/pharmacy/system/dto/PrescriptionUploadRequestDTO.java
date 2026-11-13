package com.pharmacy.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class PrescriptionUploadRequestDTO {

    @NotBlank(message = "Patient name is required")
    @Size(min = 2, max = 150, message = "Patient name must be between 2 and 150 characters")
    private String patientName;

    @NotNull(message = "Patient age is required")
    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 150, message = "Please enter a valid age")
    private Integer patientAge;

    @NotBlank(message = "Patient contact number is required")
    @Pattern(regexp = "^(?:\\+94|0)?[0-9]{9,10}$", message = "Please provide a valid contact number")
    private String patientContact;

    @NotBlank(message = "Doctor name is required")
    @Size(min = 2, max = 150, message = "Doctor name must be between 2 and 150 characters")
    private String doctorName;

    @NotBlank(message = "Doctor SLMC registration number is required")
    @Size(min = 3, max = 50, message = "SLMC registration number must be between 3 and 50 characters")
    private String doctorSlmcRegNo;

    // Optional list of items extracted or entered on submission
    private List<PrescriptionItemDTO> items;
}