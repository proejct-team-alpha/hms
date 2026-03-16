package com.smartclinic.hms.nurse.mypage;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NurseMypageService {

    private final StaffRepository staffRepository;

    public NurseMypageDto getMypage(String username) {
        return staffRepository.findByUsernameAndActiveTrue(username)
                .map(NurseMypageDto::new)
                .orElseThrow(() -> CustomException.notFound("직원 정보를 찾을 수 없습니다."));
    }
}
