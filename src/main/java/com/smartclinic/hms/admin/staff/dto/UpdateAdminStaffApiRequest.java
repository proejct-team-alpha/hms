package com.smartclinic.hms.admin.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateAdminStaffApiRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하로 입력해 주세요.")
        String name,

        Long departmentId,

        @Size(max = 100, message = "비밀번호는 100자 이하로 입력해 주세요.")
        String password,

        boolean active,

        List<String> availableDays
) {
}