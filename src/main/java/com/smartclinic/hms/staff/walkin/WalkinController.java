package com.smartclinic.hms.staff.walkin;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclinic.hms.staff.reception.ReceptionService;
import com.smartclinic.hms.staff.walkin.dto.WalkinRequestDto;
import com.smartclinic.hms.common.exception.CustomException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff")
public class WalkinController {

    private final WalkinService walkinService;
    private final ReceptionService receptionService;

    /**
     * 방문 접수 화면 (예약 정보 연동 포함)
     * [주석] 예약 환자 목록에서 넘어온 정보가 있다면 모델에 담아 화면에 미리 채워줍니다.
     */
    @GetMapping("/walkin-reception")
    public String walkinPage(
            @RequestParam(name = "reservationId", required = false) Long reservationId,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "deptId", required = false) Long deptId,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            Model model) {
        
        model.addAttribute("departments", receptionService.getAllDepartments());
        model.addAttribute("doctors", receptionService.getAllDoctors());
        model.addAttribute("today", LocalDate.now().toString());

        model.addAttribute("reservationId", reservationId);
        model.addAttribute("name", name);
        
        // [주석] 전화번호(010-1234-5678)를 하이픈 기준으로 분리하여 각 입력 칸에 배치합니다.
        if (phone != null && phone.contains("-")) {
            String[] parts = phone.split("-");
            if (parts.length >= 1) model.addAttribute("p1", parts[0]);
            if (parts.length >= 2) model.addAttribute("p2", parts[1]);
            if (parts.length >= 3) model.addAttribute("p3", parts[2]);
        } else if (phone != null) {
            model.addAttribute("p1", phone);
        }

        model.addAttribute("selectedDeptId", deptId);
        model.addAttribute("selectedDoctorId", doctorId);

        return "staff/walkin-reception";
    }

    /**
     * 방문 접수 생성 및 예약 상태 변경 처리
     */
    @PostMapping("/walkin")
    public String createWalkin(
            @Valid WalkinRequestDto request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        // 1. DTO 검증 실패 처리
        if (bindingResult.hasErrors()) {
            model.addAttribute("form", request);
            model.addAttribute("departments", receptionService.getAllDepartments());
            model.addAttribute("doctors", receptionService.getAllDoctors());
            model.addAttribute("today", LocalDate.now().toString());
            return "staff/walkin-reception";
        }

        try {
            boolean nameMismatch = walkinService.createWalkin(request);

            if (nameMismatch) {
                redirectAttributes.addFlashAttribute("message",
                        "입력하신 이름과 기존 환자의 이름이 다릅니다. 기존 환자명으로 방문 접수가 완료되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("message", "방문 접수가 완료되었습니다.");
            }

            return "redirect:/staff/reception/list?date=" + request.getDate();

        } catch (CustomException e) {
            model.addAttribute("message", e.getMessage());
            model.addAttribute("form", request);
            model.addAttribute("departments", receptionService.getAllDepartments());
            model.addAttribute("doctors", receptionService.getAllDoctors());
            model.addAttribute("today", LocalDate.now().toString());
            return "staff/walkin-reception";
        }
    }
}
