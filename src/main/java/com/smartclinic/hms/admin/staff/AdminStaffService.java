package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.AdminStaffDepartmentOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFilterOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffItemResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffPageLinkResponse;
import com.smartclinic.hms.admin.staff.dto.CreateAdminStaffRequest;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Staff;
import com.smartclinic.hms.domain.StaffRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private static final String DEFAULT_ROLE = "STAFF";
    private static final String NO_DEPARTMENT_LABEL = "-";
    private static final String STAFF_CREATED_MESSAGE = "직원이 등록되었습니다.";

    private static final Map<StaffRole, String> ROLE_LABELS = Map.of(
            StaffRole.ADMIN, "관리자",
            StaffRole.DOCTOR, "의사",
            StaffRole.NURSE, "간호사",
            StaffRole.STAFF, "접수 직원",
            StaffRole.ITEM_MANAGER, "물품 담당자"
    );

    private final AdminStaffRepository adminStaffRepository;
    private final AdminStaffDepartmentRepository adminStaffDepartmentRepository;
    private final PasswordEncoder passwordEncoder;

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

    public AdminStaffFormResponse getCreateForm() {
        return buildCreateFormResponse(
                "",
                "",
                "",
                DEFAULT_ROLE,
                null,
                true
        );
    }

    public AdminStaffFormResponse getCreateForm(CreateAdminStaffRequest request) {
        return buildCreateFormResponse(
                request.username(),
                request.name(),
                request.employeeNumber(),
                request.role(),
                request.departmentId(),
                request.active()
        );
    }

    @Transactional
    public String createStaff(CreateAdminStaffRequest request) {
        validateDuplicateUsername(request.username());
        validateDuplicateEmployeeNumber(request.employeeNumber());

        StaffRole role = resolveRequiredRole(request.role());
        Department department = resolveDepartment(request.departmentId());

        Staff staff = Staff.create(
                request.username().trim(),
                request.employeeNumber().trim(),
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                role,
                department
        );

        if (!request.active()) {
            staff.update(staff.getName(), department, false);
        }

        adminStaffRepository.save(staff);
        return STAFF_CREATED_MESSAGE;
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

    private StaffRole resolveRequiredRole(String roleParam) {
        StaffRole role = resolveRole(roleParam);
        if (role == null) {
            throw CustomException.badRequest("VALIDATION_ERROR", "유효한 역할을 선택해주세요.");
        }
        return role;
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

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }

        return adminStaffDepartmentRepository.findByIdAndActiveTrue(departmentId)
                .orElseThrow(() -> CustomException.badRequest("VALIDATION_ERROR", "유효한 부서를 선택해주세요."));
    }

    private void validateDuplicateUsername(String username) {
        if (adminStaffRepository.existsByUsername(username.trim())) {
            throw CustomException.conflict("DUPLICATE_USERNAME", "이미 사용 중인 로그인 아이디입니다.");
        }
    }

    private void validateDuplicateEmployeeNumber(String employeeNumber) {
        if (adminStaffRepository.existsByEmployeeNumber(employeeNumber.trim())) {
            throw CustomException.conflict("DUPLICATE_EMPLOYEE_NUMBER", "이미 사용 중인 사번입니다.");
        }
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

    private AdminStaffFormResponse buildCreateFormResponse(
            String username,
            String name,
            String employeeNumber,
            String selectedRole,
            Long selectedDepartmentId,
            boolean active
    ) {
        String normalizedRole = (selectedRole == null || selectedRole.isBlank()) ? DEFAULT_ROLE : selectedRole;

        return new AdminStaffFormResponse(
                "직원 등록",
                "/admin/staff/create",
                "등록하기",
                username,
                name,
                employeeNumber,
                normalizedRole,
                selectedDepartmentId,
                active,
                buildFormRoleOptions(normalizedRole),
                buildDepartmentOptions(selectedDepartmentId),
                buildEmploymentStatusFormOptions(active)
        );
    }

    private List<AdminStaffFormOptionResponse> buildFormRoleOptions(String selectedRole) {
        return List.of(
                        StaffRole.ADMIN.name(),
                        StaffRole.DOCTOR.name(),
                        StaffRole.NURSE.name(),
                        StaffRole.STAFF.name(),
                        StaffRole.ITEM_MANAGER.name()
                )
                .stream()
                .map(role -> new AdminStaffFormOptionResponse(
                        role,
                        ROLE_LABELS.get(StaffRole.valueOf(role)),
                        role.equals(selectedRole)))
                .toList();
    }

    private List<AdminStaffDepartmentOptionResponse> buildDepartmentOptions(Long selectedDepartmentId) {
        return adminStaffDepartmentRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(department -> new AdminStaffDepartmentOptionResponse(
                        department.getId(),
                        department.getName(),
                        department.getId().equals(selectedDepartmentId)))
                .toList();
    }

    private List<AdminStaffFormOptionResponse> buildEmploymentStatusFormOptions(boolean active) {
        return List.of(
                        new AdminStaffFormOptionResponse("true", "재직", active),
                        new AdminStaffFormOptionResponse("false", "비활성", !active)
                )
                .stream()
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
