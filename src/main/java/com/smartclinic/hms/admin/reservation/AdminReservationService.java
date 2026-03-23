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

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final Map<String, String> STATUS_LABELS = Map.of(
            "ALL", "전체",
            "RESERVED", "예약",
            "RECEIVED", "접수",
            "COMPLETED", "완료",
            "CANCELLED", "취소");

    private final AdminReservationRepository adminReservationRepository;

    public AdminReservationListResponse getReservationList(int page, int size, String statusParam, String keyword) {
        int safePage = page < 1 ? DEFAULT_PAGE : page;
        int safeSize = size < 1 ? DEFAULT_SIZE : size;
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedPhoneKeyword = normalizePhoneKeyword(keyword);

        ReservationStatus status = resolveStatus(statusParam);
        String selectedStatus = status == null ? "ALL" : status.name();

        Pageable pageable = PageRequest.of(
                safePage - 1,
                safeSize,
                Sort.by(Sort.Order.desc("reservationDate"), Sort.Order.desc("timeSlot")));

        Page<AdminReservationRepository.AdminReservationListProjection> pageResult = adminReservationRepository
                .findReservationListPage(status, normalizedKeyword, normalizedPhoneKeyword, pageable);

        List<AdminReservationItemResponse> reservations = pageResult.getContent().stream()
                .map(this::toItemResponse)
                .toList();

        int currentPage = pageResult.getNumber() + 1;
        int totalPages = pageResult.getTotalPages();
        boolean hasPrevious = pageResult.hasPrevious();
        boolean hasNext = pageResult.hasNext();

        List<AdminReservationStatusOptionResponse> statusOptions = buildStatusOptions(selectedStatus, safeSize,
                normalizedKeyword);
        List<AdminReservationPageLinkResponse> pageLinks = buildPageLinks(totalPages, currentPage, safeSize,
                selectedStatus, normalizedKeyword);

        String previousUrl = hasPrevious
                ? buildListUrl(currentPage - 1, safeSize, selectedStatus, normalizedKeyword)
                : "";
        String nextUrl = hasNext
                ? buildListUrl(currentPage + 1, safeSize, selectedStatus, normalizedKeyword)
                : "";

        return new AdminReservationListResponse(
                reservations,
                statusOptions,
                pageLinks,
                selectedStatus,
                normalizedKeyword,
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

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private String normalizePhoneKeyword(String keyword) {
        return normalizeKeyword(keyword).replace("-", "");
    }

    private AdminReservationItemResponse toItemResponse(
            AdminReservationRepository.AdminReservationListProjection row) {
        String status = row.getStatus().name();

        return new AdminReservationItemResponse(
                row.getId(),
                row.getReservationNumber(),
                row.getReservationDate().toString(),
                row.getTimeSlot(),
                row.getPatientId(),
                buildPatientDetailUrl(row.getPatientId()),
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

    private List<AdminReservationStatusOptionResponse> buildStatusOptions(String selectedStatus, int size,
            String keyword) {
        return List.of("ALL", "RESERVED", "RECEIVED", "COMPLETED", "CANCELLED").stream()
                .map(value -> new AdminReservationStatusOptionResponse(
                        value,
                        STATUS_LABELS.get(value),
                        buildListUrl(1, size, value, keyword),
                        value.equals(selectedStatus)))
                .toList();
    }

    private List<AdminReservationPageLinkResponse> buildPageLinks(int totalPages, int currentPage, int size,
            String selectedStatus, String keyword) {
        return IntStream.rangeClosed(1, totalPages)
                .mapToObj(page -> new AdminReservationPageLinkResponse(
                        page,
                        buildListUrl(page, size, selectedStatus, keyword),
                        page == currentPage))
                .toList();
    }

    private String buildListUrl(int page, int size, String status, String keyword) {
        StringBuilder builder = new StringBuilder("/admin/reservation/list?page=")
                .append(page)
                .append("&size=")
                .append(size)
                .append("&status=")
                .append(status);

        if (!keyword.isBlank()) {
            builder.append("&keyword=").append(keyword);
        }

        return builder.toString();
    }

    private String buildPatientDetailUrl(Long patientId) {
        return "/admin/patient/detail?patientId=" + patientId;
    }
}
