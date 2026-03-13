package com.smartclinic.hms.nurse.mypage;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.domain.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/nurse")
public class NurseMypageController {

    private final StaffRepository staffRepository;

    @GetMapping("/mypage")
    public String mypage(Authentication auth, Model model) {
        Staff staff = staffRepository.findByUsernameAndActiveTrue(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("직원 정보를 찾을 수 없습니다."));
        model.addAttribute("info", new NurseMypageDto(staff));
        model.addAttribute("pageTitle", "내 정보 관리");
        return "nurse/mypage";
    }
}
