package com.pharmacy.system.entity;

public enum SaleStatus {

    // Sale transaction successfully completed, stock deducted, and invoice generated.

    COMPLETED,


    // Sale cancelled or fully refunded (stock restored to inventory).

    REFUNDED
}