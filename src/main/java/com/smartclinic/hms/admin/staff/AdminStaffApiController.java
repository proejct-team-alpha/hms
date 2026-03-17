package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffApiRequest;
import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffApiResponse;
import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffRequest;
import com.smartclinic.hms.common.util.Resp;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/staff")
public class AdminStaffApiController {

    private final AdminStaffService adminStaffService;

    @PostMapping("/{id}")
    public ResponseEntity<Resp<UpdateAdminStaffApiResponse>> updateStaff(
            @PathVariable("id") Long staffId,
            @Valid @RequestBody UpdateAdminStaffApiRequest request) {
        UpdateAdminStaffRequest serviceRequest = new UpdateAdminStaffRequest(
                staffId,
                request.name(),
                request.departmentId(),
                request.password(),
                request.specialty(),
                request.availableDays()
        );

        String successMessage = adminStaffService.updateStaff(serviceRequest);
        UpdateAdminStaffApiResponse response = adminStaffService.getUpdateApiResponse(staffId, successMessage);

        return Resp.ok(response);
    }
}
