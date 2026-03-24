package com.smartclinic.hms.staff.reception.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 환자 상세 정보 수정을 위한 요청 DTO (Staff 전용)
 */
@Data
public class PatientInfoUpdateRequest {

    @NotNull(message = "예약 ID는 필수입니다.")
    private Long reservationId;

    // [주석] 주소 및 메모 정보
    private String address;
    private String note;

    // [주석] 추가된 수집 필드 (프론트엔드 매핑용)
    private Long deptId;        // 최종 진료과 ID
    private Long doctorId;      // 담당 의사 ID
    private String birthInfo;   // 주민번호 (YYMMDD-G)
    private String email;       // 이메일 주소
    private String visitReason; // 내원 사유
}
