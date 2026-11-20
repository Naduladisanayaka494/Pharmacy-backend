package com.pharmacy.system.service;

import com.pharmacy.system.dto.SaleItemRequestDTO;
import com.pharmacy.system.dto.SaleRequestDTO;
import com.pharmacy.system.dto.SaleResponseDTO;
import com.pharmacy.system.entity.DosageForm;
import com.pharmacy.system.entity.MedicineBatch;
import com.pharmacy.system.entity.PaymentMethod;
import com.pharmacy.system.entity.Prescription;
import com.pharmacy.system.entity.PrescriptionStatus;
import com.pharmacy.system.entity.ProductCatalog;
import com.pharmacy.system.entity.Role;
import com.pharmacy.system.entity.Sale;
import com.pharmacy.system.entity.SaleStatus;
import com.pharmacy.system.entity.ScheduleType;
import com.pharmacy.system.entity.User;
import com.pharmacy.system.repository.MedicineBatchRepository;
import com.pharmacy.system.repository.PrescriptionRepository;
import com.pharmacy.system.repository.ProductCatalogRepository;
import com.pharmacy.system.repository.SaleRepository;
import com.pharmacy.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductCatalogRepository productCatalogRepository;

    @Mock
    private MedicineBatchRepository batchRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SaleService saleService;

    private User cashierUser;
    private ProductCatalog otcDrug;
    private ProductCatalog pomDrug;
    private MedicineBatch batch1;
    private MedicineBatch batch2;
    private Prescription approvedPrescription;

    @BeforeEach
    void setUp() {
        cashierUser = User.builder()
                .id(1L)
                .username("cashier_kamal")
                .fullName("Kamal Silva")
                .role(Role.ROLE_CASHIER)
                .build();

        otcDrug = ProductCatalog.builder()
                .drugCode("DRG-PARA-500")
                .brandName("Panadol 500mg")
                .genericName("Paracetamol")
                .category("Analgesic")
                .dosageForm(DosageForm.TABLET)
                .scheduleType(ScheduleType.OVER_THE_COUNTER)
                .isActive(true)
                .build();

        pomDrug = ProductCatalog.builder()
                .drugCode("DRG-AMOX-500")
                .brandName("Amoxil 500mg")
                .genericName("Amoxicillin")
                .category("Antibiotics")
                .dosageForm(DosageForm.CAPSULE)
                .scheduleType(ScheduleType.PRESCRIPTION_ONLY)
                .isActive(true)
                .build();

        batch1 = MedicineBatch.builder()
                .id(101L)
                .productCatalog(otcDrug)
                .batchNumber("BAT-PANA-01")
                .stockQty(15)
                .retailPrice(new BigDecimal("10.00"))
                .unitCost(new BigDecimal("6.00"))
                .expDate(LocalDate.now().plusMonths(3)) // Expiring first
                .build();

        batch2 = MedicineBatch.builder()
                .id(102L)
                .productCatalog(otcDrug)
                .batchNumber("BAT-PANA-02")
                .stockQty(50)
                .retailPrice(new BigDecimal("10.00"))
                .unitCost(new BigDecimal("6.00"))
                .expDate(LocalDate.now().plusYears(1)) // Expiring later
                .build();

        approvedPrescription = Prescription.builder()
                .id(1L)
                .prescriptionNumber("RX-2026-VAL01")
                .patientName("Sunil Perera")
                .status(PrescriptionStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("FEFO Multi-Batch: Should auto-split deduction across multiple batches when requested qty exceeds first batch")
    void processCheckout_FefoMultiBatch_Success() {
        // Customer requests 25 tablets. Batch 1 has 15, Batch 2 has 50.
        SaleItemRequestDTO itemRequest = SaleItemRequestDTO.builder()
                .drugCode("DRG-PARA-500")
                .quantity(25)
                .discount(BigDecimal.ZERO)
                .build();

        SaleRequestDTO saleRequest = SaleRequestDTO.builder()
                .items(List.of(itemRequest))
                .paymentMethod(PaymentMethod.CASH)
                .paidAmount(new BigDecimal("300.00")) // 25 * 10 = 250 payable
                .customerName("Amal Perera")
                .build();

        when(userRepository.findByUsername("cashier_kamal")).thenReturn(Optional.of(cashierUser));
        when(productCatalogRepository.findById("DRG-PARA-500")).thenReturn(Optional.of(otcDrug));
        when(batchRepository.findAvailableBatchesForDeductionWithLock(eq("DRG-PARA-500"), any(LocalDate.class)))
                .thenReturn(List.of(batch1, batch2));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponseDTO response = saleService.processCheckout(saleRequest, "cashier_kamal");

        assertThat(response).isNotNull();
        assertThat(response.getNetTotal()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(response.getBalanceAmount()).isEqualByComparingTo(new BigDecimal("50.00")); // 300 - 250 = 50
        assertThat(response.getStatus()).isEqualTo(SaleStatus.COMPLETED);

        // Verify Batch 1 was fully depleted (15 - 15 = 0)
        assertThat(batch1.getStockQty()).isEqualTo(0);
        // Verify remaining 10 was deducted from Batch 2 (50 - 10 = 40)
        assertThat(batch2.getStockQty()).isEqualTo(40);

        verify(batchRepository).save(batch1);
        verify(batchRepository).save(batch2);
        verify(saleRepository).save(any(Sale.class));
    }

    @Test
    @DisplayName("Insufficient Stock: Should throw exception when requested qty exceeds total non-expired stock")
    void processCheckout_InsufficientStock_ThrowsException() {
        SaleItemRequestDTO itemRequest = SaleItemRequestDTO.builder()
                .drugCode("DRG-PARA-500")
                .quantity(100) // Total available is only 15 + 50 = 65
                .build();

        SaleRequestDTO saleRequest = SaleRequestDTO.builder()
                .items(List.of(itemRequest))
                .paymentMethod(PaymentMethod.CASH)
                .paidAmount(new BigDecimal("1000.00"))
                .build();

        when(userRepository.findByUsername("cashier_kamal")).thenReturn(Optional.of(cashierUser));
        when(productCatalogRepository.findById("DRG-PARA-500")).thenReturn(Optional.of(otcDrug));
        when(batchRepository.findAvailableBatchesForDeductionWithLock(eq("DRG-PARA-500"), any(LocalDate.class)))
                .thenReturn(List.of(batch1, batch2));

        assertThatThrownBy(() -> saleService.processCheckout(saleRequest, "cashier_kamal"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");

        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    @DisplayName("POM Enforcement: Should hard-block checkout for PRESCRIPTION_ONLY drug when prescription is missing")
    void processCheckout_PomWithoutPrescription_ThrowsException() {
        SaleItemRequestDTO itemRequest = SaleItemRequestDTO.builder()
                .drugCode("DRG-AMOX-500")
                .quantity(10)
                .build();

        SaleRequestDTO saleRequest = SaleRequestDTO.builder()
                .items(List.of(itemRequest))
                .paymentMethod(PaymentMethod.CASH)
                .paidAmount(new BigDecimal("500.00"))
                .prescriptionNumber(null) // No prescription provided
                .build();

        when(userRepository.findByUsername("cashier_kamal")).thenReturn(Optional.of(cashierUser));
        when(productCatalogRepository.findById("DRG-AMOX-500")).thenReturn(Optional.of(pomDrug));

        assertThatThrownBy(() -> saleService.processCheckout(saleRequest, "cashier_kamal"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Prescription is strictly required");

        verify(batchRepository, never()).save(any(MedicineBatch.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    @DisplayName("POM Compliance: Should allow checkout when prescription is APPROVED and auto-mark as DISPENSED")
    void processCheckout_PomWithApprovedPrescription_Success() {
        MedicineBatch amoxBatch = MedicineBatch.builder()
                .id(201L)
                .productCatalog(pomDrug)
                .batchNumber("BAT-AMOX-01")
                .stockQty(50)
                .retailPrice(new BigDecimal("20.00"))
                .unitCost(new BigDecimal("12.00"))
                .expDate(LocalDate.now().plusYears(1))
                .build();

        SaleItemRequestDTO itemRequest = SaleItemRequestDTO.builder()
                .drugCode("DRG-AMOX-500")
                .quantity(10)
                .build();

        SaleRequestDTO saleRequest = SaleRequestDTO.builder()
                .items(List.of(itemRequest))
                .paymentMethod(PaymentMethod.CARD)
                .paidAmount(new BigDecimal("200.00"))
                .prescriptionNumber("RX-2026-VAL01")
                .build();

        when(userRepository.findByUsername("cashier_kamal")).thenReturn(Optional.of(cashierUser));
        when(prescriptionRepository.findByPrescriptionNumber("RX-2026-VAL01"))
                .thenReturn(Optional.of(approvedPrescription));
        when(productCatalogRepository.findById("DRG-AMOX-500")).thenReturn(Optional.of(pomDrug));
        when(batchRepository.findAvailableBatchesForDeductionWithLock(eq("DRG-AMOX-500"), any(LocalDate.class)))
                .thenReturn(List.of(amoxBatch));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponseDTO response = saleService.processCheckout(saleRequest, "cashier_kamal");

        assertThat(response).isNotNull();
        assertThat(response.getPrescriptionNumber()).isEqualTo("RX-2026-VAL01");

        // Verify prescription status updated to DISPENSED
        assertThat(approvedPrescription.getStatus()).isEqualTo(PrescriptionStatus.DISPENSED);
        verify(prescriptionRepository).save(approvedPrescription);
    }

    @Test
    @DisplayName("Paid Amount Check: Should throw exception when paid amount is less than net total")
    void processCheckout_Underpayment_ThrowsException() {
        SaleItemRequestDTO itemRequest = SaleItemRequestDTO.builder()
                .drugCode("DRG-PARA-500")
                .quantity(10) // 10 * 10 = 100 payable
                .build();

        SaleRequestDTO saleRequest = SaleRequestDTO.builder()
                .items(List.of(itemRequest))
                .paymentMethod(PaymentMethod.CASH)
                .paidAmount(new BigDecimal("50.00")) // Underpaid: 50 < 100
                .build();

        when(userRepository.findByUsername("cashier_kamal")).thenReturn(Optional.of(cashierUser));
        when(productCatalogRepository.findById("DRG-PARA-500")).thenReturn(Optional.of(otcDrug));
        when(batchRepository.findAvailableBatchesForDeductionWithLock(eq("DRG-PARA-500"), any(LocalDate.class)))
                .thenReturn(List.of(batch1));

        assertThatThrownBy(() -> saleService.processCheckout(saleRequest, "cashier_kamal"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is less than net total payable");

        verify(saleRepository, never()).save(any(Sale.class));
    }
}