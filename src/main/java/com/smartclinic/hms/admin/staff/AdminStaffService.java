package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.department.AdminDepartmentRepository;
import com.smartclinic.hms.admin.staff.dto.AdminStaffDepartmentOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFilterOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormOptionResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffFormResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffItemResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import com.smartclinic.hms.admin.staff.dto.AdminStaffPageLinkResponse;
import com.smartclinic.hms.admin.staff.dto.CreateAdminStaffRequest;
import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffRequest;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private static final String STAFF_CREATED_MESSAGE = "직원을 등록했습니다.";
    private static final String STAFF_UPDATED_MESSAGE = "직원 정보를 수정했습니다.";
    private static final String STAFF_DEACTIVATED_MESSAGE = "직원을 비활성화했습니다.";
    private static final String INPUT_CHECK_MESSAGE = "입력값을 확인해주세요.";
    private static final String INVALID_ROLE_MESSAGE = "유효한 역할을 선택해주세요.";
    private static final String INVALID_DEPARTMENT_MESSAGE = "유효한 부서를 선택해주세요.";
    private static final String DUPLICATE_USERNAME_MESSAGE = "이미 사용 중인 로그인 아이디입니다.";
    private static final String DUPLICATE_EMPLOYEE_NUMBER_MESSAGE = "이미 사용 중인 사번입니다.";
    private static final String STAFF_NOT_FOUND_MESSAGE = "직원을 찾을 수 없습니다.";
    private static final String DOCTOR_NOT_FOUND_MESSAGE = "의사 상세 정보를 찾을 수 없습니다.";
    private static final String PASSWORD_LENGTH_MESSAGE = "비밀번호는 8자 이상이어야 합니다.";
    private static final String SELF_DEACTIVATE_MESSAGE = "본인 계정은 비활성화할 수 없습니다.";
    private static final String ALREADY_DEACTIVATED_MESSAGE = "이미 비활성화된 직원입니다.";

    private static final Map<StaffRole, String> ROLE_LABELS = Map.of(
            StaffRole.ADMIN, "관리자",
            StaffRole.DOCTOR, "의사",
            StaffRole.NURSE, "간호사",
            StaffRole.STAFF, "직원",
            StaffRole.ITEM_MANAGER, "물품 담당자");

    private static final Map<String, String> AVAILABLE_DAY_LABELS = new LinkedHashMap<>();

    static {
        AVAILABLE_DAY_LABELS.put("MON", "월요일");
        AVAILABLE_DAY_LABELS.put("TUE", "화요일");
        AVAILABLE_DAY_LABELS.put("WED", "수요일");
        AVAILABLE_DAY_LABELS.put("THU", "목요일");
        AVAILABLE_DAY_LABELS.put("FRI", "금요일");
        AVAILABLE_DAY_LABELS.put("SAT", "토요일");
        AVAILABLE_DAY_LABELS.put("SUN", "일요일");
    }

    private final AdminStaffRepository adminStaffRepository;
    private final AdminDepartmentRepository adminDepartmentRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminStaffListResponse getStaffList(
            int page,
            int size,
            String keyword,
            String roleParam,
            String employmentStatusParam,
            String currentUsername) {
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
                pageable);

        int currentPage = pageResult.getNumber() + 1;
        int totalPages = pageResult.getTotalPages();
        boolean hasPrevious = pageResult.hasPrevious();
        boolean hasNext = pageResult.hasNext();

        return new AdminStaffListResponse(
                pageResult.getContent().stream()
                        .map(projection -> toItemResponse(projection, currentUsername))
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
                hasNext ? buildListUrl(currentPage + 1, safeSize, normalizedKeyword, selectedRole, selectedEmploymentStatus) : "");
    }

    public AdminStaffFormResponse getCreateForm() {
        return buildCreateFormResponse("", "", "", DEFAULT_ROLE, null, true, "", List.of());
    }

    public AdminStaffFormResponse getCreateForm(CreateAdminStaffRequest request) {
        return buildCreateFormResponse(
                request.username(),
                request.name(),
                request.employeeNumber(),
                request.role(),
                request.departmentId(),
                request.active(),
                request.specialty(),
                request.availableDays());
    }

    public AdminStaffFormResponse getEditForm(Long staffId) {
        Staff staff = getStaff(staffId);
        Doctor doctor = getDoctorIfNeeded(staff);
        return buildEditFormResponse(staff, doctor, null);
    }

    public AdminStaffFormResponse getEditForm(UpdateAdminStaffRequest request) {
        Staff staff = getStaff(request.staffId());
        Doctor doctor = getDoctorIfNeeded(staff);
        return buildEditFormResponse(staff, doctor, request);
    }

    @Transactional
    public String createStaff(CreateAdminStaffRequest request) {
        validateDuplicateUsername(request.username());
        validateDuplicateEmployeeNumber(request.employeeNumber());
        validatePassword(request.password());

        StaffRole role = resolveRequiredRole(request.role());
        Department department = resolveDepartment(request.departmentId());
        validateDoctorDepartment(role, department);

        Staff staff = Staff.create(
                request.username().trim(),
                request.employeeNumber().trim(),
                passwordEncoder.encode(request.password().trim()),
                request.name().trim(),
                role,
                department);

        if (!request.active()) {
            staff.update(staff.getName(), department, false);
        }

        adminStaffRepository.save(staff);

        if (role == StaffRole.DOCTOR) {
            Doctor doctor = Doctor.create(
                    staff,
                    department,
                    joinAvailableDays(request.availableDays()),
                    normalizeNullableText(request.specialty()));
            doctorRepository.save(doctor);
        }

        return STAFF_CREATED_MESSAGE;
    }

    @Transactional
    public String updateStaff(UpdateAdminStaffRequest request) {
        Staff staff = getStaff(request.staffId());
        Department department = resolveDepartment(request.departmentId());
        validateDoctorDepartment(staff.getRole(), department);

        staff.update(request.name().trim(), department, staff.isActive());

        if (hasText(request.password())) {
            validatePassword(request.password());
            staff.updatePassword(passwordEncoder.encode(request.password().trim()));
        }

        if (staff.getRole() == StaffRole.DOCTOR) {
            Doctor doctor = doctorRepository.findByStaffId(staff.getId())
                    .orElseThrow(() -> CustomException.notFound(DOCTOR_NOT_FOUND_MESSAGE));

            String availableDays = joinAvailableDays(request.availableDays());
            String specialty = normalizeNullableText(request.specialty());
            doctor.updateProfile(department, availableDays, specialty);
        }

        return STAFF_UPDATED_MESSAGE;
    }

    @Transactional
    public String deactivateStaff(Long staffId, String currentUsername) {
        Staff staff = getStaff(staffId);

        if (staff.getUsername().equals(currentUsername)) {
            throw CustomException.badRequest("VALIDATION_ERROR", SELF_DEACTIVATE_MESSAGE);
        }

        if (!staff.isActive()) {
            throw CustomException.badRequest("VALIDATION_ERROR", ALREADY_DEACTIVATED_MESSAGE);
        }

        staff.update(staff.getName(), staff.getDepartment(), false);
        return STAFF_DEACTIVATED_MESSAGE;
    }

    public String getInputCheckMessage() {
        return INPUT_CHECK_MESSAGE;
    }

    private AdminStaffItemResponse toItemResponse(
            AdminStaffRepository.AdminStaffListProjection projection,
            String currentUsername) {
        boolean selfRow = projection.getUsername().equals(currentUsername);
        boolean deactivatable = projection.isActive() && !selfRow;
        String deactivateStatusLabel = projection.isActive()
                ? selfRow ? "본인" : ""
                : "비활성";

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
                projection.isActive() ? "bg-green-100 text-green-800" : "bg-slate-100 text-slate-600",
                buildDetailUrl(projection.getId()),
                deactivatable,
                deactivateStatusLabel);
    }

    private AdminStaffFormResponse buildCreateFormResponse(
            String username,
            String name,
            String employeeNumber,
            String selectedRole,
            Long selectedDepartmentId,
            boolean active,
            String specialty,
            List<String> availableDays) {
        String normalizedRole = normalizeSelectedRole(selectedRole);
        boolean doctorRole = StaffRole.DOCTOR.name().equals(normalizedRole);
        Set<String> selectedDays = normalizeAvailableDaySet(availableDays);

        return new AdminStaffFormResponse(
                "직원 등록",
                "/admin/staff/create",
                "등록하기",
                false,
                null,
                nullToEmpty(username),
                nullToEmpty(name),
                nullToEmpty(employeeNumber),
                normalizedRole,
                ROLE_LABELS.getOrDefault(StaffRole.valueOf(normalizedRole), normalizedRole),
                selectedDepartmentId,
                active,
                doctorRole,
                doctorRole ? nullToEmpty(specialty) : "",
                buildFormRoleOptions(normalizedRole),
                buildDepartmentOptions(selectedDepartmentId),
                buildEmploymentStatusFormOptions(active),
                buildAvailableDayOptions(selectedDays));
    }

    private AdminStaffFormResponse buildEditFormResponse(Staff staff, Doctor doctor, UpdateAdminStaffRequest request) {
        Long selectedDepartmentId = request != null ? request.departmentId()
                : staff.getDepartment() == null ? null : staff.getDepartment().getId();
        String name = request != null ? request.name() : staff.getName();
        String specialty = request != null ? nullToEmpty(request.specialty())
                : doctor == null ? "" : nullToEmpty(doctor.getSpecialty());
        Set<String> selectedDays = request != null ? normalizeAvailableDaySet(request.availableDays())
                : doctor == null ? Set.of() : normalizeAvailableDaySet(splitAvailableDays(doctor.getAvailableDays()));

        return new AdminStaffFormResponse(
                "직원 수정",
                "/admin/staff/update",
                "수정하기",
                true,
                staff.getId(),
                staff.getUsername(),
                nullToEmpty(name),
                staff.getEmployeeNumber(),
                staff.getRole().name(),
                ROLE_LABELS.getOrDefault(staff.getRole(), staff.getRole().name()),
                selectedDepartmentId,
                staff.isActive(),
                staff.getRole() == StaffRole.DOCTOR,
                specialty,
                buildFormRoleOptions(staff.getRole().name()),
                buildDepartmentOptions(selectedDepartmentId),
                buildEmploymentStatusFormOptions(staff.isActive()),
                buildAvailableDayOptions(selectedDays));
    }

    private List<AdminStaffFilterOptionResponse> buildRoleOptions(String selectedRole) {
        List<String> orderedRoles = List.of(
                ALL,
                StaffRole.ADMIN.name(),
                StaffRole.DOCTOR.name(),
                StaffRole.NURSE.name(),
                StaffRole.STAFF.name(),
                StaffRole.ITEM_MANAGER.name());

        return orderedRoles.stream()
                .map(role -> new AdminStaffFilterOptionResponse(
                        role,
                        ALL.equals(role) ? "전체 역할" : ROLE_LABELS.get(StaffRole.valueOf(role)),
                        role.equals(selectedRole)))
                .toList();
    }

    private List<AdminStaffFormOptionResponse> buildFormRoleOptions(String selectedRole) {
        return List.of(
                        StaffRole.ADMIN.name(),
                        StaffRole.DOCTOR.name(),
                        StaffRole.NURSE.name(),
                        StaffRole.STAFF.name(),
                        StaffRole.ITEM_MANAGER.name())
                .stream()
                .map(role -> new AdminStaffFormOptionResponse(
                        role,
                        ROLE_LABELS.get(StaffRole.valueOf(role)),
                        role.equals(selectedRole)))
                .toList();
    }

    private List<AdminStaffDepartmentOptionResponse> buildDepartmentOptions(Long selectedDepartmentId) {
        return adminDepartmentRepository.findByActiveTrueOrderByNameAsc()
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
                new AdminStaffFormOptionResponse("false", "비활성", !active));
    }

    private List<AdminStaffFormOptionResponse> buildAvailableDayOptions(Set<String> selectedDays) {
        return AVAILABLE_DAY_LABELS.entrySet().stream()
                .map(entry -> new AdminStaffFormOptionResponse(
                        entry.getKey(),
                        entry.getValue(),
                        selectedDays.contains(entry.getKey())))
                .toList();
    }

    private List<AdminStaffFilterOptionResponse> buildEmploymentStatusOptions(String selectedEmploymentStatus) {
        return List.of(
                new AdminStaffFilterOptionResponse(ALL, "전체 상태", ALL.equals(selectedEmploymentStatus)),
                new AdminStaffFilterOptionResponse(ACTIVE, "재직", ACTIVE.equals(selectedEmploymentStatus)),
                new AdminStaffFilterOptionResponse(INACTIVE, "비활성", INACTIVE.equals(selectedEmploymentStatus)));
    }

    private List<AdminStaffPageLinkResponse> buildPageLinks(
            int totalPages,
            int currentPage,
            int size,
            String keyword,
            String selectedRole,
            String selectedEmploymentStatus) {
        if (totalPages == 0) {
            return List.of();
        }

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

        if (hasText(keyword)) {
            builder.queryParam("keyword", keyword);
        }

        return builder.build().encode().toUriString();
    }

    private String buildDetailUrl(Long staffId) {
        return UriComponentsBuilder
                .fromPath("/admin/staff/detail")
                .queryParam("staffId", staffId)
                .build()
                .encode().toUriString();
    }

    private String normalizeKeyword(String keyword) {
        if (!hasText(keyword)) {
            return null;
        }
        return keyword.trim();
    }

    private StaffRole resolveRole(String roleParam) {
        if (!hasText(roleParam)) {
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
            throw CustomException.badRequest("VALIDATION_ERROR", INVALID_ROLE_MESSAGE);
        }
        return role;
    }

    private Boolean resolveActive(String employmentStatusParam) {
        if (!hasText(employmentStatusParam)) {
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

        return adminDepartmentRepository.findByIdAndActiveTrue(departmentId)
                .orElseThrow(() -> CustomException.badRequest("VALIDATION_ERROR", INVALID_DEPARTMENT_MESSAGE));
    }

    private void validateDoctorDepartment(StaffRole role, Department department) {
        if (role == StaffRole.DOCTOR && department == null) {
            throw CustomException.badRequest("VALIDATION_ERROR", INVALID_DEPARTMENT_MESSAGE);
        }
    }

    private Staff getStaff(Long staffId) {
        return adminStaffRepository.findById(staffId)
                .orElseThrow(() -> CustomException.notFound(STAFF_NOT_FOUND_MESSAGE));
    }

    private Doctor getDoctorIfNeeded(Staff staff) {
        if (staff.getRole() != StaffRole.DOCTOR) {
            return null;
        }

        return doctorRepository.findByStaffId(staff.getId())
                .orElseThrow(() -> CustomException.notFound(DOCTOR_NOT_FOUND_MESSAGE));
    }

    private void validateDuplicateUsername(String username) {
        if (adminStaffRepository.existsByUsername(username.trim())) {
            throw CustomException.conflict("DUPLICATE_USERNAME", DUPLICATE_USERNAME_MESSAGE);
        }
    }

    private void validateDuplicateEmployeeNumber(String employeeNumber) {
        if (adminStaffRepository.existsByEmployeeNumber(employeeNumber.trim())) {
            throw CustomException.conflict("DUPLICATE_EMPLOYEE_NUMBER", DUPLICATE_EMPLOYEE_NUMBER_MESSAGE);
        }
    }

    private void validatePassword(String password) {
        if (password.trim().length() < 8) {
            throw CustomException.badRequest("VALIDATION_ERROR", PASSWORD_LENGTH_MESSAGE);
        }
    }

    private String joinAvailableDays(List<String> availableDays) {
        Set<String> normalizedDays = normalizeAvailableDaySet(availableDays);
        if (normalizedDays.isEmpty()) {
            return null;
        }

        return AVAILABLE_DAY_LABELS.keySet().stream()
                .filter(normalizedDays::contains)
                .collect(Collectors.joining(","));
    }

    private Set<String> normalizeAvailableDaySet(List<String> availableDays) {
        if (availableDays == null) {
            return Set.of();
        }

        return availableDays.stream()
                .filter(this::hasText)
                .map(day -> day.trim().toUpperCase(Locale.ROOT))
                .filter(AVAILABLE_DAY_LABELS::containsKey)
                .collect(Collectors.toSet());
    }

    private List<String> splitAvailableDays(String availableDays) {
        if (!hasText(availableDays)) {
            return List.of();
        }

        return List.of(availableDays.split(","));
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

    private String normalizeSelectedRole(String selectedRole) {
        if (!hasText(selectedRole)) {
            return DEFAULT_ROLE;
        }
        return selectedRole.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullableText(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}