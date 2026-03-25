package com.smartclinic.hms.admin.patient;

import com.smartclinic.hms.domain.Patient;
import java.time.format.DateTimeFormatter;

public record AdminPatientSummary(
        Long patientId,
        String name,
        String phone,
        String registeredDate,
        String detailUrl
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static AdminPatientSummary from(Patient patient) {
        return new AdminPatientSummary(
                patient.getId(),
                patient.getName(),
                patient.getPhone(),
                patient.getCreatedAt() == null ? "" : patient.getCreatedAt().toLocalDate().format(DATE_FORMATTER),
                "/admin/patient/detail?patientId=" + patient.getId()
        );
    }
}
