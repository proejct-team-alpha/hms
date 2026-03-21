package com.smartclinic.hms.admin.rule.dto;

import com.smartclinic.hms.domain.HospitalRuleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAdminRuleRequest(
        @NotNull(message = "규칙 ID는 필수입니다.")
        Long ruleId,

        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 200, message = "제목은 200자 이하로 입력해 주세요.")
        String title,

        @NotBlank(message = "내용을 입력해 주세요.")
        @Size(max = 3000, message = "내용은 3000자 이하로 입력해 주세요.")
        String content,

        @NotNull(message = "카테고리를 선택해 주세요.")
        HospitalRuleCategory category,

        Boolean active
) {

    public static UpdateAdminRuleRequest from(AdminRuleDetailResponse detail) {
        return new UpdateAdminRuleRequest(
                detail.id(),
                detail.title(),
                detail.content(),
                detail.category(),
                detail.active()
        );
    }

    public String normalizedTitle() {
        return normalize(title);
    }

    public String normalizedContent() {
        return normalize(content);
    }

    public HospitalRuleCategory normalizedCategory() {
        return category;
    }

    public boolean isActiveChecked() {
        return Boolean.TRUE.equals(active);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
