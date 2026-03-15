package com.smartclinic.hms.doctor.treatment;

import com.smartclinic.hms.doctor.treatment.dto.DoctorPageLinkDto;
import com.smartclinic.hms.doctor.treatment.dto.DoctorReservationDto;
import com.smartclinic.hms.doctor.treatment.dto.DoctorTreatmentDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/doctor")
public class DoctorTreatmentController {

    private final DoctorTreatmentService treatmentService;

    @GetMapping("/treatment-list")
    public String treatmentList(Authentication auth,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        Page<DoctorReservationDto> resultPage = treatmentService.getTreatmentPage(auth.getName(), page);
        addPaginationAttributes(model, resultPage, page, "/doctor/treatment-list?page=");
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
    public String completedList(Authentication auth,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        Page<DoctorReservationDto> resultPage = treatmentService.getCompletedPage(auth.getName(), page);
        addPaginationAttributes(model, resultPage, page, "/doctor/completed-list?page=");
        model.addAttribute("pageTitle", "진료 완료 목록");
        return "doctor/completed-list";
    }

    private void addPaginationAttributes(Model model, Page<DoctorReservationDto> resultPage,
                                         int page, String baseUrl) {
        int totalPages = resultPage.getTotalPages();
        List<DoctorPageLinkDto> pageLinks = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageLinks.add(new DoctorPageLinkDto(i + 1, i == page, baseUrl + i));
        }
        model.addAttribute("reservations", resultPage.getContent());
        model.addAttribute("hasPrev", page > 0);
        model.addAttribute("prevUrl", baseUrl + (page - 1));
        model.addAttribute("hasNext", page < totalPages - 1);
        model.addAttribute("nextUrl", baseUrl + (page + 1));
        model.addAttribute("pageLinks", pageLinks);
    }
}
