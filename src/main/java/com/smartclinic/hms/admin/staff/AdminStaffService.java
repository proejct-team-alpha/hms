package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.AdminStaffFilterOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffItemResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffPageLinkResponse;
import com.smartclinic.hms.domain.StaffRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStaffService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final String ALL = "ALL";
    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";
    private static final String NO_DEPARTMENT_LABEL = "-";

    private static final Map<StaffRole, String> ROLE_LABELS = Map.of(
            StaffRole.ADMIN, "관리자",
            StaffRole.DOCTOR, "의사",
            StaffRole.NURSE, "간호사",
            StaffRole.STAFF, "접수 직원",
            StaffRole.ITEM_MANAGER, "물품 담당자"
    );

    private final AdminStaffRepository adminStaffRepository;

    public AdminStaffListResponse getStaffList(int page, int size, String keyword, String roleParam, String employmentStatusParam) {
        int safePage = page < 1 ? DEFAULT_PAGE : page;
        int safeSize = size < 1 ? DEFAULT_SIZE : size;

        String normalizedKeyword = normalizeKeyword(keyword);
        StaffRole role = resolveRole(roleParam);
        Boolean active = resolveActive(employmentStatusParam);

        String selectedRole = role == null ? ALL : role.name();
        String selectedEmploymentStatus = active == null ? ALL : active ? ACTIVE : INACTIVE;

        Pageable pageable = PageRequest.of(
                safePage - 1,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<AdminStaffRepository.AdminStaffListProjection> pageResult = adminStaffRepository.findStaffListPage(
                normalizedKeyword,
                role,
                active,
                pageable
        );

        int currentPage = pageResult.getNumber() + 1;
        int totalPages = pageResult.getTotalPages();
        boolean hasPrevious = pageResult.hasPrevious();
        boolean hasNext = pageResult.hasNext();

        return new AdminStaffListResponse(
                pageResult.getContent().stream()
                        .map(this::toItemResponse)
                        .toList(),
                buildRoleOptions(selectedRole),
                buildEmploymentStatusOptions(selectedEmploymentStatus),
                buildPageLinks(totalPages, currentPage, safeSize, normalizedKeyword, selectedRole, selectedEmploymentStatus),
                normalizedKeyword == null ? "" : normalizedKeyword,
                selectedRole,
                selectedEmploymentStatus,
                pageResult.getTotalElements(),
                currentPage,
                safeSize,
                totalPages,
                hasPrevious,
                hasNext,
                hasPrevious ? buildListUrl(currentPage - 1, safeSize, normalizedKeyword, selectedRole, selectedEmploymentStatus) : "",
                hasNext ? buildListUrl(currentPage + 1, safeSize, normalizedKeyword, selectedRole, selectedEmploymentStatus) : ""
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private StaffRole resolveRole(String roleParam) {
        if (roleParam == null || roleParam.isBlank()) {
            return null;
        }

        String normalized = roleParam.trim().toUpperCase(Locale.ROOT);
        if (ALL.equals(normalized)) {
            return null;
        }

        try {
            return StaffRole.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Boolean resolveActive(String employmentStatusParam) {
        if (employmentStatusParam == null || employmentStatusParam.isBlank()) {
            return null;
        }

        String normalized = employmentStatusParam.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case ALL -> null;
            case ACTIVE -> true;
            case INACTIVE -> false;
            default -> null;
        };
    }

    private AdminStaffItemResponse toItemResponse(AdminStaffRepository.AdminStaffListProjection projection) {
        return new AdminStaffItemResponse(
                projection.getId(),
                projection.getName(),
                projection.getUsername(),
                projection.getEmployeeNumber(),
                projection.getRole().name(),
                ROLE_LABELS.getOrDefault(projection.getRole(), projection.getRole().name()),
                getRoleBadgeClass(projection.getRole()),
                projection.getDepartmentName() == null ? NO_DEPARTMENT_LABEL : projection.getDepartmentName(),
                projection.isActive(),
                projection.isActive() ? "재직" : "비활성",
                projection.isActive() ? "bg-green-100 text-green-800" : "bg-slate-100 text-slate-600"
        );
    }

    private String getRoleBadgeClass(StaffRole role) {
        return switch (role) {
            case ADMIN -> "bg-red-100 text-red-800";
            case DOCTOR -> "bg-blue-100 text-blue-800";
            case NURSE -> "bg-green-100 text-green-800";
            case STAFF -> "bg-purple-100 text-purple-800";
            case ITEM_MANAGER -> "bg-amber-100 text-amber-800";
        };
    }

    private List<AdminStaffFilterOptionResponse> buildRoleOptions(String selectedRole) {
        List<String> orderedRoles = List.of(
                ALL,
                StaffRole.ADMIN.name(),
                StaffRole.DOCTOR.name(),
                StaffRole.NURSE.name(),
                StaffRole.STAFF.name(),
                StaffRole.ITEM_MANAGER.name()
        );

        return orderedRoles.stream()
                .map(role -> new AdminStaffFilterOptionResponse(
                        role,
                        ALL.equals(role) ? "전체 역할" : ROLE_LABELS.get(StaffRole.valueOf(role)),
                        role.equals(selectedRole)))
                .toList();
    }

    private List<AdminStaffFilterOptionResponse> buildEmploymentStatusOptions(String selectedEmploymentStatus) {
        return List.of(
                        new AdminStaffFilterOptionResponse(ALL, "전체 상태", ALL.equals(selectedEmploymentStatus)),
                        new AdminStaffFilterOptionResponse(ACTIVE, "재직", ACTIVE.equals(selectedEmploymentStatus)),
                        new AdminStaffFilterOptionResponse(INACTIVE, "비활성", INACTIVE.equals(selectedEmploymentStatus))
                )
                .stream()
                .toList();
    }

    private List<AdminStaffPageLinkResponse> buildPageLinks(
            int totalPages,
            int currentPage,
            int size,
            String keyword,
            String selectedRole,
            String selectedEmploymentStatus
    ) {
        return IntStream.rangeClosed(1, totalPages)
                .mapToObj(page -> new AdminStaffPageLinkResponse(
                        page,
                        buildListUrl(page, size, keyword, selectedRole, selectedEmploymentStatus),
                        page == currentPage))
                .toList();
    }

    private String buildListUrl(int page, int size, String keyword, String role, String employmentStatus) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/admin/staff/list")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("role", role)
                .queryParam("employmentStatus", employmentStatus);

        if (keyword != null && !keyword.isBlank()) {
            builder.queryParam("keyword", keyword);
        }

        return builder.build().encode().toUriString();
    }
}
