package com.pharmacy.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRequestDTO {

    @NotBlank(message = "Drug code is required")
    @Size(max = 50, message = "Drug code must not exceed 50 characters")
    private String drugCode;

    @NotBlank(message = "Batch number is required")
    @Size(min = 2, max = 50, message = "Batch number must be between 2 and 50 characters")
    private String batchNumber;

    @NotNull(message = "Manufacturing date is required")
    @PastOrPresent(message = "Manufacturing date cannot be in the future")
    private LocalDate mfgDate;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be a future date")
    private LocalDate expDate;

    @NotNull(message = "Unit cost is required")
    @DecimalMin(value = "0.01", message = "Unit cost must be greater than zero")
    private BigDecimal unitCost;

    @NotNull(message = "Retail price is required")
    @DecimalMin(value = "0.01", message = "Retail price must be greater than zero")
    private BigDecimal retailPrice;

    @NotNull(message = "Stock quantity is required")
    @Positive(message = "Stock quantity must be greater than zero")
    private Integer stockQty;
}