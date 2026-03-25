package com.smartclinic.hms.item.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ItemCategoryFilter {
    private final String label;
    private final String value;
    private final boolean selected;
    private final String url;
}
