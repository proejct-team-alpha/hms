package com.smartclinic.hms.admin.patient;

import com.smartclinic.hms.admin.patient.dto.AdminPatientDetailResponse;
import com.smartclinic.hms.admin.patient.dto.AdminPatientListResponse;
import com.smartclinic.hms.admin.patient.dto.AdminPatientPageLinkResponse;
import com.smartclinic.hms.admin.patient.dto.AdminPatientReservationHistoryItemResponse;
import com.smartclinic.hms.admin.patient.dto.UpdateAdminPatientApiResponse;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPatientService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final String PATIENT_NOT_FOUND_MESSAGE = "환자를 찾을 수 없습니다.";
    private static final String DUPLICATE_PATIENT_PHONE_ERROR_CODE = "DUPLICATE_PATIENT_PHONE";
    private static final String DUPLICATE_PATIENT_PHONE_MESSAGE = "이미 사용 중인 연락처입니다.";
    private static final String PATIENT_UPDATED_MESSAGE = "환자 정보가 수정되었습니다.";
    private static final String DEFAULT_TEXT = "-";

    private static final Map<ReservationStatus, String> STATUS_LABELS = Map.of(
            ReservationStatus.RESERVED, "예약",
            ReservationStatus.RECEIVED, "접수",
            ReservationStatus.IN_TREATMENT, "진료 중",
            ReservationStatus.COMPLETED, "완료",
            ReservationStatus.CANCELLED, "취소"
    );

    private final AdminPatientRepository adminPatientRepository;

    public AdminPatientListResponse getPatientList(int page, int size, String keyword) {
        int safePage = page < 1 ? DEFAULT_PAGE : page;
        int safeSize = size < 1 ? DEFAULT_SIZE : size;
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedContactKeyword = normalizeContact(keyword);

        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        Page<Patient> pageResult = adminPatientRepository.search(normalizedKeyword, normalizedContactKeyword, pageable);

        int currentPage = pageResult.getNumber() + 1;
        int totalPages = pageResult.getTotalPages();
        boolean hasPrevious = pageResult.hasPrevious();
        boolean hasNext = pageResult.hasNext();

        return new AdminPatientListResponse(
                pageResult.getContent().stream()
                        .map(AdminPatientSummary::from)
                        .toList(),
                buildPageLinks(totalPages, currentPage, safeSize, normalizedKeyword),
                normalizedKeyword,
                pageResult.getTotalElements(),
                currentPage,
                safeSize,
                totalPages,
                totalPages > 0,
                hasPrevious,
                hasNext,
                hasPrevious ? buildListUrl(currentPage - 1, safeSize, normalizedKeyword) : "",
                hasNext ? buildListUrl(currentPage + 1, safeSize, normalizedKeyword) : ""
        );
    }

    public AdminPatientDetailResponse getPatientDetail(Long patientId) {
        Patient patient = adminPatientRepository.findById(patientId)
                .orElseThrow(() -> CustomException.notFound(PATIENT_NOT_FOUND_MESSAGE));

        List<AdminPatientReservationHistoryItemResponse> reservationHistories = adminPatientRepository
                .findReservationHistoriesByPatientId(patientId)
                .stream()
                .map(history -> new AdminPatientReservationHistoryItemResponse(
                        history.getReservationNumber(),
                        history.getReservationDate().toString(),
                        history.getTimeSlot(),
                        history.getDepartmentName(),
                        history.getDoctorName(),
                        STATUS_LABELS.getOrDefault(history.getStatus(), history.getStatus().name())
                ))
                .toList();

        return new AdminPatientDetailResponse(
                patient.getId(),
                patient.getName(),
                patient.getPhone(),
                defaultText(patient.getEmail()),
                defaultText(patient.getAddress()),
                defaultText(patient.getNote()),
                patient.getNote() == null ? "" : patient.getNote().trim(),
                reservationHistories
        );
    }

    @Transactional
    public UpdateAdminPatientApiResponse updatePatient(Long patientId, String name, String phone, String note) {
        Patient patient = adminPatientRepository.findById(patientId)
                .orElseThrow(() -> CustomException.notFound(PATIENT_NOT_FOUND_MESSAGE));

        String normalizedName = normalizeRequiredText(name);
        String normalizedPhone = normalizeRequiredText(phone);
        String comparablePhone = normalizeComparablePhone(normalizedPhone);
        String normalizedNote = normalizeOptionalText(note);

        if (adminPatientRepository.existsByNormalizedPhoneAndIdNot(patientId, comparablePhone)) {
            throw CustomException.conflict(DUPLICATE_PATIENT_PHONE_ERROR_CODE, DUPLICATE_PATIENT_PHONE_MESSAGE);
        }

        patient.updateInfo(
                normalizedName,
                normalizedPhone,
                patient.getEmail(),
                patient.getAddress(),
                normalizedNote
        );

        return new UpdateAdminPatientApiResponse(
                patient.getId(),
                patient.getName(),
                patient.getPhone(),
                patient.getNote(),
                PATIENT_UPDATED_MESSAGE
        );
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private String normalizeRequiredText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeContact(String keyword) {
        return normalizeKeyword(keyword).replace("-", "");
    }

    private String normalizeComparablePhone(String phone) {
        return normalizeRequiredText(phone)
                .replace("-", "")
                .replace(" ", "");
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? DEFAULT_TEXT : value.trim();
    }

    private List<AdminPatientPageLinkResponse> buildPageLinks(
            int totalPages,
            int currentPage,
            int size,
            String keyword) {
        if (totalPages < 1) {
            return List.of();
        }

        return IntStream.rangeClosed(1, totalPages)
                .mapToObj(page -> new AdminPatientPageLinkResponse(
                        page,
                        buildListUrl(page, size, keyword),
                        page == currentPage
                ))
                .toList();
    }

    private String buildListUrl(int page, int size, String keyword) {
        StringBuilder builder = new StringBuilder("/admin/patient/list?page=")
                .append(page)
                .append("&size=")
                .append(size);

        if (!keyword.isBlank()) {
            builder.append("&keyword=").append(keyword);
        }

        return builder.toString();
    }
}
