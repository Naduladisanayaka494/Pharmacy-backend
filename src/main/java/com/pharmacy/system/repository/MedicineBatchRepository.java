package com.pharmacy.system.repository;

import com.pharmacy.system.dto.StockSummaryDTO;
import com.pharmacy.system.entity.MedicineBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, Long> {


    boolean existsByProductCatalog_DrugCodeAndBatchNumber(String drugCode, String batchNumber);

    List<MedicineBatch> findByProductCatalog_DrugCode(String drugCode);

    Optional<MedicineBatch> findByProductCatalog_DrugCodeAndBatchNumber(String drugCode, String batchNumber);


    @Query("""
        SELECT b FROM MedicineBatch b
        WHERE b.productCatalog.drugCode = :drugCode
          AND b.stockQty > 0
          AND b.expDate > :currentDate
        ORDER BY b.expDate ASC, b.mfgDate ASC
    """)
    List<MedicineBatch> findAvailableBatchesForDispensing(
            @Param("drugCode") String drugCode,
            @Param("currentDate") LocalDate currentDate
    );


    @Query("""
        SELECT b FROM MedicineBatch b
        WHERE b.stockQty > 0
          AND b.expDate BETWEEN :today AND :thresholdDate
        ORDER BY b.expDate ASC
    """)
    List<MedicineBatch> findNearExpiryBatches(
            @Param("today") LocalDate today,
            @Param("thresholdDate") LocalDate thresholdDate
    );


    @Query("""
        SELECT b FROM MedicineBatch b
        WHERE b.stockQty > 0
          AND b.expDate <= :currentDate
        ORDER BY b.expDate ASC
    """)
    List<MedicineBatch> findExpiredBatchesWithStock(@Param("currentDate") LocalDate currentDate);


    @Query("""
        SELECT COALESCE(SUM(b.stockQty), 0)
        FROM MedicineBatch b
        WHERE b.productCatalog.drugCode = :drugCode
          AND b.expDate > :currentDate
    """)
    Long calculateTotalSellableStock(
            @Param("drugCode") String drugCode,
            @Param("currentDate") LocalDate currentDate
    );


    @Query("""
        SELECT new com.pharmacy.system.dto.StockSummaryDTO(
            p.drugCode,
            p.brandName,
            p.genericName,
            COALESCE(SUM(CASE WHEN b.expDate > :currentDate THEN b.stockQty ELSE 0 END), 0L),
            COUNT(CASE WHEN b.stockQty > 0 AND b.expDate > :currentDate THEN b.id ELSE NULL END),
            MIN(CASE WHEN b.stockQty > 0 AND b.expDate > :currentDate THEN b.expDate ELSE NULL END)
        )
        FROM ProductCatalog p
        LEFT JOIN MedicineBatch b ON b.productCatalog = p
        WHERE p.isActive = true
        GROUP BY p.drugCode, p.brandName, p.genericName
        ORDER BY p.brandName ASC
    """)
    List<StockSummaryDTO> getStockSummaryOverview(@Param("currentDate") LocalDate currentDate);
}