package com.pharmacy.system.service;

import com.pharmacy.system.dto.BatchRequestDTO;
import com.pharmacy.system.dto.BatchResponseDTO;
import com.pharmacy.system.dto.StockSummaryDTO;
import com.pharmacy.system.entity.DosageForm;
import com.pharmacy.system.entity.MedicineBatch;
import com.pharmacy.system.entity.ProductCatalog;
import com.pharmacy.system.entity.ScheduleType;
import com.pharmacy.system.repository.MedicineBatchRepository;
import com.pharmacy.system.repository.ProductCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicineBatchServiceTest {

    @Mock
    private MedicineBatchRepository batchRepository;

    @Mock
    private ProductCatalogRepository productCatalogRepository;

    @InjectMocks
    private MedicineBatchService batchService;

    private ProductCatalog activeDrug;
    private BatchRequestDTO validBatchRequest;
    private MedicineBatch savedBatch;

    @BeforeEach
    void setUp() {
        activeDrug = ProductCatalog.builder()
                .drugCode("DRG-AMOX-500")
                .brandName("Amoxil 500mg")
                .genericName("Amoxicillin")
                .category("Antibiotics")
                .dosageForm(DosageForm.CAPSULE)
                .scheduleType(ScheduleType.PRESCRIPTION_ONLY)
                .isActive(true)
                .build();

        validBatchRequest = BatchRequestDTO.builder()
                .drugCode("DRG-AMOX-500")
                .batchNumber("BAT-2026-001")
                .mfgDate(LocalDate.now().minusMonths(1))
                .expDate(LocalDate.now().plusYears(2))
                .unitCost(new BigDecimal("15.00"))
                .retailPrice(new BigDecimal("22.50"))
                .stockQty(100)
                .build();

        savedBatch = MedicineBatch.builder()
                .id(1L)
                .productCatalog(activeDrug)
                .batchNumber("BAT-2026-001")
                .mfgDate(validBatchRequest.getMfgDate())
                .expDate(validBatchRequest.getExpDate())
                .unitCost(validBatchRequest.getUnitCost())
                .retailPrice(validBatchRequest.getRetailPrice())
                .stockQty(100)
                .build();
    }

    @Test
    @DisplayName("Should successfully add batch when all clinical & financial rules pass")
    void addBatch_Success() {
        when(productCatalogRepository.findById("DRG-AMOX-500")).thenReturn(Optional.of(activeDrug));
        when(batchRepository.existsByProductCatalog_DrugCodeAndBatchNumber("DRG-AMOX-500", "BAT-2026-001")).thenReturn(false);
        when(batchRepository.save(any(MedicineBatch.class))).thenReturn(savedBatch);

        BatchResponseDTO response = batchService.addBatch(validBatchRequest);

        assertThat(response).isNotNull();
        assertThat(response.getBatchNumber()).isEqualTo("BAT-2026-001");
        assertThat(response.getRetailPrice()).isEqualByComparingTo(new BigDecimal("22.50"));
        assertThat(response.getDrugCode()).isEqualTo("DRG-AMOX-500");
        verify(batchRepository).save(any(MedicineBatch.class));
    }

    @Test
    @DisplayName("Financial Rule: Should throw exception when retailPrice is strictly less than unitCost")
    void addBatch_FinancialRule_RetailPriceLessThanUnitCost_ThrowsException() {
        validBatchRequest.setRetailPrice(new BigDecimal("10.00"));

        when(productCatalogRepository.findById("DRG-AMOX-500")).thenReturn(Optional.of(activeDrug));
        when(batchRepository.existsByProductCatalog_DrugCodeAndBatchNumber("DRG-AMOX-500", "BAT-2026-001")).thenReturn(false);

        assertThatThrownBy(() -> batchService.addBatch(validBatchRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Retail price cannot be less than unit cost");

        verify(batchRepository, never()).save(any(MedicineBatch.class));
    }

    @Test
    @DisplayName("Date Rule: Should throw exception when expDate is before or equal to mfgDate")
    void addBatch_DateRule_ExpBeforeMfg_ThrowsException() {
        validBatchRequest.setMfgDate(LocalDate.now().plusMonths(1));
        validBatchRequest.setExpDate(LocalDate.now());

        when(productCatalogRepository.findById("DRG-AMOX-500")).thenReturn(Optional.of(activeDrug));
        when(batchRepository.existsByProductCatalog_DrugCodeAndBatchNumber("DRG-AMOX-500", "BAT-2026-001")).thenReturn(false);

        assertThatThrownBy(() -> batchService.addBatch(validBatchRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expiry date must be after manufacturing date");

        verify(batchRepository, never()).save(any(MedicineBatch.class));
    }

    @Test
    @DisplayName("Clinical Rule: Should hard-block registering an already expired batch")
    void addBatch_ExpiredBatch_ThrowsException() {
        validBatchRequest.setMfgDate(LocalDate.now().minusYears(2));
        validBatchRequest.setExpDate(LocalDate.now().minusDays(1));

        when(productCatalogRepository.findById("DRG-AMOX-500")).thenReturn(Optional.of(activeDrug));
        when(batchRepository.existsByProductCatalog_DrugCodeAndBatchNumber("DRG-AMOX-500", "BAT-2026-001")).thenReturn(false);

        assertThatThrownBy(() -> batchService.addBatch(validBatchRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot register an expired medicine batch");

        verify(batchRepository, never()).save(any(MedicineBatch.class));
    }

    @Test
    @DisplayName("Duplicate Check: Should throw exception when batch number already exists for drug")
    void addBatch_DuplicateBatchNumber_ThrowsException() {
        when(productCatalogRepository.findById("DRG-AMOX-500")).thenReturn(Optional.of(activeDrug));
        when(batchRepository.existsByProductCatalog_DrugCodeAndBatchNumber("DRG-AMOX-500", "BAT-2026-001")).thenReturn(true);

        assertThatThrownBy(() -> batchService.addBatch(validBatchRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists for drug code");

        verify(batchRepository, never()).save(any(MedicineBatch.class));
    }

    @Test
    @DisplayName("FEFO Dispensing: Should return available non-expired batches sorted by earliest expiry")
    void getAvailableBatchesForDispensing_Success() {
        when(productCatalogRepository.existsById("DRG-AMOX-500")).thenReturn(true);
        when(batchRepository.findAvailableBatchesForDispensing(eq("DRG-AMOX-500"), any(LocalDate.class)))
                .thenReturn(List.of(savedBatch));

        List<BatchResponseDTO> result = batchService.getAvailableBatchesForDispensing("DRG-AMOX-500");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBatchNumber()).isEqualTo("BAT-2026-001");
    }

    @Test
    @DisplayName("Low Stock: Should return medicines at or below threshold")
    void getLowStockOverview_ReturnsFilteredList() {
        StockSummaryDTO lowStock = StockSummaryDTO.builder()
                .drugCode("DRG-AMOX-500")
                .brandName("Amoxil 500mg")
                .totalStock(20L)
                .build();

        StockSummaryDTO ampleStock = StockSummaryDTO.builder()
                .drugCode("DRG-PARA-500")
                .brandName("Panadol 500mg")
                .totalStock(200L)
                .build();

        when(batchRepository.getStockSummaryOverview(any(LocalDate.class)))
                .thenReturn(List.of(lowStock, ampleStock));

        List<StockSummaryDTO> result = batchService.getLowStockOverview(50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDrugCode()).isEqualTo("DRG-AMOX-500");
    }
}