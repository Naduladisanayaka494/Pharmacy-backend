package com.pharmacy.system.repository;

import com.pharmacy.system.entity.Prescription;
import com.pharmacy.system.entity.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    /**
     * Find prescription by unique prescription number (e.g. RX-2026-00001).
     * Used by POS Cashier to lookup approved prescription for dispensing.
     */
    Optional<Prescription> findByPrescriptionNumber(String prescriptionNumber);

    /**
     * Check if a prescription number already exists.
     */
    boolean existsByPrescriptionNumber(String prescriptionNumber);

    /**
     * Retrieve all prescriptions matching a given status (e.g., PENDING for Pharmacist dashboard).
     * Ordered by submission date (FIFO - oldest first so pending prescriptions are verified in order).
     */
    List<Prescription> findByStatusOrderByCreatedAtAsc(PrescriptionStatus status);

    /**
     * Search prescriptions by patient contact number.
     */
    List<Prescription> findByPatientContactOrderByCreatedAtDesc(String patientContact);

    /**
     * Search prescriptions by Doctor SLMC Registration Number (for medical audits).
     */
    List<Prescription> findByDoctorSlmcRegNoOrderByCreatedAtDesc(String doctorSlmcRegNo);

    /**
     * Count pending prescriptions awaiting verification (useful for dashboard badge counters).
     */
    long countByStatus(PrescriptionStatus status);

    /**
     * Fetch prescription along with its child items in a single query to avoid N+1 problem.
     */
    @Query("""
        SELECT DISTINCT p FROM Prescription p
        LEFT JOIN FETCH p.items i
        LEFT JOIN FETCH i.productCatalog
        WHERE p.prescriptionNumber = :prescriptionNumber
    """)
    Optional<Prescription> findByPrescriptionNumberWithItems(@Param("prescriptionNumber") String prescriptionNumber);
}