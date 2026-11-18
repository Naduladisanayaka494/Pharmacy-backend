package com.pharmacy.system.repository;

import com.pharmacy.system.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    /**
     * Find all sold items belonging to a specific sale invoice.
     */
    List<SaleItem> findBySale_Id(Long saleId);

    /**
     * Find sales lines associated with a specific medicine batch.
     * Used for Batch Recall audits if NMRA issues a safety recall for a batch.
     */
    List<SaleItem> findByMedicineBatch_Id(Long batchId);

    /**
     * Calculate total units sold for a specific drug code across all sales.
     */
    @Query("""
        SELECT COALESCE(SUM(si.quantity), 0)
        FROM SaleItem si
        WHERE si.productCatalog.drugCode = :drugCode
          AND si.sale.status = 'COMPLETED'
    """)
    Long calculateTotalUnitsSoldForDrug(@Param("drugCode") String drugCode);
}