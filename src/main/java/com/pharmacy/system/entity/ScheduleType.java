package com.pharmacy.system.entity;

public enum ScheduleType {

    OVER_THE_COUNTER, // Any customer can purchase without prescription (ex-Paracetamol)
    PRESCRIPTION_ONLY, // Requires a verified doctor's prescription (ex-Antibiotics)
    NARCOTICS // Controlled substances requiring strict compliance and audit logs

}
