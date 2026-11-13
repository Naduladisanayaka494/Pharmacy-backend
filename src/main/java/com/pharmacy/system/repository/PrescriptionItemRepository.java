package com.pharmacy.system.repository;

import com.pharmacy.system.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {


    // Find all prescription items associated with a given prescription ID.

    List<PrescriptionItem> findByPrescription_Id(Long prescriptionId);


    // Find how many times a specific drug code has been prescribed (for analytics).

    List<PrescriptionItem> findByProductCatalog_DrugCode(String drugCode);
}