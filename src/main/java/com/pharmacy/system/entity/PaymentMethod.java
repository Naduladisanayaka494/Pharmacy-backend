package com.pharmacy.system.entity;

public enum PaymentMethod {

    // Cash payment at the counter.

    CASH,


    // Credit / Debit card transaction via POS terminal.

    CARD,


    // Health insurance claim coverage.

    INSURANCE,


    // Split payment (partially cash, partially card).

    SPLIT
}