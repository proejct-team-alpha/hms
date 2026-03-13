package com.smartclinic.hms.doctor.mypage;

import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.Doctor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/doctor")
public class DoctorMypageController {

    private final DoctorRepository doctorRepository;

    @GetMapping("/mypage")
    public String mypage(Authentication auth, Model model) {
        Doctor doctor = doctorRepository.findByStaff_Username(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("의사 정보를 찾을 수 없습니다."));

        DoctorMypageDto dto = new DoctorMypageDto(doctor);
        model.addAttribute("info", dto);
        model.addAttribute("pageTitle", "내 정보 관리");
        return "doctor/mypage";
    }
}
