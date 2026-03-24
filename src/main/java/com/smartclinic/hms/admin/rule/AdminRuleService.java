package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.admin.rule.dto.AdminRuleFilterOptionResponse;
import com.smartclinic.hms.admin.rule.dto.AdminRuleDeleteResponse;
import com.smartclinic.hms.admin.rule.dto.AdminRuleDetailResponse;
import com.smartclinic.hms.admin.rule.dto.AdminRuleItemResponse;
import com.smartclinic.hms.admin.rule.dto.AdminRuleListResponse;
import com.smartclinic.hms.admin.rule.dto.AdminRulePageLinkResponse;
import com.smartclinic.hms.admin.rule.dto.CreateAdminRuleRequest;
import com.smartclinic.hms.admin.rule.dto.UpdateAdminRuleRequest;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.HospitalRule;
import com.smartclinic.hms.domain.HospitalRuleCategory;
import com.smartclinic.hms.llm.service.RuleIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRuleService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int PAGE_LINK_WINDOW_SIZE = 5;
    private static final String DEFAULT_CATEGORY = "ALL";
    private static final String DEFAULT_ACTIVE = "ALL";
    private static final String RULE_NOT_FOUND_MESSAGE = "규칙을 찾을 수 없습니다.";
    private static final String RULE_UPDATED_MESSAGE = "규칙이 수정되었습니다.";

    private final HospitalRuleRepository hospitalRuleRepository;
    private final RuleIndexService ruleIndexService;

    public List<AdminRuleItemResponse> getRuleList() {
        return hospitalRuleRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AdminRuleItemResponse::new)
                .collect(Collectors.toList());
    }

    public AdminRuleListResponse getRuleList(int page, int size, String category, String active, String keyword) {
        int safePage = page < 1 ? DEFAULT_PAGE : page;
        int safeSize = size < 1 ? DEFAULT_SIZE : size;
        String normalizedCategory = normalizeCategory(category);
        String normalizedActive = normalizeActive(active);
        String normalizedKeyword = normalizeKeyword(keyword);

        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        Page<HospitalRule> pageResult = hospitalRuleRepository.search(
                toCategoryOrNull(normalizedCategory),
                toActiveOrNull(normalizedActive),
                normalizedKeyword,
                pageable
        );

        int currentPage = pageResult.getNumber() + 1;
        int totalPages = pageResult.getTotalPages();
        boolean hasPrevious = pageResult.hasPrevious();
        boolean hasNext = pageResult.hasNext();

        return new AdminRuleListResponse(
                pageResult.getContent().stream().map(AdminRuleItemResponse::new).toList(),
                buildPageLinks(totalPages, currentPage, safeSize, normalizedCategory, normalizedActive, normalizedKeyword),
                buildCategoryOptions(normalizedCategory),
                buildActiveOptions(normalizedActive),
                normalizedCategory,
                normalizedActive,
                normalizedKeyword,
                pageResult.getTotalElements(),
                currentPage,
                safeSize,
                totalPages,
                totalPages > 0,
                hasPrevious,
                hasNext,
                hasPrevious ? buildListUrl(currentPage - 1, safeSize, normalizedCategory, normalizedActive, normalizedKeyword) : "",
                hasNext ? buildListUrl(currentPage + 1, safeSize, normalizedCategory, normalizedActive, normalizedKeyword) : ""
        );
    }

    public AdminRuleDetailResponse getRuleDetail(Long ruleId) {
        return AdminRuleDetailResponse.from(findRule(ruleId));
    }

    @Transactional
    public String createRule(CreateAdminRuleRequest request) {
        HospitalRule rule = HospitalRule.create(
                request.normalizedTitle(),
                request.normalizedContent(),
                request.normalizedCategory(),
                request.isActiveChecked()
        );

        hospitalRuleRepository.save(rule);
        ruleIndexService.indexRule(rule);
        return "규칙이 등록되었습니다.";
    }

    @Transactional
    public String updateRule(UpdateAdminRuleRequest request) {
        HospitalRule rule = findRule(request.ruleId());
        rule.update(
                request.normalizedTitle(),
                request.normalizedContent(),
                request.normalizedCategory(),
                request.isActiveChecked()
        );
        hospitalRuleRepository.save(rule);
        ruleIndexService.indexRule(rule);
        return RULE_UPDATED_MESSAGE;
    }

    @Transactional
    public AdminRuleDeleteResponse deleteRule(Long ruleId) {
        HospitalRule rule = findRule(ruleId);
        hospitalRuleRepository.delete(rule);
        ruleIndexService.deleteRuleIndex(ruleId);
        return AdminRuleDeleteResponse.success(ruleId);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return DEFAULT_CATEGORY;
        }

        String upper = category.trim().toUpperCase();
        if (DEFAULT_CATEGORY.equals(upper)) {
            return DEFAULT_CATEGORY;
        }

        return switch (upper) {
            case "EMERGENCY", "SUPPLY", "DUTY", "HYGIENE", "OTHER" -> upper;
            default -> DEFAULT_CATEGORY;
        };
    }

    private String normalizeActive(String active) {
        if (active == null || active.isBlank()) {
            return DEFAULT_ACTIVE;
        }

        String upper = active.trim().toUpperCase();
        return switch (upper) {
            case "ACTIVE", "INACTIVE", "ALL" -> upper;
            default -> DEFAULT_ACTIVE;
        };
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private HospitalRuleCategory toCategoryOrNull(String category) {
        if (DEFAULT_CATEGORY.equals(category)) {
            return null;
        }
        return HospitalRuleCategory.valueOf(category);
    }

    private Boolean toActiveOrNull(String active) {
        return switch (active) {
            case "ACTIVE" -> Boolean.TRUE;
            case "INACTIVE" -> Boolean.FALSE;
            default -> null;
        };
    }

    private List<AdminRuleFilterOptionResponse> buildCategoryOptions(String selectedCategory) {
        return List.of(
                new AdminRuleFilterOptionResponse("ALL", "전체", DEFAULT_CATEGORY.equals(selectedCategory)),
                new AdminRuleFilterOptionResponse("EMERGENCY", "응급", "EMERGENCY".equals(selectedCategory)),
                new AdminRuleFilterOptionResponse("SUPPLY", "물품", "SUPPLY".equals(selectedCategory)),
                new AdminRuleFilterOptionResponse("DUTY", "근무", "DUTY".equals(selectedCategory)),
                new AdminRuleFilterOptionResponse("HYGIENE", "위생", "HYGIENE".equals(selectedCategory)),
                new AdminRuleFilterOptionResponse("OTHER", "기타", "OTHER".equals(selectedCategory))
        );
    }

    private List<AdminRuleFilterOptionResponse> buildActiveOptions(String selectedActive) {
        return List.of(
                new AdminRuleFilterOptionResponse("ALL", "전체", DEFAULT_ACTIVE.equals(selectedActive)),
                new AdminRuleFilterOptionResponse("ACTIVE", "활성", "ACTIVE".equals(selectedActive)),
                new AdminRuleFilterOptionResponse("INACTIVE", "비활성", "INACTIVE".equals(selectedActive))
        );
    }

    private List<AdminRulePageLinkResponse> buildPageLinks(
            int totalPages,
            int currentPage,
            int size,
            String category,
            String active,
            String keyword) {
        if (totalPages < 1) {
            return List.of();
        }

        int startPage = Math.max(1, currentPage - PAGE_LINK_WINDOW_SIZE / 2);
        int endPage = Math.min(totalPages, startPage + PAGE_LINK_WINDOW_SIZE - 1);

        if (endPage - startPage + 1 < PAGE_LINK_WINDOW_SIZE) {
            startPage = Math.max(1, endPage - PAGE_LINK_WINDOW_SIZE + 1);
        }

        return IntStream.rangeClosed(startPage, endPage)
                .mapToObj(page -> new AdminRulePageLinkResponse(
                        page,
                        buildListUrl(page, size, category, active, keyword),
                        page == currentPage
                ))
                .toList();
    }

    private String buildListUrl(int page, int size, String category, String active, String keyword) {
        StringBuilder builder = new StringBuilder("/admin/rule/list?page=")
                .append(page)
                .append("&size=")
                .append(size)
                .append("&category=")
                .append(category)
                .append("&active=")
                .append(active);

        if (!keyword.isBlank()) {
            builder.append("&keyword=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        }

        return builder.toString();
    }

    private HospitalRule findRule(Long ruleId) {
        return hospitalRuleRepository.findById(ruleId)
                .orElseThrow(() -> CustomException.notFound(RULE_NOT_FOUND_MESSAGE));
    }
}
