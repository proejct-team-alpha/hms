package com.smartclinic.hms.admin.rule.dto;

import com.smartclinic.hms.domain.HospitalRule;
import com.smartclinic.hms.domain.HospitalRuleCategory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AdminRuleDetailResponse(
        Long id,
        String title,
        String content,
        HospitalRuleCategory category,
        String categoryText,
        boolean active,
        String activeText,
        String activeBadgeClass,
        String createdAtText,
        String updatedAtText,
        String updateAction
) {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AdminRuleDetailResponse from(HospitalRule rule) {
        return new AdminRuleDetailResponse(
                rule.getId(),
                rule.getTitle(),
                rule.getContent(),
                rule.getCategory(),
                toCategoryText(rule.getCategory()),
                rule.isActive(),
                rule.isActive() ? "활성" : "비활성",
                rule.isActive()
                        ? "px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-700"
                        : "px-2 py-1 text-xs font-medium rounded-full bg-slate-100 text-slate-500",
                format(rule.getCreatedAt()),
                format(rule.getUpdatedAt()),
                "/admin/rule/update"
        );
    }

    private static String toCategoryText(HospitalRuleCategory category) {
        return switch (category) {
            case EMERGENCY -> "응급";
            case SUPPLY -> "물품";
            case DUTY -> "근무";
            case HYGIENE -> "위생";
            case OTHER -> "기타";
        };
    }

    private static String format(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }
}
