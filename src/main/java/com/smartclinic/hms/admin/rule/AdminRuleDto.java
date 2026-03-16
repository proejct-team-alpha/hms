package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.domain.HospitalRule;
import com.smartclinic.hms.domain.HospitalRuleCategory;
import lombok.Getter;

@Getter
public class AdminRuleDto {

    private final Long id;
    private final String title;
    private final String content;
    private final String categoryText;
    private final boolean active;
    private final String activeText;
    private final String activeBadgeClass;

    public AdminRuleDto(HospitalRule rule) {
        this.id = rule.getId();
        this.title = rule.getTitle();
        this.content = rule.getContent();
        this.categoryText = toCategoryText(rule.getCategory());
        this.active = rule.isActive();
        this.activeText = rule.isActive() ? "활성" : "비활성";
        this.activeBadgeClass = rule.isActive()
                ? "px-2 py-1 text-xs font-medium rounded-full bg-green-100 text-green-700"
                : "px-2 py-1 text-xs font-medium rounded-full bg-slate-100 text-slate-500";
    }

    private String toCategoryText(HospitalRuleCategory category) {
        return switch (category) {
            case EMERGENCY -> "응급";
            case SUPPLY -> "물품";
            case DUTY -> "근무";
            case HYGIENE -> "위생";
            case OTHER -> "기타";
        };
    }
}
