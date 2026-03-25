package com.smartclinic.hms.admin.staff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateAdminStaffApiRequest(
        @NotBlank(message = "?대쫫? ?꾩닔?낅땲??")
        @Size(max = 50, message = "?대쫫? 50???댄븯濡??낅젰??二쇱꽭??")
        String name,

        Long departmentId,

        @Size(max = 100, message = "鍮꾨?踰덊샇??100???댄븯濡??낅젰??二쇱꽭??")
        String password,

        boolean active,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime retiredAt,

        String retiredAtDate,

        String retiredAtHour,

        List<String> availableDays
) {
    public UpdateAdminStaffApiRequest(
            String name,
            Long departmentId,
            String password,
            boolean active,
            LocalDateTime retiredAt,
            List<String> availableDays
    ) {
        this(name, departmentId, password, active, retiredAt, null, null, availableDays);
    }
}