package com.pharmacy.system.dto;

import com.pharmacy.system.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequestDTO {

    @NotEmpty(message = "Sale must contain at least one item")
    @Valid
    private List<SaleItemRequestDTO> items;

    @NotNull(message = "Payment method is required (CASH, CARD, INSURANCE, SPLIT)")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Paid amount is required")
    @DecimalMin(value = "0.01", message = "Paid amount must be greater than zero")
    private BigDecimal paidAmount;

    // Only required when dispensing PRESCRIPTION_ONLY (POM) or NARCOTICS medicines
    private String prescriptionNumber;

    @Size(max = 150, message = "Customer name must not exceed 150 characters")
    private String customerName;

    @Size(max = 20, message = "Customer phone must not exceed 20 characters")
    private String customerPhone;
}