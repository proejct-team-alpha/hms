package com.smartclinic.hms.admin.patient.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAdminPatientApiRequest(
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "연락처는 필수입니다.")
        String phone,

        String note
) {
}
