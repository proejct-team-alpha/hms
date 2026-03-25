package com.smartclinic.hms.admin.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaffRetirementScheduler {

    private static final long ONE_HOUR_MILLIS = 3_600_000L;

    private final AdminStaffService adminStaffService;

    @Scheduled(fixedRate = ONE_HOUR_MILLIS)
    public void deactivateExpiredStaffs() {
        adminStaffService.deactivateExpiredStaffs();
    }
}
