package com.smartclinic.hms.admin.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateAdminStaffRequest(
        @NotNull(message = "직원 ID는 필수입니다.")
        Long staffId,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
        String name,

        @NotNull(message = "부서는 필수입니다.")
        Long departmentId,

        @Size(max = 100, message = "비밀번호는 100자 이하로 입력해주세요.")
        String password,

        @Size(max = 100, message = "전문 분야는 100자 이하로 입력해주세요.")
        String specialty,

        List<String> availableDays
) {
}