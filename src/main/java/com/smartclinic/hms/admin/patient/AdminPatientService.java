package com.smartclinic.hms.admin.patient;

import com.smartclinic.hms.admin.patient.dto.AdminPatientListResponse;
import com.smartclinic.hms.admin.patient.dto.AdminPatientPageLinkResponse;
import com.smartclinic.hms.domain.Patient;
import java.util.List;
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
public class AdminPatientService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private final AdminPatientRepository adminPatientRepository;

    public AdminPatientListResponse getPatientList(int page, int size, String nameKeyword, String contactKeyword) {
        int safePage = page < 1 ? DEFAULT_PAGE : page;
        int safeSize = size < 1 ? DEFAULT_SIZE : size;

        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        Page<Patient> pageResult = adminPatientRepository.search(nameKeyword, normalizeContact(contactKeyword), pageable);

        int currentPage = pageResult.getNumber() + 1;
        int totalPages = pageResult.getTotalPages();
        boolean hasPrevious = pageResult.hasPrevious();
        boolean hasNext = pageResult.hasNext();

        return new AdminPatientListResponse(
                pageResult.getContent().stream()
                        .map(AdminPatientSummary::from)
                        .toList(),
                buildPageLinks(totalPages, currentPage, safeSize, nameKeyword, contactKeyword),
                normalizeKeyword(nameKeyword),
                normalizeKeyword(contactKeyword),
                pageResult.getTotalElements(),
                currentPage,
                safeSize,
                totalPages,
                totalPages > 0,
                hasPrevious,
                hasNext,
                hasPrevious ? buildListUrl(currentPage - 1, safeSize, nameKeyword, contactKeyword) : "",
                hasNext ? buildListUrl(currentPage + 1, safeSize, nameKeyword, contactKeyword) : ""
        );
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private String normalizeContact(String contactKeyword) {
        return normalizeKeyword(contactKeyword).replace("-", "");
    }

    private List<AdminPatientPageLinkResponse> buildPageLinks(
            int totalPages,
            int currentPage,
            int size,
            String nameKeyword,
            String contactKeyword) {
        if (totalPages < 1) {
            return List.of();
        }

        return IntStream.rangeClosed(1, totalPages)
                .mapToObj(page -> new AdminPatientPageLinkResponse(
                        page,
                        buildListUrl(page, size, nameKeyword, contactKeyword),
                        page == currentPage
                ))
                .toList();
    }

    private String buildListUrl(int page, int size, String nameKeyword, String contactKeyword) {
        StringBuilder builder = new StringBuilder("/admin/patient/list?page=")
                .append(page)
                .append("&size=")
                .append(size);

        String normalizedNameKeyword = normalizeKeyword(nameKeyword);
        String normalizedContactKeyword = normalizeKeyword(contactKeyword);

        if (!normalizedNameKeyword.isBlank()) {
            builder.append("&nameKeyword=").append(normalizedNameKeyword);
        }
        if (!normalizedContactKeyword.isBlank()) {
            builder.append("&contactKeyword=").append(normalizedContactKeyword);
        }
        return builder.toString();
    }
}
