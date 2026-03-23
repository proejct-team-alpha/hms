package com.smartclinic.hms.admin.rule.dto;

import java.util.List;

public record AdminRuleListResponse(
        List<AdminRuleItemResponse> rules,
        List<AdminRulePageLinkResponse> pageLinks,
        List<AdminRuleFilterOptionResponse> categoryOptions,
        List<AdminRuleFilterOptionResponse> activeOptions,
        String selectedCategory,
        String selectedActive,
        String keyword,
        long totalCount,
        int currentPage,
        int size,
        int totalPages,
        boolean hasPages,
        boolean hasPrevious,
        boolean hasNext,
        String previousUrl,
        String nextUrl
) {
}
