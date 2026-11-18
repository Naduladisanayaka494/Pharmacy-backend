package com.pharmacy.system.service;

import com.pharmacy.system.dto.ReceiptDTO;
import com.pharmacy.system.dto.SaleItemRequestDTO;
import com.pharmacy.system.dto.SaleRequestDTO;
import com.pharmacy.system.dto.SaleResponseDTO;
import com.pharmacy.system.entity.MedicineBatch;
import com.pharmacy.system.entity.Prescription;
import com.pharmacy.system.entity.PrescriptionStatus;
import com.pharmacy.system.entity.ProductCatalog;
import com.pharmacy.system.entity.Sale;
import com.pharmacy.system.entity.SaleItem;
import com.pharmacy.system.entity.SaleStatus;
import com.pharmacy.system.entity.ScheduleType;
import com.pharmacy.system.entity.User;
import com.pharmacy.system.repository.MedicineBatchRepository;
import com.pharmacy.system.repository.PrescriptionRepository;
import com.pharmacy.system.repository.ProductCatalogRepository;
import com.pharmacy.system.repository.SaleRepository;
import com.pharmacy.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductCatalogRepository productCatalogRepository;
    private final MedicineBatchRepository batchRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;

    /**
     * Process a POS Checkout sale transaction.
     * Enforces:
     * 1. Transactional atomicity (all-or-nothing rollback).
     * 2. Clinical POM prescription validation and auto-dispense marking.
     * 3. Pessimistic concurrency locking on batches during stock deduction.
     * 4. FEFO multi-batch stock allocation.
     * 5. Accurate backend financial arithmetic and price freezing.
     */
    @Transactional
    public SaleResponseDTO processCheckout(SaleRequestDTO requestDTO, String cashierUsername) {
        log.info("Processing POS sale checkout initiated by cashier: {}", cashierUsername);

        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new IllegalArgumentException("Cashier user not found: " + cashierUsername));

        // 1. Clinical POM & Prescription Validation
        Prescription prescription = null;
        if (requestDTO.getPrescriptionNumber() != null && !requestDTO.getPrescriptionNumber().trim().isEmpty()) {
            prescription = prescriptionRepository.findByPrescriptionNumber(requestDTO.getPrescriptionNumber().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Prescription not found with number: " + requestDTO.getPrescriptionNumber()));

            if (prescription.getStatus() != PrescriptionStatus.APPROVED) {
                throw new IllegalStateException("Prescription " + requestDTO.getPrescriptionNumber() +
                        " cannot be dispensed. Current status: " + prescription.getStatus());
            }
        }

        // 2. Prepare Sale entity
        String invoiceNumber = generateInvoiceNumber();
        Sale sale = Sale.builder()
                .invoiceNumber(invoiceNumber)
                .cashier(cashier)
                .prescription(prescription)
                .customerName(requestDTO.getCustomerName() != null ? requestDTO.getCustomerName().trim() : null)
                .customerPhone(requestDTO.getCustomerPhone() != null ? requestDTO.getCustomerPhone().trim() : null)
                .paymentMethod(requestDTO.getPaymentMethod())
                .status(SaleStatus.COMPLETED)
                .subtotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .netTotal(BigDecimal.ZERO)
                .paidAmount(requestDTO.getPaidAmount())
                .balanceAmount(BigDecimal.ZERO)
                .build();

        BigDecimal runningSubtotal = BigDecimal.ZERO;
        BigDecimal runningDiscount = BigDecimal.ZERO;

        // 3. Process each requested medicine line item
        for (SaleItemRequestDTO itemRequest : requestDTO.getItems()) {
            ProductCatalog drug = productCatalogRepository.findById(itemRequest.getDrugCode())
                    .orElseThrow(() -> new IllegalArgumentException("Drug not found with code: " + itemRequest.getDrugCode()));

            if (!Boolean.TRUE.equals(drug.getIsActive())) {
                throw new IllegalStateException("Cannot sell inactive medicine: " + drug.getDrugCode());
            }

            // Legal Check: POM / Narcotics require an approved prescription
            if ((drug.getScheduleType() == ScheduleType.PRESCRIPTION_ONLY || drug.getScheduleType() == ScheduleType.NARCOTICS)
                    && prescription == null) {
                throw new IllegalStateException("Prescription is strictly required for scheduled medicine: " +
                        drug.getBrandName() + " (" + drug.getScheduleType() + ")");
            }

            // 4. FEFO Multi-Batch Deduction with Pessimistic Write Lock
            int quantityToDeduct = itemRequest.getQuantity();
            LocalDate today = LocalDate.now();

            List<MedicineBatch> availableBatches = batchRepository.findAvailableBatchesForDeductionWithLock(
                    drug.getDrugCode(), today);

            int totalAvailableStock = availableBatches.stream()
                    .mapToInt(MedicineBatch::getStockQty)
                    .sum();

            if (totalAvailableStock < quantityToDeduct) {
                throw new IllegalArgumentException(String.format(
                        "Insufficient stock for '%s' (%s). Requested: %d, Available non-expired stock: %d",
                        drug.getBrandName(), drug.getDrugCode(), quantityToDeduct, totalAvailableStock
                ));
            }

            // Split deduction across batches in FEFO order
            for (MedicineBatch batch : availableBatches) {
                if (quantityToDeduct <= 0) {
                    break;
                }

                int batchStock = batch.getStockQty();
                int deductFromThisBatch = Math.min(batchStock, quantityToDeduct);

                // Update batch stock in database
                batch.setStockQty(batchStock - deductFromThisBatch);
                batchRepository.save(batch);

                quantityToDeduct -= deductFromThisBatch;

                // Calculate item financials based on frozen batch retail price
                BigDecimal unitPrice = batch.getRetailPrice();
                BigDecimal unitCost = batch.getUnitCost();
                BigDecimal lineItemDiscount = BigDecimal.ZERO;

                // Proportionate discount if specified on the item request
                if (itemRequest.getDiscount() != null && itemRequest.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    lineItemDiscount = itemRequest.getDiscount()
                            .multiply(BigDecimal.valueOf(deductFromThisBatch))
                            .divide(BigDecimal.valueOf(itemRequest.getQuantity()), 2, RoundingMode.HALF_UP);
                }

                BigDecimal lineGrossTotal = unitPrice.multiply(BigDecimal.valueOf(deductFromThisBatch));
                BigDecimal lineSubtotal = lineGrossTotal.subtract(lineItemDiscount);

                SaleItem saleItem = SaleItem.builder()
                        .productCatalog(drug)
                        .medicineBatch(batch)
                        .quantity(deductFromThisBatch)
                        .unitPrice(unitPrice)
                        .unitCost(unitCost)
                        .discount(lineItemDiscount)
                        .subtotal(lineSubtotal)
                        .build();

                sale.addItem(saleItem);

                runningSubtotal = runningSubtotal.add(lineGrossTotal);
                runningDiscount = runningDiscount.add(lineItemDiscount);
            }
        }

        // 5. Finalize Financial Totals
        BigDecimal netTotal = runningSubtotal.subtract(runningDiscount);

        if (requestDTO.getPaidAmount().compareTo(netTotal) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Paid amount (%s) is less than net total payable (%s)", requestDTO.getPaidAmount(), netTotal
            ));
        }

        BigDecimal balanceAmount = requestDTO.getPaidAmount().subtract(netTotal);

        sale.setSubtotal(runningSubtotal);
        sale.setDiscountAmount(runningDiscount);
        sale.setNetTotal(netTotal);
        sale.setBalanceAmount(balanceAmount);

        // 6. Mark Prescription as DISPENSED if applicable
        if (prescription != null) {
            prescription.setStatus(PrescriptionStatus.DISPENSED);
            prescriptionRepository.save(prescription);
            log.info("Prescription '{}' updated to DISPENSED status after sale completion", prescription.getPrescriptionNumber());
        }

        Sale savedSale = saleRepository.save(sale);
        log.info("Sale transaction completed successfully. Invoice: {}, Net Total: {}", savedSale.getInvoiceNumber(), savedSale.getNetTotal());

        return mapToResponseDTO(savedSale);
    }

    /**
     * Retrieve sale details by invoice number.
     */
    @Transactional(readOnly = true)
    public SaleResponseDTO getSaleByInvoiceNumber(String invoiceNumber) {
        Sale sale = saleRepository.findByInvoiceNumberWithDetails(invoiceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceNumber));
        return mapToResponseDTO(sale);
    }

    /**
     * Generate thermal printer / PDF printable receipt.
     */
    @Transactional(readOnly = true)
    public ReceiptDTO getReceiptByInvoiceNumber(String invoiceNumber) {
        Sale sale = saleRepository.findByInvoiceNumberWithDetails(invoiceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceNumber));

        List<ReceiptDTO.ReceiptItemDTO> receiptItems = sale.getItems().stream()
                .map(item -> ReceiptDTO.ReceiptItemDTO.builder()
                        .drugName(item.getProductCatalog().getBrandName())
                        .batchNumber(item.getMedicineBatch().getBatchNumber())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return ReceiptDTO.builder()
                .invoiceNumber(sale.getInvoiceNumber())
                .cashierName(sale.getCashier().getFullName() != null ? sale.getCashier().getFullName() : sale.getCashier().getUsername())
                .prescriptionNumber(sale.getPrescription() != null ? sale.getPrescription().getPrescriptionNumber() : null)
                .customerName(sale.getCustomerName())
                .transactionDate(sale.getCreatedAt())
                .items(receiptItems)
                .subtotal(sale.getSubtotal())
                .discount(sale.getDiscountAmount())
                .tax(sale.getTaxAmount())
                .netTotal(sale.getNetTotal())
                .paidAmount(sale.getPaidAmount())
                .balance(sale.getBalanceAmount())
                .paymentMethod(sale.getPaymentMethod())
                .build();
    }

    /**
     * Retrieve all sales transactions between two date-times.
     */
    @Transactional(readOnly = true)
    public List<SaleResponseDTO> getSalesBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(Year.now().getValue());
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "INV-" + year + "-" + randomSuffix;
    }

    private SaleResponseDTO mapToResponseDTO(Sale sale) {
        List<SaleResponseDTO.SaleItemResponseDTO> itemDTOs = sale.getItems().stream()
                .map(item -> SaleResponseDTO.SaleItemResponseDTO.builder()
                        .id(item.getId())
                        .drugCode(item.getProductCatalog().getDrugCode())
                        .brandName(item.getProductCatalog().getBrandName())
                        .genericName(item.getProductCatalog().getGenericName())
                        .batchNumber(item.getMedicineBatch().getBatchNumber())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .discount(item.getDiscount())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return SaleResponseDTO.builder()
                .id(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .cashierUsername(sale.getCashier().getUsername())
                .prescriptionNumber(sale.getPrescription() != null ? sale.getPrescription().getPrescriptionNumber() : null)
                .customerName(sale.getCustomerName())
                .customerPhone(sale.getCustomerPhone())
                .subtotal(sale.getSubtotal())
                .discountAmount(sale.getDiscountAmount())
                .taxAmount(sale.getTaxAmount())
                .netTotal(sale.getNetTotal())
                .paidAmount(sale.getPaidAmount())
                .balanceAmount(sale.getBalanceAmount())
                .paymentMethod(sale.getPaymentMethod())
                .status(sale.getStatus())
                .items(itemDTOs)
                .createdAt(sale.getCreatedAt())
                .build();
    }
}