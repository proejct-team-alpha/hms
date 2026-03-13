package com.smartclinic.hms.doctor.treatment;

import com.smartclinic.hms.doctor.treatment.dto.DoctorReservationDto;
import com.smartclinic.hms.doctor.treatment.dto.DoctorTreatmentDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/doctor")
public class DoctorTreatmentController {

    private final DoctorTreatmentService treatmentService;

    @GetMapping("/treatment-list")
    public String treatmentList(Authentication auth, Model model) {
        List<DoctorReservationDto> list = treatmentService.getTreatmentList(auth.getName());
        model.addAttribute("reservations", list);
        model.addAttribute("pageTitle", "진료 목록");
        return "doctor/treatment-list";
    }

    @GetMapping("/treatment-detail")
    public String treatmentDetail(@RequestParam Long id, Authentication auth, Model model) {
        DoctorTreatmentDetailDto detail = treatmentService.getTreatmentDetail(id, auth.getName());
        model.addAttribute("detail", detail);
        model.addAttribute("pageTitle", "진료실");
        return "doctor/treatment-detail";
    }

    @PostMapping("/treatment/complete")
    public String completeTreatment(@RequestParam Long id,
                                    @RequestParam String diagnosis,
                                    @RequestParam String prescription,
                                    @RequestParam(required = false) String remark,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            treatmentService.completeTreatment(id, auth.getName(), diagnosis, prescription, remark);
            redirectAttributes.addFlashAttribute("message", "진료가 완료되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/doctor/treatment-detail?id=" + id;
        }
        return "redirect:/doctor/treatment-list";
    }

    @GetMapping("/completed-list")
    public String completedList(Authentication auth, Model model) {
        List<DoctorReservationDto> list = treatmentService.getCompletedList(auth.getName());
        model.addAttribute("reservations", list);
        model.addAttribute("pageTitle", "진료 완료 목록");
        return "doctor/completed-list";
    }
}
