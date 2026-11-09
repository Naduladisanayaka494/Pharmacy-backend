package com.pharmacy.system.service;

import com.pharmacy.system.dto.DrugRequestDTO;
import com.pharmacy.system.dto.DrugResponseDTO;
import com.pharmacy.system.entity.DosageForm;
import com.pharmacy.system.entity.ProductCatalog;
import com.pharmacy.system.entity.ScheduleType;
import com.pharmacy.system.repository.ProductCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceTest {

    @Mock
    private ProductCatalogRepository productCatalogRepository;

    @InjectMocks
    private ProductCatalogService productCatalogService;

    private DrugRequestDTO sampleRequestDTO;
    private ProductCatalog sampleDrug;

    @BeforeEach
    void setUp() {
        sampleRequestDTO = DrugRequestDTO.builder()
                .drugCode("DRG-PARA-500")
                .brandName("Panadol 500mg")
                .genericName("Paracetamol")
                .category("Analgesic")
                .dosageForm(DosageForm.TABLET)
                .scheduleType(ScheduleType.OVER_THE_COUNTER)
                .build();

        sampleDrug = ProductCatalog.builder()
                .drugCode("DRG-PARA-500")
                .brandName("Panadol 500mg")
                .genericName("Paracetamol")
                .category("Analgesic")
                .dosageForm(DosageForm.TABLET)
                .scheduleType(ScheduleType.OVER_THE_COUNTER)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new drug when drug code is unique")
    void createDrug_Success() {
        when(productCatalogRepository.existsById("DRG-PARA-500")).thenReturn(false);
        when(productCatalogRepository.save(any(ProductCatalog.class))).thenReturn(sampleDrug);

        DrugResponseDTO response = productCatalogService.createDrug(sampleRequestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getDrugCode()).isEqualTo("DRG-PARA-500");
        assertThat(response.getBrandName()).isEqualTo("Panadol 500mg");
        verify(productCatalogRepository).save(any(ProductCatalog.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when creating drug with duplicate code")
    void createDrug_DuplicateCode_ThrowsException() {
        when(productCatalogRepository.existsById("DRG-PARA-500")).thenReturn(true);

        assertThatThrownBy(() -> productCatalogService.createDrug(sampleRequestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(productCatalogRepository, never()).save(any(ProductCatalog.class));
    }

    @Test
    @DisplayName("Should return drug by code when active drug exists")
    void getDrugByCode_Success() {
        when(productCatalogRepository.findById("DRG-PARA-500")).thenReturn(Optional.of(sampleDrug));

        DrugResponseDTO response = productCatalogService.getDrugByCode("DRG-PARA-500");

        assertThat(response).isNotNull();
        assertThat(response.getDrugCode()).isEqualTo("DRG-PARA-500");
        assertThat(response.getBrandName()).isEqualTo("Panadol 500mg");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when drug code is not found")
    void getDrugByCode_NotFound_ThrowsException() {
        when(productCatalogRepository.findById("UNKNOWN-DRG")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productCatalogService.getDrugByCode("UNKNOWN-DRG"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should update an existing drug successfully")
    void updateDrug_Success() {
        when(productCatalogRepository.findById("DRG-PARA-500")).thenReturn(Optional.of(sampleDrug));
        when(productCatalogRepository.save(any(ProductCatalog.class))).thenReturn(sampleDrug);

        DrugRequestDTO updateRequest = DrugRequestDTO.builder()
                .drugCode("DRG-PARA-500")
                .brandName("Panadol Extra 500mg")
                .genericName("Paracetamol + Caffeine")
                .category("Analgesic")
                .dosageForm(DosageForm.TABLET)
                .scheduleType(ScheduleType.OVER_THE_COUNTER)
                .build();

        DrugResponseDTO updatedResponse = productCatalogService.updateDrug("DRG-PARA-500", updateRequest);

        assertThat(updatedResponse).isNotNull();
        verify(productCatalogRepository).save(sampleDrug);
    }

    @Test
    @DisplayName("Should deactivate drug instead of physical deletion")
    void deactivateDrug_Success() {
        when(productCatalogRepository.findById("DRG-PARA-500")).thenReturn(Optional.of(sampleDrug));

        productCatalogService.deactivateDrug("DRG-PARA-500");

        assertThat(sampleDrug.getIsActive()).isFalse();
        verify(productCatalogRepository).save(sampleDrug);
    }

    @Test
    @DisplayName("Should search drugs by keyword using searchByKeyword")
    void searchDrugs_Success() {
        when(productCatalogRepository.searchByKeyword("pana")).thenReturn(List.of(sampleDrug));

        List<DrugResponseDTO> results = productCatalogService.searchDrugs("pana");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getBrandName()).isEqualTo("Panadol 500mg");
    }
}