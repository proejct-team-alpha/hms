package com.smartclinic.hms.admin.staff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class StaffRetirementSchedulerTest {

    @Mock
    private AdminStaffService adminStaffService;

    @InjectMocks
    private StaffRetirementScheduler staffRetirementScheduler;

    @Test
    @DisplayName("스케줄러는 만료 직원 비활성화를 서비스에 위임한다")
    void deactivateExpiredStaffs_delegatesToService() {
        // when
        staffRetirementScheduler.deactivateExpiredStaffs();

        // then
        then(adminStaffService).should().deactivateExpiredStaffs();
    }
}
