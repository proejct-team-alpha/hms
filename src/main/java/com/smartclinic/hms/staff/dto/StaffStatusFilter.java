package com.smartclinic.hms.staff.dto;

import lombok.Getter;

@Getter
public class StaffStatusFilter {

    private final String label;
    private final String value;
    private final String href;
    private final String cssClass;

    public StaffStatusFilter(String label, String value, String selected, String date) {
        this.label = label;
        this.value = value;
        this.href = "/staff/reception/list?date=" + date + "&status=" + value;
        boolean isSelected = value.equals(selected == null ? "" : selected);
        this.cssClass = isSelected
                ? switch (value) {
                    case "RESERVED" -> "px-4 py-1.5 rounded-full text-sm font-medium bg-indigo-600 text-white";
                    case "RECEIVED" -> "px-4 py-1.5 rounded-full text-sm font-medium bg-orange-600 text-white";
                    case "COMPLETED" -> "px-4 py-1.5 rounded-full text-sm font-medium bg-green-600 text-white";
                    default -> "px-4 py-1.5 rounded-full text-sm font-medium bg-slate-800 text-white";
                }
                : "px-4 py-1.5 rounded-full text-sm font-medium bg-white border border-slate-300 text-slate-600 hover:bg-slate-100";
    }
}
