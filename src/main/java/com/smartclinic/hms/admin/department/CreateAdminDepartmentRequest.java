package com.smartclinic.hms.admin.department;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateAdminDepartmentRequest {

    @NotBlank(message = "\uC9C4\uB8CC\uACFC\uBA85\uC740 \uD544\uC218\uC785\uB2C8\uB2E4.")
    private String name;

    private boolean active;
}