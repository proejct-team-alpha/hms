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
import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffApiResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    private static final String STAFF_CREATED_MESSAGE = "?꿔꺂???????嚥싲갭큔?댁쉩???嶺???????";
    private static final String STAFF_UPDATED_MESSAGE = "?꿔꺂??????癲ル슢???ъ쒜筌믡굥夷???쎛 ????볥궚???嶺???????";
    private static final String STAFF_DEACTIVATED_MESSAGE = "?꿔꺂???????????嚥싲갭큔????嶺???????";
    private static final String INPUT_CHECK_MESSAGE = "????怨몄７??醫딆┫???????⑤베鍮??癲ル슢캉????????녿뮝???ル튉??";
    private static final String INVALID_ROLE_MESSAGE = "????ъ군???? ??? ????????뉖뤁??";
    private static final String INVALID_DEPARTMENT_MESSAGE = "????ъ군???? ??? ?꿔꺂????壤쎻뫔?롳쭕?뼿?縕???????딅젩.";
    private static final String DUPLICATE_USERNAME_MESSAGE = "???? ????嚥싳쉶瑗??꾧틡???汝??吏???????썹땟?㈑???됰Ŋ???????딅젩.";
    private static final String DUPLICATE_EMPLOYEE_NUMBER_MESSAGE = "???? ????嚥싳쉶瑗??꾧틡??????????뉖뤁??";
    private static final String STAFF_NOT_FOUND_MESSAGE = "?꿔꺂???????꿔꺂????????????ㅿ폍??????딅젩.";
    private static final String DOCTOR_NOT_FOUND_MESSAGE = "??嶺뚮슣堉???癲ル슢???ъ쒜???꿔꺂????????????ㅿ폍??????딅젩.";
    private static final String PASSWORD_LENGTH_MESSAGE = "?????筌??????8???????壤????ㅿ폎?????嶺뚮ㅎ????";
    private static final String SELF_DEACTIVATE_MESSAGE = "??⑤슢?뽫춯????影??낟??? ?????嚥싲갭큔??????????ㅿ폍??????딅젩.";
    private static final String ALREADY_DEACTIVATED_MESSAGE = "???? ?????嚥싲갭큔?????꿔꺂?????????뉖뤁??";
    private static final String REACTIVATION_NOT_ALLOWED_MESSAGE = "???????꿔꺂?????? ????嚥싲갭큔??????????ㅿ폍??????딅젩.";
    private static final String INACTIVE_STAFF_UPDATE_NOT_ALLOWED_MESSAGE = "\uBE44\uD65C\uC131\uD654\uB41C \uC9C1\uC6D0\uC740 \uC218\uC815\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
    private static final String RETIRED_AT_REQUIRED_MESSAGE = "\uD1F4\uC0AC \uC77C\uC2DC\uB294 \uB0A0\uC9DC\uC640 \uC2DC\uAC04\uC744 \uBAA8\uB450 \uC120\uD0DD\uD574\uC57C \uD569\uB2C8\uB2E4.";
    private static final String INVALID_RETIRED_AT_MESSAGE = "\uD1F4\uC0AC \uC77C\uC2DC \uD615\uC2DD\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.";
    private static final DateTimeFormatter RETIRED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter RETIRED_AT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter RETIRED_AT_HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH");

    private static final Map<StaffRole, String> ROLE_LABELS = Map.of(
            StaffRole.ADMIN, "\uAD00\uB9AC\uC790",
            StaffRole.DOCTOR, "\uC758\uC0AC",
            StaffRole.NURSE, "\uAC04\uD638\uC0AC",
            StaffRole.STAFF, "\uC9C1\uC6D0",
            StaffRole.ITEM_MANAGER, "\uBB3C\uD488 \uB2F4\uB2F9\uC790");

    private static final Map<String, String> AVAILABLE_DAY_LABELS = new LinkedHashMap<>();

    static {
        AVAILABLE_DAY_LABELS.put("MON", "\uC6D4\uC694\uC77C");
        AVAILABLE_DAY_LABELS.put("TUE", "\uD654\uC694\uC77C");
        AVAILABLE_DAY_LABELS.put("WED", "\uC218\uC694\uC77C");
        AVAILABLE_DAY_LABELS.put("THU", "\uBAA9\uC694\uC77C");
        AVAILABLE_DAY_LABELS.put("FRI", "\uAE08\uC694\uC77C");
        AVAILABLE_DAY_LABELS.put("SAT", "\uD1A0\uC694\uC77C");
        AVAILABLE_DAY_LABELS.put("SUN", "\uC77C\uC694\uC77C");
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
        return buildCreateFormResponse("", "", "", DEFAULT_ROLE, null, true, null, "", "", List.of());
    }

    public AdminStaffFormResponse getCreateForm(CreateAdminStaffRequest request) {
        return buildCreateFormResponse(
                request.username(),
                request.name(),
                request.employeeNumber(),
                request.role(),
                request.departmentId(),
                request.active(),
                request.retiredAt(),
                request.retiredAtDate(),
                request.retiredAtHour(),
                request.availableDays());
    }

    public AdminStaffFormResponse getEditForm(Long staffId, String currentUsername) {
        Staff staff = getStaff(staffId);
        Doctor doctor = getDoctorIfNeeded(staff);
        return buildEditFormResponse(staff, doctor, null, currentUsername);
    }

    public AdminStaffFormResponse getEditForm(UpdateAdminStaffRequest request, String currentUsername) {
        Staff staff = getStaff(request.staffId());
        Doctor doctor = getDoctorIfNeeded(staff);
        return buildEditFormResponse(staff, doctor, request, currentUsername);
    }

    public UpdateAdminStaffApiResponse getUpdateApiResponse(Long staffId, String message) {
        Staff staff = getStaff(staffId);
        Doctor doctor = getDoctorIfNeeded(staff);
        Department doctorDepartment = doctor == null ? null : doctor.getDepartment();

        return new UpdateAdminStaffApiResponse(
                staff.getId(),
                staff.getUsername(),
                staff.getEmployeeNumber(),
                staff.getName(),
                staff.getRole().name(),
                doctorDepartment == null ? null : doctorDepartment.getId(),
                doctorDepartment == null ? NO_DEPARTMENT_LABEL : doctorDepartment.getName(),
                staff.isActive(),
                formatRetiredAt(staff.getRetiredAt()),
                doctor == null ? List.of() : splitAvailableDays(doctor.getAvailableDays()),
                message
        );
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
                null);

        LocalDateTime retiredAt = resolveRetiredAt(request.retiredAt(), request.retiredAtDate(), request.retiredAtHour());
        boolean active = resolveActiveState(request.active(), retiredAt);
        staff.update(staff.getName(), null, active);
        staff.updateRetiredAt(retiredAt);

        adminStaffRepository.save(staff);

        if (role == StaffRole.DOCTOR) {
            Doctor doctor = Doctor.create(
                    staff,
                    department,
                    joinAvailableDays(request.availableDays()),
                    resolveDoctorSpecialty(department));
            doctorRepository.save(doctor);
        }

        return STAFF_CREATED_MESSAGE;
    }

    @Transactional
    public String updateStaff(UpdateAdminStaffRequest request) {
        return updateStaff(request, null);
    }

    @Transactional
    public String updateStaff(UpdateAdminStaffRequest request, String currentUsername) {
        Staff staff = getStaff(request.staffId());
        validateInactiveStaffUpdate(staff);
        Department department = resolveDepartment(request.departmentId());
        validateDoctorDepartment(staff.getRole(), department);
        validateSelfAccountUpdate(staff, request, currentUsername);
        validateReactivation(staff, request.active());

        LocalDateTime retiredAt = resolveRetiredAt(request.retiredAt(), request.retiredAtDate(), request.retiredAtHour());
        boolean active = resolveActiveState(request.active(), retiredAt);

        staff.update(request.name().trim(), null, active);
        staff.updateRetiredAt(retiredAt);

        if (hasText(request.password())) {
            validatePassword(request.password());
            staff.updatePassword(passwordEncoder.encode(request.password().trim()));
        }

        if (staff.getRole() == StaffRole.DOCTOR) {
            Doctor doctor = doctorRepository.findByStaffId(staff.getId())
                    .orElseThrow(() -> CustomException.notFound(DOCTOR_NOT_FOUND_MESSAGE));

            String availableDays = joinAvailableDays(request.availableDays());
            String specialty = resolveDoctorSpecialty(department);
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

        staff.update(staff.getName(), null, false);
        return STAFF_DEACTIVATED_MESSAGE;
    }

    @Transactional
    public int deactivateExpiredStaffs() {
        LocalDateTime now = LocalDateTime.now();
        List<Staff> expiredStaffs = adminStaffRepository.findAllByActiveTrueAndRetiredAtLessThanEqual(now);

        expiredStaffs.forEach(staff -> staff.update(staff.getName(), staff.getDepartment(), false));
        return expiredStaffs.size();
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
                ? selfRow ? "\uBCF8\uC778" : ""
                : "\uBE44\uD65C\uC131";

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
                projection.isActive() ? "\uC7AC\uC9C1" : "\uBE44\uD65C\uC131",
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
            LocalDateTime retiredAt,
            String retiredAtDate,
            String retiredAtHour,
            List<String> availableDays) {
        String normalizedRole = normalizeSelectedRole(selectedRole);
        boolean doctorRole = StaffRole.DOCTOR.name().equals(normalizedRole);
        Set<String> selectedDays = normalizeAvailableDaySet(availableDays);
        String retiredAtDateValue = resolveRetiredAtDateForForm(retiredAt, retiredAtDate);
        String retiredAtHourValue = resolveRetiredAtHourForForm(retiredAt, retiredAtHour);

        return new AdminStaffFormResponse(
                "\uC9C1\uC6D0 \uB4F1\uB85D",
                "/admin/staff/create",
                "\uB4F1\uB85D\uD558\uAE30",
                false,
                null,
                nullToEmpty(username),
                nullToEmpty(name),
                nullToEmpty(employeeNumber),
                normalizedRole,
                ROLE_LABELS.getOrDefault(StaffRole.valueOf(normalizedRole), normalizedRole),
                selectedDepartmentId,
                active,
                resolveRetiredAtValueForForm(retiredAt, retiredAtDateValue, retiredAtHourValue),
                retiredAtDateValue,
                retiredAtHourValue,
                false,
                false,
                false,
                false,
                doctorRole,
                buildFormRoleOptions(normalizedRole),
                buildDepartmentOptions(selectedDepartmentId),
                buildEmploymentStatusFormOptions(active),
                buildRetiredAtHourOptions(retiredAtHourValue),
                buildAvailableDayOptions(selectedDays));
    }

    private AdminStaffFormResponse buildEditFormResponse(
            Staff staff,
            Doctor doctor,
            UpdateAdminStaffRequest request,
            String currentUsername) {
        boolean selfEdit = currentUsername != null && staff.getUsername().equals(currentUsername);
        boolean readOnly = !staff.isActive();
        Long persistedDepartmentId = doctor == null || doctor.getDepartment() == null ? null : doctor.getDepartment().getId();
        Long selectedDepartmentId = readOnly || request == null ? persistedDepartmentId : request.departmentId();
        String name = readOnly || request == null ? staff.getName() : request.name();
        boolean employmentStatusLocked = selfEdit || readOnly;
        boolean retiredAtLocked = selfEdit || readOnly;
        boolean active = employmentStatusLocked ? staff.isActive() : request != null ? request.active() : staff.isActive();
        LocalDateTime retiredAtSource = retiredAtLocked
                ? staff.getRetiredAt()
                : request != null ? request.retiredAt() : staff.getRetiredAt();
        String retiredAtDateValue = retiredAtLocked
                ? formatRetiredAtDate(staff.getRetiredAt())
                : request != null
                ? resolveRetiredAtDateForForm(request.retiredAt(), request.retiredAtDate())
                : formatRetiredAtDate(staff.getRetiredAt());
        String retiredAtHourValue = retiredAtLocked
                ? formatRetiredAtHour(staff.getRetiredAt())
                : request != null
                ? resolveRetiredAtHourForForm(request.retiredAt(), request.retiredAtHour())
                : formatRetiredAtHour(staff.getRetiredAt());
        Set<String> selectedDays = readOnly || request == null
                ? doctor == null ? Set.of() : normalizeAvailableDaySet(splitAvailableDays(doctor.getAvailableDays()))
                : normalizeAvailableDaySet(request.availableDays());

        return new AdminStaffFormResponse(
                "\uC9C1\uC6D0 \uC218\uC815",
                "/admin/staff/update",
                "\uC218\uC815\uD558\uAE30",
                true,
                staff.getId(),
                staff.getUsername(),
                nullToEmpty(name),
                staff.getEmployeeNumber(),
                staff.getRole().name(),
                ROLE_LABELS.getOrDefault(staff.getRole(), staff.getRole().name()),
                selectedDepartmentId,
                active,
                resolveRetiredAtValueForForm(retiredAtSource, retiredAtDateValue, retiredAtHourValue),
                retiredAtDateValue,
                retiredAtHourValue,
                selfEdit,
                employmentStatusLocked,
                retiredAtLocked,
                readOnly,
                staff.getRole() == StaffRole.DOCTOR,
                buildFormRoleOptions(staff.getRole().name()),
                buildDepartmentOptions(selectedDepartmentId),
                buildEmploymentStatusFormOptions(active),
                buildRetiredAtHourOptions(retiredAtHourValue),
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
                        ALL.equals(role) ? "\uC804\uCCB4 \uC5ED\uD560" : ROLE_LABELS.get(StaffRole.valueOf(role)),
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
                new AdminStaffFormOptionResponse("true", "\uC7AC\uC9C1", active),
                new AdminStaffFormOptionResponse("false", "\uBE44\uD65C\uC131", !active));
    }

    private List<AdminStaffFormOptionResponse> buildRetiredAtHourOptions(String selectedHour) {
        return IntStream.rangeClosed(0, 23)
                .mapToObj(hour -> "%02d".formatted(hour))
                .map(hour -> new AdminStaffFormOptionResponse(hour, hour + ":00", hour.equals(selectedHour)))
                .toList();
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
                new AdminStaffFilterOptionResponse(ALL, "\uC804\uCCB4 \uC0C1\uD0DC", ALL.equals(selectedEmploymentStatus)),
                new AdminStaffFilterOptionResponse(ACTIVE, "\uC7AC\uC9C1", ACTIVE.equals(selectedEmploymentStatus)),
                new AdminStaffFilterOptionResponse(INACTIVE, "\uBE44\uD65C\uC131", INACTIVE.equals(selectedEmploymentStatus)));
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

    private String resolveDoctorSpecialty(Department department) {
        if (department == null) {
            return null;
        }
        return department.getName();
    }

    private void validateSelfAccountUpdate(Staff staff, UpdateAdminStaffRequest request, String currentUsername) {
        if (!hasText(currentUsername) || !staff.getUsername().equals(currentUsername)) {
            return;
        }

        boolean deactivateAttempt = staff.isActive() && !request.active();
        LocalDateTime resolvedRetiredAt = resolveRetiredAt(request.retiredAt(), request.retiredAtDate(), request.retiredAtHour());
        boolean retiredAtChanged = !Objects.equals(staff.getRetiredAt(), resolvedRetiredAt);

        if (deactivateAttempt || retiredAtChanged) {
            throw CustomException.badRequest("VALIDATION_ERROR", SELF_DEACTIVATE_MESSAGE);
        }
    }

    private void validateReactivation(Staff staff, boolean requestedActive) {
        if (!staff.isActive() && requestedActive) {
            throw CustomException.badRequest("VALIDATION_ERROR", REACTIVATION_NOT_ALLOWED_MESSAGE);
        }
    }

    private void validateInactiveStaffUpdate(Staff staff) {
        if (!staff.isActive()) {
            throw CustomException.badRequest("VALIDATION_ERROR", INACTIVE_STAFF_UPDATE_NOT_ALLOWED_MESSAGE);
        }
    }

    private LocalDateTime resolveRetiredAt(LocalDateTime retiredAt, String retiredAtDate, String retiredAtHour) {
        boolean hasDate = hasText(retiredAtDate);
        boolean hasHour = hasText(retiredAtHour);

        if (hasDate != hasHour) {
            throw CustomException.badRequest("VALIDATION_ERROR", RETIRED_AT_REQUIRED_MESSAGE);
        }

        if (hasDate) {
            try {
                LocalDate date = LocalDate.parse(retiredAtDate.trim(), RETIRED_AT_DATE_FORMATTER);
                int hour = Integer.parseInt(retiredAtHour.trim());
                if (hour < 0 || hour > 23) {
                    throw new IllegalArgumentException();
                }
                return date.atTime(hour, 0);
            } catch (DateTimeParseException | IllegalArgumentException ex) {
                throw CustomException.badRequest("VALIDATION_ERROR", INVALID_RETIRED_AT_MESSAGE);
            }
        }

        return normalizeRetiredAt(retiredAt);
    }

    private LocalDateTime normalizeRetiredAt(LocalDateTime retiredAt) {
        if (retiredAt == null) {
            return null;
        }
        return retiredAt.withMinute(0).withSecond(0).withNano(0);
    }

    private String resolveRetiredAtDateForForm(LocalDateTime retiredAt, String retiredAtDate) {
        if (hasText(retiredAtDate)) {
            return retiredAtDate.trim();
        }
        return formatRetiredAtDate(retiredAt);
    }

    private String resolveRetiredAtHourForForm(LocalDateTime retiredAt, String retiredAtHour) {
        if (hasText(retiredAtHour)) {
            return retiredAtHour.trim();
        }
        return formatRetiredAtHour(retiredAt);
    }

    private String resolveRetiredAtValueForForm(LocalDateTime retiredAt, String retiredAtDate, String retiredAtHour) {
        if (hasText(retiredAtDate) && hasText(retiredAtHour)) {
            return retiredAtDate.trim() + "T" + retiredAtHour.trim() + ":00";
        }
        return formatRetiredAt(retiredAt);
    }

    private boolean shouldDeactivateImmediately(LocalDateTime retiredAt) {
        return retiredAt != null && !retiredAt.isAfter(LocalDateTime.now());
    }

    private boolean resolveActiveState(boolean requestedActive, LocalDateTime retiredAt) {
        if (!requestedActive) {
            return false;
        }
        return !shouldDeactivateImmediately(retiredAt);
    }

    private String formatRetiredAt(LocalDateTime retiredAt) {
        retiredAt = normalizeRetiredAt(retiredAt);
        if (retiredAt == null) {
            return "";
        }
        return retiredAt.format(RETIRED_AT_FORMATTER);
    }

    private String formatRetiredAtDate(LocalDateTime retiredAt) {
        retiredAt = normalizeRetiredAt(retiredAt);
        if (retiredAt == null) {
            return "";
        }
        return retiredAt.format(RETIRED_AT_DATE_FORMATTER);
    }

    private String formatRetiredAtHour(LocalDateTime retiredAt) {
        retiredAt = normalizeRetiredAt(retiredAt);
        if (retiredAt == null) {
            return "";
        }
        return retiredAt.format(RETIRED_AT_HOUR_FORMATTER);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
