package com.pharmacy.system.repository;

import com.pharmacy.system.entity.DosageForm;
import com.pharmacy.system.entity.ProductCatalog;
import com.pharmacy.system.entity.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCatalogRepository extends JpaRepository<ProductCatalog, String> {

    // Get all active medicines
    List<ProductCatalog> findByIsActiveTrue();

    // Search by brand name
    List<ProductCatalog> findByBrandNameContainingIgnoreCaseAndIsActiveTrue(String brandName);

    // Search by generic name
    List<ProductCatalog> findByGenericNameContainingIgnoreCaseAndIsActiveTrue(String genericName);

    // Search Single search query for Brand Name or Generic Name
    @Query("SELECT p FROM ProductCatalog p WHERE p.isActive = true AND " +
            "(LOWER(p.brandName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.genericName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ProductCatalog> searchByKeyword(@Param("keyword") String keyword);

    // Filter medicines by Category
    List<ProductCatalog> findByCategoryIgnoreCaseAndIsActiveTrue(String category);

    // Filter by Schedule Type
    List<ProductCatalog> findByScheduleTypeAndIsActiveTrue(ScheduleType scheduleType);

    // Filter by Dosage Form
    List<ProductCatalog> findByDosageFormAndIsActiveTrue(DosageForm dosageForm);
}