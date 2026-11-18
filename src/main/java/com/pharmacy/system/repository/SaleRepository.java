package com.pharmacy.system.repository;

import com.pharmacy.system.entity.Sale;
import com.pharmacy.system.entity.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    /**
     * Find sale by unique invoice number (e.g., INV-2026-00001).
     */
    Optional<Sale> findByInvoiceNumber(String invoiceNumber);

    /**
     * Check if an invoice number already exists to ensure uniqueness.
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Optimized query to load a sale with its itemized lines and associated drug details.
     * Prevents N+1 query problem during invoice printing and viewing.
     */
    @Query("""
        SELECT DISTINCT s FROM Sale s
        LEFT JOIN FETCH s.items i
        LEFT JOIN FETCH i.productCatalog
        LEFT JOIN FETCH i.medicineBatch
        LEFT JOIN FETCH s.cashier
        LEFT JOIN FETCH s.prescription
        WHERE s.invoiceNumber = :invoiceNumber
    """)
    Optional<Sale> findByInvoiceNumberWithDetails(@Param("invoiceNumber") String invoiceNumber);

    /**
     * Retrieve all sales completed within a specific date/time range.
     * Used for daily sales reports and financial audits.
     */
    List<Sale> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Retrieve sales conducted by a specific cashier.
     */
    List<Sale> findByCashier_UsernameOrderByCreatedAtDesc(String cashierUsername);

    /**
     * Retrieve sales by transaction status (e.g., COMPLETED, REFUNDED).
     */
    List<Sale> findByStatusOrderByCreatedAtDesc(SaleStatus status);
}