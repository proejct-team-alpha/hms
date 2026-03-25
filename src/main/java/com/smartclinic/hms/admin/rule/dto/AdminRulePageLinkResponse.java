package com.smartclinic.hms.admin.rule.dto;

public record AdminRulePageLinkResponse(
        int page,
        String url,
        boolean active
) {
}
