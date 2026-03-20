package com.smartclinic.hms.admin.rule;

public record AdminRulePageLinkResponse(
        int page,
        String url,
        boolean active
) {
}
