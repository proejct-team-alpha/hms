package com.smartclinic.hms.admin.rule.dto;

public record AdminRuleDeleteResponse(
        Long ruleId,
        String message
) {

    private static final String DEFAULT_MESSAGE = "규칙이 삭제되었습니다.";

    public static AdminRuleDeleteResponse success(Long ruleId) {
        return new AdminRuleDeleteResponse(ruleId, DEFAULT_MESSAGE);
    }
}
