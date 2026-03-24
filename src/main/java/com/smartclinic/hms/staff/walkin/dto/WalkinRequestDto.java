package com.smartclinic.hms.staff.walkin.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalkinRequestDto {

    /**
     * [신규] 예약 연동 시 사용할 기존 예약 ID
     */
    private Long reservationId;

    @NotBlank(message = "환자 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

    /**
     * 환자 생년월일 및 성별 정보 (예: 940101-1)
     */
    @NotBlank(message = "생년월일 및 성별 정보는 필수입니다.")
    private String birthInfo;

    /**
     * 내원 사유
     */
    private String visitReason;

    /**
     * 우편번호 (주소 API 연동용)
     */
    private String zipcode;

    /**
     * 기본 주소
     */
    private String address;

    /**
     * 상세 주소
     */
    private String detailAddress;

    @NotNull(message = "의사 선택은 필수입니다.")
    private Long doctorId;

    @NotNull(message = "진료과 선택은 필수입니다.")
    private Long departmentId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotBlank(message = "접수 시간은 필수입니다.")
    private String time;

    private String notes;

    // [기능 추가] 목록 화면 필터 유지를 위한 파라미터들
    private String query;
    private java.util.List<Long> deptIds;
    private java.util.List<Long> doctorIds;
    private String source;
    private String tab;
    private Integer page;
}
