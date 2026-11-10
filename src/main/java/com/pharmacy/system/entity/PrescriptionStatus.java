package com.pharmacy.system.entity;

public enum PrescriptionStatus {

    // Prescription is uploaded and waiting for a licensed pharmacist's clinical review.

    PENDING,


    // Prescription is clinically validated and approved by a pharmacist.
    // Ready for POS billing and dispensing.

    APPROVED,


    // Rejected by the pharmacist due to clinical issues (e.g., unclear dosage, missing doctor seal).

    REJECTED,


    // All prescribed medicines have been billed and dispensed to the patient at POS. Cannot be reused.

    DISPENSED
}