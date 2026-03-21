package com.smartclinic.hms.admin.department;

import com.smartclinic.hms.admin.department.dto.AdminDepartmentDetailResponse;
import com.smartclinic.hms.admin.department.dto.AdminDepartmentItemResponse;
import com.smartclinic.hms.admin.department.dto.AdminDepartmentListResponse;
import com.smartclinic.hms.admin.department.dto.AdminDepartmentPageLinkResponse;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Department;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDepartmentService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;

    private static final String DEPARTMENT_NOT_FOUND_MESSAGE = "진료과를 찾을 수 없습니다.";
    private static final String DEPARTMENT_NAME_REQUIRED_MESSAGE = "진료과명은 필수입니다.";
    private static final String DUPLICATE_DEPARTMENT_NAME_MESSAGE = "이미 존재하는 진료과명입니다.";
    private static final String DEPARTMENT_UPDATED_MESSAGE = "진료과명이 수정되었습니다.";
    private static final String DEPARTMENT_ACTIVATED_MESSAGE = "진료과가 활성화되었습니다.";
    private static final String DEPARTMENT_DEACTIVATED_MESSAGE = "진료과가 비활성화되었습니다.";
    private static final String ALREADY_ACTIVE_MESSAGE = "이미 활성화된 진료과입니다.";
    private static final String ALREADY_INACTIVE_MESSAGE = "이미 비활성화된 진료과입니다.";

    private final AdminDepartmentRepository adminDepartmentRepository;

    public AdminDepartmentListResponse getDepartmentList(int page, int size) {
        int safePage = page < 1 ? DEFAULT_PAGE : page;
        int safeSize = size < 1 ? DEFAULT_SIZE : size;

        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        Page<Department> pageResult = adminDepartmentRepository.findAllByOrderByIdDesc(pageable);

        List<AdminDepartmentItemResponse> departments = pageResult.getContent().stream()
                .map(AdminDepartmentItemResponse::new)
                .collect(Collectors.toList());

        int currentPage = pageResult.getNumber() + 1;
        int totalPages = pageResult.getTotalPages();
        boolean hasPrevious = pageResult.hasPrevious();
        boolean hasNext = pageResult.hasNext();

        return new AdminDepartmentListResponse(
                departments,
                buildPageLinks(totalPages, currentPage, safeSize),
                pageResult.getTotalElements(),
                currentPage,
                safeSize,
                totalPages,
                totalPages > 0,
                hasPrevious,
                hasNext,
                hasPrevious ? buildListUrl(currentPage - 1, safeSize) : "",
                hasNext ? buildListUrl(currentPage + 1, safeSize) : ""
        );
    }

    public AdminDepartmentDetailResponse getDepartmentDetail(Long departmentId) {
        return AdminDepartmentDetailResponse.from(findDepartment(departmentId));
    }

    @Transactional
    public void createDepartment(String name, boolean active) {
        String normalizedName = normalizeName(name);
        validateDepartmentName(normalizedName);

        if (adminDepartmentRepository.existsByNameIgnoreCase(normalizedName)) {
            throw CustomException.conflict("DUPLICATE_DEPARTMENT_NAME", DUPLICATE_DEPARTMENT_NAME_MESSAGE);
        }

        adminDepartmentRepository.save(Department.create(normalizedName, active));
    }

    @Transactional
    public String updateDepartmentName(Long departmentId, String name) {
        String normalizedName = normalizeName(name);
        validateDepartmentName(normalizedName);

        Department department = findDepartment(departmentId);
        if (adminDepartmentRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, departmentId)) {
            throw CustomException.conflict("DUPLICATE_DEPARTMENT_NAME", DUPLICATE_DEPARTMENT_NAME_MESSAGE);
        }

        department.rename(normalizedName);
        adminDepartmentRepository.save(department);
        return DEPARTMENT_UPDATED_MESSAGE;
    }

    @Transactional
    public String deactivateDepartment(Long departmentId) {
        Department department = findDepartment(departmentId);
        if (!department.isActive()) {
            throw CustomException.invalidStatusTransition(ALREADY_INACTIVE_MESSAGE);
        }

        department.deactivate();
        adminDepartmentRepository.save(department);
        return DEPARTMENT_DEACTIVATED_MESSAGE;
    }

    @Transactional
    public String activateDepartment(Long departmentId) {
        Department department = findDepartment(departmentId);
        if (department.isActive()) {
            throw CustomException.invalidStatusTransition(ALREADY_ACTIVE_MESSAGE);
        }

        department.activate();
        adminDepartmentRepository.save(department);
        return DEPARTMENT_ACTIVATED_MESSAGE;
    }

    private Department findDepartment(Long departmentId) {
        return adminDepartmentRepository.findById(departmentId)
                .orElseThrow(() -> CustomException.notFound(DEPARTMENT_NOT_FOUND_MESSAGE));
    }

    private void validateDepartmentName(String normalizedName) {
        if (normalizedName.isBlank()) {
            throw CustomException.badRequest("VALIDATION_ERROR", DEPARTMENT_NAME_REQUIRED_MESSAGE);
        }
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private List<AdminDepartmentPageLinkResponse> buildPageLinks(int totalPages, int currentPage, int size) {
        if (totalPages < 1) {
            return List.of();
        }

        return IntStream.rangeClosed(1, totalPages)
                .mapToObj(page -> new AdminDepartmentPageLinkResponse(
                        page,
                        buildListUrl(page, size),
                        page == currentPage
                ))
                .toList();
    }

    private String buildListUrl(int page, int size) {
        return "/admin/department/list?page=" + page + "&size=" + size;
    }
}
