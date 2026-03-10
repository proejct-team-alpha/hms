package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationListItemView;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationListView;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationPageLink;
import com.smartclinic.hms.admin.reservation.dto.AdminReservationStatusOption;
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
            "CANCELLED", "취소"
    );

    private final AdminReservationRepository adminReservationRepository;

    public AdminReservationListView getReservationList(int page, int size, String statusParam) {
        int safePage = page < 1 ? DEFAULT_PAGE : page;
        int safeSize = size < 1 ? DEFAULT_SIZE : size;

        ReservationStatus status = resolveStatus(statusParam);
        String selectedStatus = status == null ? "ALL" : status.name();

        Pageable pageable = PageRequest.of(
                safePage - 1,
                safeSize,
                Sort.by(Sort.Order.desc("reservationDate"), Sort.Order.desc("timeSlot"))
        );

        Page<AdminReservationRepository.AdminReservationListProjection> pageResult =
                adminReservationRepository.findReservationListPage(status, pageable);

        List<AdminReservationListItemView> reservations = pageResult.getContent().stream()
                .map(this::toItemView)
                .toList();

        int currentPage = pageResult.getNumber() + 1;
        int totalPages = pageResult.getTotalPages();

        List<AdminReservationStatusOption> statusOptions = buildStatusOptions(selectedStatus, safeSize);
        List<AdminReservationPageLink> pageLinks = buildPageLinks(totalPages, currentPage, safeSize, selectedStatus);

        boolean hasPrevious = pageResult.hasPrevious();
        boolean hasNext = pageResult.hasNext();

        String previousUrl = hasPrevious
                ? buildListUrl(currentPage - 1, safeSize, selectedStatus)
                : "";
        String nextUrl = hasNext
                ? buildListUrl(currentPage + 1, safeSize, selectedStatus)
                : "";

        return new AdminReservationListView(
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
                nextUrl
        );
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

    private AdminReservationListItemView toItemView(AdminReservationRepository.AdminReservationListProjection row) {
        String status = row.getStatus().name();

        return new AdminReservationListItemView(
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
                "CANCELLED".equals(status)
        );
    }

    private List<AdminReservationStatusOption> buildStatusOptions(String selectedStatus, int size) {
        return List.of("ALL", "RESERVED", "RECEIVED", "COMPLETED", "CANCELLED").stream()
                .map(value -> new AdminReservationStatusOption(
                        value,
                        STATUS_LABELS.get(value),
                        buildListUrl(1, size, value),
                        value.equals(selectedStatus)
                ))
                .toList();
    }

    private List<AdminReservationPageLink> buildPageLinks(int totalPages, int currentPage, int size, String selectedStatus) {
        return IntStream.rangeClosed(1, totalPages)
                .mapToObj(page -> new AdminReservationPageLink(
                        page,
                        buildListUrl(page, size, selectedStatus),
                        page == currentPage
                ))
                .toList();
    }

    private String buildListUrl(int page, int size, String status) {
        return "/admin/reservation/list?page=" + page + "&size=" + size + "&status=" + status;
    }
}
