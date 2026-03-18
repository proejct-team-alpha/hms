package com.smartclinic.hms.admin.department;

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

    private final AdminDepartmentRepository adminDepartmentRepository;

    public AdminDepartmentListResponse getDepartmentList(int page, int size) {
        int safePage = page < 1 ? DEFAULT_PAGE : page;
        int safeSize = size < 1 ? DEFAULT_SIZE : size;

        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        Page<Department> pageResult = adminDepartmentRepository.findAllByOrderByIdDesc(pageable);

        List<AdminDepartmentDto> departments = pageResult.getContent().stream()
                .map(AdminDepartmentDto::new)
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
        Department department = adminDepartmentRepository.findById(departmentId)
                .orElseThrow(() -> CustomException.notFound("진료과를 찾을 수 없습니다."));

        return AdminDepartmentDetailResponse.from(department);
    }

    @Transactional
    public void createDepartment(String name, boolean active) {
        adminDepartmentRepository.save(Department.create(name, active));
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