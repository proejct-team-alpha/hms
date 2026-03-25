package com.smartclinic.hms.admin.patient.dto;

public record AdminPatientPageLinkResponse(
        int page,
        String url,
        boolean active
) {
}
