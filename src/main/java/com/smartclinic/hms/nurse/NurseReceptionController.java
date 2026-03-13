package com.smartclinic.hms.nurse;

import com.smartclinic.hms.nurse.dto.NursePatientDto;
import com.smartclinic.hms.nurse.dto.NurseReservationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/nurse")
public class NurseReceptionController {

    private final NurseService nurseService;

    @GetMapping("/reception-list")
    public String receptionList(@RequestParam(required = false) String status, Model model) {
        List<NurseReservationDto> reservations = nurseService.getReceptionList(status);
        model.addAttribute("reservations", reservations);
        model.addAttribute("statusFilters", nurseService.getStatusFilters(status));
        model.addAttribute("pageTitle", "오늘 예약 현황");
        return "nurse/reception-list";
    }

    @GetMapping("/patient-detail")
    public String patientDetail(@RequestParam Long id, Model model) {
        NursePatientDto detail = nurseService.getPatientDetail(id);
        model.addAttribute("detail", detail);
        model.addAttribute("pageTitle", "환자 상세 정보");
        return "nurse/patient-detail";
    }

    @PostMapping("/reservation/receive")
    public String receiveReservation(@RequestParam Long id, RedirectAttributes ra) {
        try {
            nurseService.receiveReservation(id);
            ra.addFlashAttribute("message", "환자가 접수되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nurse/reception-list";
    }

    @PostMapping("/patient/update")
    public String updatePatient(@RequestParam Long patientId,
                                @RequestParam Long reservationId,
                                @RequestParam String phone,
                                @RequestParam(required = false, defaultValue = "") String address,
                                @RequestParam(required = false, defaultValue = "") String note,
                                RedirectAttributes ra) {
        try {
            nurseService.updatePatient(patientId, phone, address, note);
            ra.addFlashAttribute("message", "환자 정보가 저장되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nurse/patient-detail?id=" + reservationId;
    }
}
