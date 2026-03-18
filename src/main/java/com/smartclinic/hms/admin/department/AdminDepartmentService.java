package com.smartclinic.hms.admin.department;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.domain.Department;

import lombok.RequiredArgsConstructor;

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

        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Order.asc("name")));
        Page<Department> pageResult = adminDepartmentRepository.findAllByOrderByNameAsc(pageable);

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
                hasPrevious,
                hasNext,
                hasPrevious ? buildListUrl(currentPage - 1, safeSize) : "",
                hasNext ? buildListUrl(currentPage + 1, safeSize) : ""
        );
    }

    @Transactional
    public void createDepartment(String name) {
        adminDepartmentRepository.save(Department.create(name, true));
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