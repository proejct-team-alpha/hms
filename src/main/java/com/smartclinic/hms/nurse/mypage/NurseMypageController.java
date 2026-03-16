package com.smartclinic.hms.nurse.mypage;

import com.smartclinic.hms.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/nurse")
public class NurseMypageController {

    private final NurseMypageService nurseMypageService;

    @GetMapping("/mypage")
    public String mypage(Authentication auth, Model model) {
        model.addAttribute("info", nurseMypageService.getMypage(auth.getName()));
        model.addAttribute("pageTitle", "내 정보 관리");
        return "nurse/mypage";
    }

    @PostMapping("/mypage")
    public String updateMypage(Authentication auth,
                               @RequestParam String name,
                               @RequestParam(required = false) String currentPassword,
                               @RequestParam(required = false) String newPassword,
                               @RequestParam(required = false) String confirmPassword,
                               RedirectAttributes ra) {
        try {
            nurseMypageService.updateMypage(auth.getName(), name, currentPassword, newPassword, confirmPassword);
            ra.addFlashAttribute("success", "정보가 수정되었습니다.");
        } catch (CustomException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/nurse/mypage";
    }
}
