package com.smartclinic.hms.admin.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateAdminStaffRequest(
        @NotNull(message = "吏곸썝 ID???꾩닔?낅땲??")
        Long staffId,

        @NotBlank(message = "?대쫫? ?꾩닔?낅땲??")
        @Size(max = 50, message = "?대쫫? 50???댄븯濡??낅젰??二쇱꽭??")
        String name,

        Long departmentId,

        @Size(max = 100, message = "鍮꾨?踰덊샇??100???댄븯濡??낅젰??二쇱꽭??")
        String password,

        boolean active,

        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime retiredAt,

        String retiredAtDate,

        String retiredAtHour,

        List<String> availableDays
) {
    public UpdateAdminStaffRequest(
            Long staffId,
            String name,
            Long departmentId,
            String password,
            boolean active,
            LocalDateTime retiredAt,
            List<String> availableDays
    ) {
        this(staffId, name, departmentId, password, active, retiredAt, null, null, availableDays);
    }
}