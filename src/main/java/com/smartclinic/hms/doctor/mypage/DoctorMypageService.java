package com.smartclinic.hms.doctor.mypage;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.doctor.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorMypageService {

    private final DoctorRepository doctorRepository;

    public DoctorMypageDto getMypage(String username) {
        return doctorRepository.findByStaff_Username(username)
                .map(DoctorMypageDto::new)
                .orElseThrow(() -> CustomException.notFound("의사 정보를 찾을 수 없습니다."));
    }
}
