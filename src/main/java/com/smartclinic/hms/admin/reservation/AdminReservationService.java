package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationItemResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationListResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationPageLinkResponse;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationStatusOptionResponse;
import com.smartclinic.hms.domain.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReservationService {

        // 페이지네이션
        private static final int DEFAULT_PAGE = 1;
        private static final int DEFAULT_SIZE = 10;

        // 상태값 화면에 보여줄 한글 라벨
        private static final Map<String, String> STATUS_LABELS = Map.of(
                        "ALL", "전체",
                        "RESERVED", "예약",
                        "RECEIVED", "접수",
                        "COMPLETED", "완료",
                        "CANCELLED", "취소");
        // repo 선언
        private final AdminReservationRepository adminReservationRepository;

        // 관리자 예약 화면 조회
        public AdminReservationListResponse getReservationList(int page, int size, String statusParam) {
                // 잘못된 페이지 번호나 사이즈가 들어와도 기본값으로 보정해서 안전하게 페이지네이션 수행하는 코드
                int safePage = page < 1 ? DEFAULT_PAGE : page;
                int safeSize = size < 1 ? DEFAULT_SIZE : size;

                // 상태값
                ReservationStatus status = resolveStatus(statusParam);
                // 선택된 상태값
                String selectedStatus = status == null ? "ALL" : status.name();

                // 페이지네이션
                Pageable pageable = PageRequest.of(
                                safePage - 1,
                                safeSize,
                                Sort.by(Sort.Order.desc("reservationDate"), Sort.Order.desc("timeSlot")));
                // 조회 결과
                Page<AdminReservationRepository.AdminReservationListProjection> pageResult = adminReservationRepository
                                .findReservationListPage(status, pageable);

                // DTO 변환
                List<AdminReservationItemResponse> reservations = pageResult.getContent().stream()
                                .map(item -> toItemResponse(item))
                                .toList();

                // 페이지네이션 정보
                int currentPage = pageResult.getNumber() + 1;
                // 총 페이지 수
                int totalPages = pageResult.getTotalPages();

                // DTO 변환
                List<AdminReservationStatusOptionResponse> statusOptions = buildStatusOptions(selectedStatus, safeSize);
                // DTO 변환
                List<AdminReservationPageLinkResponse> pageLinks = buildPageLinks(totalPages, currentPage, safeSize,
                                selectedStatus);

                // 이전 페이지, 다음 페이지 여부
                boolean hasPrevious = pageResult.hasPrevious();
                boolean hasNext = pageResult.hasNext();

                // 이전 페이지, 다음 페이지 URL
                String previousUrl = hasPrevious
                                ? buildListUrl(currentPage - 1, safeSize, selectedStatus)
                                : "";
                String nextUrl = hasNext
                                ? buildListUrl(currentPage + 1, safeSize, selectedStatus)
                                : "";

                return new AdminReservationListResponse(
                                reservations,
                                statusOptions,
                                pageLinks,
                                selectedStatus,
                                pageResult.getTotalElements(),
                                currentPage,
                                safeSize,
                                totalPages,
                                hasPrevious,
                                hasNext,
                                previousUrl,
                                nextUrl);
        }

        private ReservationStatus resolveStatus(String statusParam) {
                if (statusParam == null || statusParam.isBlank()) {
                        return null;
                }

                String normalized = statusParam.trim().toUpperCase(Locale.ROOT);
                if ("ALL".equals(normalized)) {
                        return null;
                }

                try {
                        return ReservationStatus.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                        return null;
                }
        }

        // DTO 변환
        private AdminReservationItemResponse toItemResponse(
                        AdminReservationRepository.AdminReservationListProjection row) {
                String status = row.getStatus().name();

                return new AdminReservationItemResponse(
                                row.getId(),
                                row.getReservationNumber(),
                                row.getReservationDate().toString(),
                                row.getTimeSlot(),
                                row.getPatientName(),
                                row.getPatientPhone(),
                                row.getDepartmentName(),
                                row.getDoctorName(),
                                status,
                                STATUS_LABELS.getOrDefault(status, status),
                                "RESERVED".equals(status),
                                "RECEIVED".equals(status),
                                "COMPLETED".equals(status),
                                "CANCELLED".equals(status));
        }

        private List<AdminReservationStatusOptionResponse> buildStatusOptions(String selectedStatus, int size) {
                return List.of("ALL", "RESERVED", "RECEIVED", "COMPLETED", "CANCELLED").stream()
                                .map(value -> new AdminReservationStatusOptionResponse(
                                                value,
                                                STATUS_LABELS.get(value),
                                                buildListUrl(1, size, value),
                                                value.equals(selectedStatus)))
                                .toList();
        }

        private List<AdminReservationPageLinkResponse> buildPageLinks(int totalPages, int currentPage, int size,
                        String selectedStatus) {
                return IntStream.rangeClosed(1, totalPages)
                                .mapToObj(page -> new AdminReservationPageLinkResponse(
                                                page,
                                                buildListUrl(page, size, selectedStatus),
                                                page == currentPage))
                                .toList();
        }

        private String buildListUrl(int page, int size, String status) {
                return "/admin/reservation/list?page=" + page + "&size=" + size + "&status=" + status;
        }
}
