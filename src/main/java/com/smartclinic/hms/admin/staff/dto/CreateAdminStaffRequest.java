package com.smartclinic.hms.admin.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

public record CreateAdminStaffRequest(
        @NotBlank(message = "濡쒓렇???꾩씠?붾뒗 ?꾩닔?낅땲??")
        @Size(max = 50, message = "濡쒓렇???꾩씠?붾뒗 50???댄븯濡??낅젰??二쇱꽭??")
        String username,

        @NotBlank(message = "鍮꾨?踰덊샇???꾩닔?낅땲??")
        @Size(min = 8, message = "鍮꾨?踰덊샇??8???댁긽?댁뼱???⑸땲??")
        String password,

        @NotBlank(message = "?대쫫? ?꾩닔?낅땲??")
        @Size(max = 50, message = "?대쫫? 50???댄븯濡??낅젰??二쇱꽭??")
        String name,

        @NotBlank(message = "?щ쾲? ?꾩닔?낅땲??")
        @Size(max = 20, message = "?щ쾲? 20???댄븯濡??낅젰??二쇱꽭??")
        String employeeNumber,

        @NotBlank(message = "??븷? ?꾩닔?낅땲??")
        String role,

        Long departmentId,

        boolean active,

        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime retiredAt,

        String retiredAtDate,

        String retiredAtHour,

        List<String> availableDays
) {
    public CreateAdminStaffRequest(
            String username,
            String password,
            String name,
            String employeeNumber,
            String role,
            Long departmentId,
            boolean active,
            LocalDateTime retiredAt,
            List<String> availableDays
    ) {
        this(username, password, name, employeeNumber, role, departmentId, active, retiredAt, null, null, availableDays);
    }
}