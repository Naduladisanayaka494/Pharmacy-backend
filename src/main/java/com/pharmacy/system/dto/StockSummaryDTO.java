package com.pharmacy.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSummaryDTO {

    private String drugCode;
    private String brandName;
    private String genericName;
    private Long totalStock;
    private Long activeBatchCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate nearestExpiryDate;


    public StockSummaryDTO(String drugCode, String brandName, String genericName, int totalStock, Long activeBatchCount, LocalDate nearestExpiryDate) {
        this.drugCode = drugCode;
        this.brandName = brandName;
        this.genericName = genericName;
        this.totalStock = (long) totalStock;
        this.activeBatchCount = activeBatchCount;
        this.nearestExpiryDate = nearestExpiryDate;
    }


    public StockSummaryDTO(String drugCode, String brandName, String genericName, Integer totalStock, Long activeBatchCount, LocalDate nearestExpiryDate) {
        this.drugCode = drugCode;
        this.brandName = brandName;
        this.genericName = genericName;
        this.totalStock = totalStock != null ? totalStock.longValue() : 0L;
        this.activeBatchCount = activeBatchCount;
        this.nearestExpiryDate = nearestExpiryDate;
    }
}