package com.pharmacy.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemRequestDTO {

    @NotBlank(message = "Drug code is required")
    private String drugCode;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;
}