package com.pharmacy.system.service;

import com.pharmacy.system.dto.BatchRequestDTO;
import com.pharmacy.system.dto.BatchResponseDTO;
import com.pharmacy.system.dto.StockSummaryDTO;
import com.pharmacy.system.entity.MedicineBatch;
import com.pharmacy.system.entity.ProductCatalog;
import com.pharmacy.system.repository.MedicineBatchRepository;
import com.pharmacy.system.repository.ProductCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineBatchService {

    private final MedicineBatchRepository batchRepository;
    private final ProductCatalogRepository productCatalogRepository;

    @Transactional
    public BatchResponseDTO addBatch(BatchRequestDTO requestDTO) {
        log.info("Attempting to add new batch: {} for drug: {}", requestDTO.getBatchNumber(), requestDTO.getDrugCode());

        // Verify parent drug exists and is active
        ProductCatalog productCatalog = productCatalogRepository.findById(requestDTO.getDrugCode())
                .orElseThrow(() -> new IllegalArgumentException("Drug not found with code: " + requestDTO.getDrugCode()));

        if (!Boolean.TRUE.equals(productCatalog.getIsActive())) {
            throw new IllegalStateException("Cannot add batch to an inactive drug catalog: " + requestDTO.getDrugCode());
        }

        // Prevent duplicate batch number for the same drug
        if (batchRepository.existsByProductCatalog_DrugCodeAndBatchNumber(requestDTO.getDrugCode(), requestDTO.getBatchNumber())) {
            throw new IllegalArgumentException(
                    String.format("Batch '%s' already exists for drug code '%s'", requestDTO.getBatchNumber(), requestDTO.getDrugCode())
            );
        }

        // Financial Rule Validation
        if (requestDTO.getRetailPrice().compareTo(requestDTO.getUnitCost()) < 0) {
            throw new IllegalArgumentException("Retail price cannot be less than unit cost");
        }

        // Clinical Date Rule Validation
        if (!requestDTO.getExpDate().isAfter(requestDTO.getMfgDate())) {
            throw new IllegalArgumentException("Expiry date must be after manufacturing date");
        }

        // expDate must not be past or today
        if (!requestDTO.getExpDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot register an expired medicine batch. Expiry date must be in the future");
        }

        MedicineBatch batch = MedicineBatch.builder()
                .productCatalog(productCatalog)
                .batchNumber(requestDTO.getBatchNumber())
                .mfgDate(requestDTO.getMfgDate())
                .expDate(requestDTO.getExpDate())
                .unitCost(requestDTO.getUnitCost())
                .retailPrice(requestDTO.getRetailPrice())
                .stockQty(requestDTO.getStockQty())
                .build();

        MedicineBatch savedBatch = batchRepository.save(batch);
        log.info("Successfully registered batch ID: {} for drug: {}", savedBatch.getId(), requestDTO.getDrugCode());

        return mapToDTO(savedBatch);
    }


    // Retrieve all batches for a specific drug code.

    @Transactional(readOnly = true)
    public List<BatchResponseDTO> getBatchesByDrugCode(String drugCode) {
        if (!productCatalogRepository.existsById(drugCode)) {
            throw new IllegalArgumentException("Drug not found with code: " + drugCode);
        }
        return batchRepository.findByProductCatalog_DrugCode(drugCode)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    // FEFO Allocation: Retrieve available non-expired batches for dispensing, ordered by earliest expiry date.

    @Transactional(readOnly = true)
    public List<BatchResponseDTO> getAvailableBatchesForDispensing(String drugCode) {
        if (!productCatalogRepository.existsById(drugCode)) {
            throw new IllegalArgumentException("Drug not found with code: " + drugCode);
        }
        return batchRepository.findAvailableBatchesForDispensing(drugCode, LocalDate.now())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    // Retrieve batches expiring within a given number of days (default 30 days).

    @Transactional(readOnly = true)
    public List<BatchResponseDTO> getNearExpiryBatches(int daysThreshold) {
        if (daysThreshold <= 0) {
            daysThreshold = 30;
        }
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(daysThreshold);

        return batchRepository.findNearExpiryBatches(today, thresholdDate)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    //  Retrieve medicines where total available stock is at or below the given threshold (default 50 units).

    @Transactional(readOnly = true)
    public List<StockSummaryDTO> getLowStockOverview(int threshold) {
        int reorderThreshold = threshold <= 0 ? 50 : threshold;
        LocalDate today = LocalDate.now();
        List<StockSummaryDTO> allSummaries = batchRepository.getStockSummaryOverview(today);

        return allSummaries.stream()
                .filter(summary -> summary.getTotalStock() <= reorderThreshold)
                .collect(Collectors.toList());
    }


    // Retrieve aggregated stock summary overview for all active medicines.

    @Transactional(readOnly = true)
    public List<StockSummaryDTO> getStockSummaryOverview() {
        return batchRepository.getStockSummaryOverview(LocalDate.now());
    }

    private BatchResponseDTO mapToDTO(MedicineBatch batch) {
        return BatchResponseDTO.builder()
                .id(batch.getId())
                .drugCode(batch.getProductCatalog().getDrugCode())
                .brandName(batch.getProductCatalog().getBrandName())
                .genericName(batch.getProductCatalog().getGenericName())
                .batchNumber(batch.getBatchNumber())
                .mfgDate(batch.getMfgDate())
                .expDate(batch.getExpDate())
                .unitCost(batch.getUnitCost())
                .retailPrice(batch.getRetailPrice())
                .stockQty(batch.getStockQty())
                .isExpired(batch.isExpired())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}