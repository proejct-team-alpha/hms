package com.smartclinic.hms.admin.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateAdminDepartmentRequest {

    @NotNull(message = "진료과 ID는 필수입니다.")
    private Long departmentId;

    @NotBlank(message = "진료과명은 필수입니다.")
    private String name;
}
