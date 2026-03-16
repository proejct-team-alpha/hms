package com.smartclinic.hms.nurse.dto;

import lombok.Getter;

@Getter
public class NursePageLinkDto {

    private final int num;
    private final boolean current;
    private final String url;

    public NursePageLinkDto(int num, boolean current, String url) {
        this.num = num;
        this.current = current;
        this.url = url;
    }
}
