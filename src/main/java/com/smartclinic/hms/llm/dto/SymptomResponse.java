package com.smartclinic.hms.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SymptomResponse {
    private Long departmentId;
    private String departmentName;
}
