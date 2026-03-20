package com.smartclinic.hms.admin.patient;

import com.smartclinic.hms.admin.patient.dto.UpdateAdminPatientApiRequest;
import com.smartclinic.hms.admin.patient.dto.UpdateAdminPatientApiResponse;
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
@RequestMapping("/admin/api/patients")
public class AdminPatientApiController {

    private final AdminPatientService adminPatientService;

    @PostMapping("/{id}/update")
    public ResponseEntity<Resp<UpdateAdminPatientApiResponse>> updatePatient(
            @PathVariable("id") Long patientId,
            @Valid @RequestBody UpdateAdminPatientApiRequest request) {
        UpdateAdminPatientApiResponse response = adminPatientService.updatePatient(
                patientId,
                request.name(),
                request.phone(),
                request.note()
        );

        return Resp.ok(response);
    }
}
