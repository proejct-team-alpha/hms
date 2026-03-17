package com.smartclinic.hms.admin.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminStaffRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Size(max = 50, message = "로그인 아이디는 50자 이하로 입력해주세요.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
        String name,

        @NotBlank(message = "사번은 필수입니다.")
        @Size(max = 20, message = "사번은 20자 이하로 입력해주세요.")
        String employeeNumber,

        @NotBlank(message = "역할은 필수입니다.")
        String role,

        Long departmentId,

        boolean active
) {
}
