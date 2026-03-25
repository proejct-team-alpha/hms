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
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "deptId", required = false) Long deptId,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "deptIds", required = false) java.util.List<Long> deptIds,
            @RequestParam(name = "doctorIds", required = false) java.util.List<Long> doctorIds,
            @RequestParam(name = "source", required = false) String source,
            @RequestParam(name = "tab", required = false) String tab,
            @RequestParam(name = "page", required = false) Integer page,
            Model model) {
        
        model.addAttribute("departments", receptionService.getAllDepartments());
        model.addAttribute("doctors", receptionService.getAllDoctors());
        model.addAttribute("today", LocalDate.now().toString());

        model.addAttribute("reservationId", reservationId);
        model.addAttribute("patientId", patientId);
        model.addAttribute("name", name);
        
        // [주석] 필터 유지를 위한 파라미터들을 모델에 담습니다.
        model.addAttribute("query", query);
        model.addAttribute("deptIds", deptIds);
        model.addAttribute("doctorIds", doctorIds);
        model.addAttribute("source", source);
        model.addAttribute("tab", tab);
        model.addAttribute("page", page);
        
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
            java.util.List<String> errors = new java.util.ArrayList<>();
            bindingResult.getAllErrors().forEach(e -> errors.add(e.getDefaultMessage()));
            model.addAttribute("message", String.join(", ", errors));
            model.addAttribute("name", request.getName());
            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                String[] parts = request.getPhone().split("-");
                if (parts.length >= 1) model.addAttribute("p1", parts[0]);
                if (parts.length >= 2) model.addAttribute("p2", parts[1]);
                if (parts.length >= 3) model.addAttribute("p3", parts[2]);
            }
            model.addAttribute("selectedDeptId", request.getDepartmentId());
            model.addAttribute("selectedDoctorId", request.getDoctorId());
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

            // [기능 추가] 목록 화면 필터 유지를 위한 파라미터들 전달 (날짜는 ISO 표준 형식으로 강제)
            redirectAttributes.addAttribute("date", request.getDate().toString());
            if (request.getQuery() != null) redirectAttributes.addAttribute("query", request.getQuery());
            if (request.getDeptIds() != null) redirectAttributes.addAttribute("deptIds", request.getDeptIds());
            if (request.getDoctorIds() != null) redirectAttributes.addAttribute("doctorIds", request.getDoctorIds());
            if (request.getSource() != null) redirectAttributes.addAttribute("source", request.getSource());
            // 방문 접수는 즉시 '진료대기' 상태이므로 received 탭으로 이동해야 목록에 표시됨
            redirectAttributes.addAttribute("tab", "received");

            return "redirect:/staff/reception/list";

        } catch (CustomException e) {
            model.addAttribute("message", e.getMessage());
            model.addAttribute("name", request.getName());
            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                String[] parts = request.getPhone().split("-");
                if (parts.length >= 1) model.addAttribute("p1", parts[0]);
                if (parts.length >= 2) model.addAttribute("p2", parts[1]);
                if (parts.length >= 3) model.addAttribute("p3", parts[2]);
            }
            model.addAttribute("selectedDeptId", request.getDepartmentId());
            model.addAttribute("selectedDoctorId", request.getDoctorId());
            model.addAttribute("departments", receptionService.getAllDepartments());
            model.addAttribute("doctors", receptionService.getAllDoctors());
            model.addAttribute("today", LocalDate.now().toString());
            return "staff/walkin-reception";
        }
    }
}
