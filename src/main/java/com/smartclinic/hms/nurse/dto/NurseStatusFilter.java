package com.smartclinic.hms.nurse.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NurseStatusFilter {
    private final String label;
    private final String value;
    private final boolean selected;
    private final String url;
}
