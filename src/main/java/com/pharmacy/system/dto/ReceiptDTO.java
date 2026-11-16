package com.pharmacy.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pharmacy.system.entity.PaymentMethod;
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
public class ReceiptDTO {

    // Pharmacy Branch
    @Builder.Default
    private String pharmacyName = "MediCare Enterprise Pharmacy";
    @Builder.Default
    private String address = "No. 123, Galle Road, Colombo 03, Sri Lanka";
    @Builder.Default
    private String contactNumber = "+94 11 234 5678";

    // Transaction Details
    private String invoiceNumber;
    private String cashierName;
    private String prescriptionNumber;
    private String customerName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transactionDate;

    // Itemized lines
    private List<ReceiptItemDTO> items;

    // Financial Totals
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal netTotal;
    private BigDecimal paidAmount;
    private BigDecimal balance;
    private PaymentMethod paymentMethod;

    @Builder.Default
    private String footerMessage = "Thank you for choosing MediCare Pharmacy. Get well soon!";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptItemDTO {
        private String drugName;
        private String batchNumber;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}