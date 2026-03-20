package com.smartclinic.hms.admin.rule;

public record AdminRuleFilterOptionResponse(
        String value,
        String label,
        boolean selected
) {
}
