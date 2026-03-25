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

    // 기본 시작 페이지 번호
    private static final int DEFAULT_PAGE = 1;
    // 한 페이지에 보여줄 데이터 개수
    private static final int DEFAULT_SIZE = 10;

    // 메시지
    private static final String DEPARTMENT_NOT_FOUND_MESSAGE = "진료과를 찾을 수 없습니다.";
    private static final String DEPARTMENT_NAME_REQUIRED_MESSAGE = "진료과명은 필수입니다.";
    private static final String DUPLICATE_DEPARTMENT_NAME_MESSAGE = "이미 존재하는 진료과명입니다.";
    private static final String DEPARTMENT_UPDATED_MESSAGE = "진료과명이 수정되었습니다.";
    private static final String DEPARTMENT_ACTIVATED_MESSAGE = "진료과가 활성화되었습니다.";
    private static final String DEPARTMENT_DEACTIVATED_MESSAGE = "진료과가 비활성화되었습니다.";
    private static final String ALREADY_ACTIVE_MESSAGE = "이미 활성화된 진료과입니다.";
    private static final String ALREADY_INACTIVE_MESSAGE = "이미 비활성화된 진료과입니다.";

    private final AdminDepartmentRepository adminDepartmentRepository;

    // 목록 화면을 그리는 데 필요한 정보 묶음
    public AdminDepartmentListResponse getDepartmentList(int page, int size) {
        // page가 1보다 작으면 → 기본값(1) 사용
        // 아니면 → 사용자가 준 page 그대로 사용
        int safePage = page < 1 ? DEFAULT_PAGE : page;
        // size가 1보다 작으면 → 기본값(10) 사용
        // 아니면 → 그대로 사용
        int safeSize = size < 1 ? DEFAULT_SIZE : size;
        // 페이지 번호 + 페이지당 데이터 개수 설정
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        // 현재 페이지 + size로 offset 계산해서
        // desc 기준으로 size만큼 가져온다
        Page<Department> pageResult = adminDepartmentRepository.findAllByOrderByIdDesc(pageable);

        // Page에서 조회한 엔티티 리스트를 DTO로 변환 (화면에 필요한 데이터만 전달)
        List<AdminDepartmentItemResponse> departments = pageResult.getContent().stream()
                .map(AdminDepartmentItemResponse::new)
                .collect(Collectors.toList());

        // pring 내부 페이지(0부터)를 → 화면용 페이지(1부터)로 변환
        int currentPage = pageResult.getNumber() + 1;
        // 전체 페이지 수
        int totalPages = pageResult.getTotalPages();
        // 위에 정보들로 스프링이 계산 해줌
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
                // 페이지 가 존재한다면 URL 생성해서 넘겨줌
                hasPrevious ? buildListUrl(currentPage - 1, safeSize) : "",
                hasNext ? buildListUrl(currentPage + 1, safeSize) : "");
    }

    // DB에서 해당 ID의 Department 엔티티 조회
    // 엔티티 → DTO 변환
    public AdminDepartmentDetailResponse getDepartmentDetail(Long departmentId) {
        return AdminDepartmentDetailResponse.from(findDepartment(departmentId));
    }

    // 부서 생성
    @Transactional
    public void createDepartment(String name, boolean active) {
        // 공백 제거
        String normalizedName = normalizeName(name);
        // 공백이면 예외 발생
        validateDepartmentName(normalizedName);

        // 같은 이름(대소문자 무시)이 이미 존재하면 → 중복 예외 발생
        if (adminDepartmentRepository.existsByNameIgnoreCase(normalizedName)) {
            throw CustomException.conflict("DUPLICATE_DEPARTMENT_NAME", DUPLICATE_DEPARTMENT_NAME_MESSAGE);
        }

        adminDepartmentRepository.save(Department.create(normalizedName, active));
    }

    // 부서명 수정
    @Transactional
    public String updateDepartmentName(Long departmentId, String name) {
        String normalizedName = normalizeName(name);
        validateDepartmentName(normalizedName);

        Department department = findDepartment(departmentId);
        // 이 이름을 가진 다른 엔티티가 존재하는가?(자기자신 제외)
        if (adminDepartmentRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, departmentId)) {
            throw CustomException.conflict("DUPLICATE_DEPARTMENT_NAME", DUPLICATE_DEPARTMENT_NAME_MESSAGE);
        }

        department.rename(normalizedName);
        adminDepartmentRepository.save(department);
        return DEPARTMENT_UPDATED_MESSAGE;
    }

    // 부서 비활성화
    @Transactional
    public String deactivateDepartment(Long departmentId) {
        Department department = findDepartment(departmentId);
        // 이미 비활성화 라면 예외 발생
        if (!department.isActive()) {
            throw CustomException.invalidStatusTransition(ALREADY_INACTIVE_MESSAGE);
        }

        // 엔티티 내부에 정의된 비활성화용 메서드 (상태 변경 로직)
        department.deactivate();
        adminDepartmentRepository.save(department);
        return DEPARTMENT_DEACTIVATED_MESSAGE;
    }

    // 부서 활성화
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

    // ID로 부서를 조회하고, 없으면 예외로 즉시 실패시키는 안전한 조회 메서드
    private Department findDepartment(Long departmentId) {
        return adminDepartmentRepository.findById(departmentId)
                .orElseThrow(() -> CustomException.notFound(DEPARTMENT_NOT_FOUND_MESSAGE));
    }

    // 부서명이 비어있으면(공백 포함) 예외 발생
    // 서비스 직접 호출 방어용
    private void validateDepartmentName(String normalizedName) {
        if (normalizedName.isBlank()) {
            throw CustomException.badRequest("VALIDATION_ERROR", DEPARTMENT_NAME_REQUIRED_MESSAGE);
        }
    }

    // null이면 빈 문자열("")로 바꾸고, 아니면 앞뒤 공백 제거
    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    // 전체 페이지 수와 현재 페이지를 기준으로
    // 페이지 번호(1 ~ totalPages) 링크 목록 생성
    private List<AdminDepartmentPageLinkResponse> buildPageLinks(int totalPages, int currentPage, int size) {
        // 페이지가 하나도 없으면 빈 리스트 반환
        if (totalPages < 1) {
            return List.of();
        }

        // 1부터 totalPages까지 숫자 생성
        return IntStream.rangeClosed(1, totalPages)
                .mapToObj(page -> new AdminDepartmentPageLinkResponse(
                        page, // 페이지 번호호
                        buildListUrl(page, size), // 이동 url
                        page == currentPage)) // 현재 페이지인가?
                .toList();
    }

    // url 작성하는 메서드
    private String buildListUrl(int page, int size) {
        return "/admin/department/list?page=" + page + "&size=" + size;
    }
}
