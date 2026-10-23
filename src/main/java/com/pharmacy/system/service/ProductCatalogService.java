package com.pharmacy.system.service;

import com.pharmacy.system.dto.DrugRequestDTO;
import com.pharmacy.system.dto.DrugResponseDTO;
import com.pharmacy.system.entity.ProductCatalog;
import com.pharmacy.system.entity.ScheduleType;
import com.pharmacy.system.repository.ProductCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCatalogService {

    private final ProductCatalogRepository productCatalogRepository;

    // Register a new medicine into the catalog (Admin & Store Manager)

    @Transactional
    public DrugResponseDTO createDrug(DrugRequestDTO request) {
        log.info("Creating new medicine catalog entry with code: {}", request.getDrugCode());

        // Check for duplicate drug code
        if (productCatalogRepository.existsById(request.getDrugCode())) {
            throw new IllegalArgumentException("Drug code already exists in catalog: " + request.getDrugCode());
        }

        ProductCatalog product = ProductCatalog.builder()
                .drugCode(request.getDrugCode().trim().toUpperCase())
                .brandName(request.getBrandName().trim())
                .genericName(request.getGenericName().trim())
                .category(request.getCategory().trim())
                .dosageForm(request.getDosageForm())
                .scheduleType(request.getScheduleType())
                .isActive(true)
                .build();

        ProductCatalog savedProduct = productCatalogRepository.save(product);
        log.info("Medicine created successfully: {}", savedProduct.getDrugCode());

        return mapToResponseDTO(savedProduct);
    }


    // Retrieve all active medicines

    @Transactional(readOnly = true)
    public List<DrugResponseDTO> getAllActiveDrugs() {
        return productCatalogRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }


    // Get single medicine details by drug code

    @Transactional(readOnly = true)
    public DrugResponseDTO getDrugByCode(String drugCode) {
        ProductCatalog product = findProductOrThrow(drugCode);
        return mapToResponseDTO(product);
    }


    // Unified Fast Search (Matches Brand Name or Generic Name)

    @Transactional(readOnly = true)
    public List<DrugResponseDTO> searchDrugs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveDrugs();
        }

        return productCatalogRepository.searchByKeyword(keyword.trim())
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    // Filter medicines by Schedule Type

    @Transactional(readOnly = true)
    public List<DrugResponseDTO> getDrugsBySchedule(ScheduleType scheduleType) {
        return productCatalogRepository.findByScheduleTypeAndIsActiveTrue(scheduleType)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }


    // Filter medicines by Category

    @Transactional(readOnly = true)
    public List<DrugResponseDTO> getDrugsByCategory(String category) {
        return productCatalogRepository.findByCategoryIgnoreCaseAndIsActiveTrue(category.trim())
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }


    // Update an existing medicines information

    @Transactional
    public DrugResponseDTO updateDrug(String drugCode, DrugRequestDTO request) {
        log.info("Updating medicine details for code: {}", drugCode);
        ProductCatalog product = findProductOrThrow(drugCode);

        product.setBrandName(request.getBrandName().trim());
        product.setGenericName(request.getGenericName().trim());
        product.setCategory(request.getCategory().trim());
        product.setDosageForm(request.getDosageForm());
        product.setScheduleType(request.getScheduleType());

        ProductCatalog updatedProduct = productCatalogRepository.save(product);
        log.info("Medicine updated successfully: {}", updatedProduct.getDrugCode());

        return mapToResponseDTO(updatedProduct);
    }


    // Deactivate a medicine instead of physical deletion

    @Transactional
    public void deactivateDrug(String drugCode) {
        log.info("Deactivating medicine: {}", drugCode);
        ProductCatalog product = findProductOrThrow(drugCode);

        product.setIsActive(false);
        productCatalogRepository.save(product);
        log.info("Medicine deactivated successfully: {}", drugCode);
    }

    // Helper Methods

    private ProductCatalog findProductOrThrow(String drugCode) {
        return productCatalogRepository.findById(drugCode.trim().toUpperCase())
                .filter(ProductCatalog::getIsActive)
                .orElseThrow(() -> new IllegalArgumentException("Active medicine not found with drug code: " + drugCode));
    }

    private DrugResponseDTO mapToResponseDTO(ProductCatalog product) {
        return DrugResponseDTO.builder()
                .drugCode(product.getDrugCode())
                .brandName(product.getBrandName())
                .genericName(product.getGenericName())
                .category(product.getCategory())
                .dosageForm(product.getDosageForm())
                .scheduleType(product.getScheduleType())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}