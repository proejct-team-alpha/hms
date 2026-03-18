package com.smartclinic.hms.domain;

/**
 * 예약 상태 (ERD §4.1)
 */
public enum ReservationStatus {
    RESERVED,
    RECEIVED,
    IN_TREATMENT,
    COMPLETED,
    CANCELLED
}
