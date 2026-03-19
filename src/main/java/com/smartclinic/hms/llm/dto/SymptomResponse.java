package com.smartclinic.hms.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SymptomResponse {
    private String dept;
    private String doctor;
    private String time;
}
