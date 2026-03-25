package com.smartclinic.hms.admin.patient.dto;

public record UpdateAdminPatientApiResponse(
        Long patientId,
        String name,
        String phone,
        String note,
        String message
) {
}
