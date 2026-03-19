package com.smartclinic.hms.admin.rule;

import java.util.List;

public record AdminRuleListResponse(
        List<AdminRuleDto> rules,
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
