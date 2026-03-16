package com.smartclinic.hms.doctor.treatment.dto;

import lombok.Getter;

@Getter
public class DoctorPageLinkDto {

    private final int num;
    private final boolean current;
    private final String url;

    public DoctorPageLinkDto(int num, boolean current, String url) {
        this.num = num;
        this.current = current;
        this.url = url;
    }
}
