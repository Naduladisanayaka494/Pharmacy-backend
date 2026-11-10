package com.pharmacy.system.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "prescriptions",
        indexes = {
                @Index(name = "idx_prescription_number", columnList = "prescription_number", unique = true),
                @Index(name = "idx_prescription_status", columnList = "status"),
                @Index(name = "idx_patient_contact", columnList = "patient_contact")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_number", nullable = false, unique = true, length = 50)
    private String prescriptionNumber; // e.g., "RX-2026-00001"

    @Column(name = "patient_name", nullable = false, length = 150)
    private String patientName;

    @Column(name = "patient_age", nullable = false)
    private Integer patientAge;

    @Column(name = "patient_contact", nullable = false, length = 20)
    private String patientContact;

    @Column(name = "doctor_name", nullable = false, length = 150)
    private String doctorName;

    @Column(name = "doctor_slmc_reg_no", nullable = false, length = 50)
    private String doctorSlmcRegNo; // Sri Lanka Medical Council Registration Number

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath; // Stored path of the uploaded image or PDF

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PrescriptionStatus status = PrescriptionStatus.PENDING;

    @Column(name = "pharmacist_notes", length = 500)
    private String pharmacistNotes; // Notes or rejection reason provided by the pharmacist

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy; // Licensed Pharmacist who verified the prescription

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PrescriptionItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


     // Helper method to add items and maintain bidirectional relationship.

    public void addItem(PrescriptionItem item) {
        items.add(item);
        item.setPrescription(this);
    }
}