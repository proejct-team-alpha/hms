package com.smartclinic.hms.admin.rule.dto;

public record AdminRuleFilterOptionResponse(
        String value,
        String label,
        boolean selected
) {
}
