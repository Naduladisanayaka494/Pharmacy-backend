package com.pharmacy.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pharmacy.system.entity.PaymentMethod;
import com.pharmacy.system.entity.SaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponseDTO {

    private Long id;
    private String invoiceNumber;
    private String cashierUsername;
    private String prescriptionNumber;
    private String customerName;
    private String customerPhone;

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal netTotal;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;

    private PaymentMethod paymentMethod;
    private SaleStatus status;

    private List<SaleItemResponseDTO> items;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemResponseDTO {
        private Long id;
        private String drugCode;
        private String brandName;
        private String genericName;
        private String batchNumber;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal subtotal;
    }
}