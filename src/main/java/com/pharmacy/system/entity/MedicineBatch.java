package com.pharmacy.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "medicine_batch",
        indexes = {
                @Index(name = "idx_batch_exp_date", columnList = "exp_date"),
                @Index(name = "idx_batch_number", columnList = "batch_number"),
                @Index(name = "idx_batch_drug_code", columnList = "drug_code")
        },
        uniqueConstraints = {
                // Prevent duplicate batch numbers for the same medicine
                @UniqueConstraint(name = "uk_drug_batch_number", columnNames = {"drug_code", "batch_number"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Long id;

    // Many batches belong to one ProductCatalog
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "drug_code",
            referencedColumnName = "drug_code",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_batch_product_catalog")
    )
    private ProductCatalog productCatalog;

    @Column(name = "batch_number", nullable = false, length = 50)
    private String batchNumber;

    @Column(name = "unit_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "retail_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "mfg_date", nullable = false)
    private LocalDate mfgDate;

    @Column(name = "exp_date", nullable = false)
    private LocalDate expDate;

    @Column(name = "stock_qty", nullable = false)
    private Integer stockQty;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;



    // Business Helper Methods
    public boolean isExpired() {
        return this.expDate != null && this.expDate.isBefore(LocalDate.now());
    }

    public boolean hasStock() {
        return this.stockQty != null && this.stockQty > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedicineBatch that = (MedicineBatch) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}