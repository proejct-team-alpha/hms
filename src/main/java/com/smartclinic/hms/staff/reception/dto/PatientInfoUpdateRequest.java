package com.smartclinic.hms.staff.reception.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatientInfoUpdateRequest {

    @NotNull(message = "예약 ID는 필수입니다.")
    private Long reservationId;

    private String address;
    private String note;

}
