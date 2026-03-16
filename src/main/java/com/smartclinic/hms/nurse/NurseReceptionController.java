package com.smartclinic.hms.nurse;

import com.smartclinic.hms.nurse.dto.NursePageLinkDto;
import com.smartclinic.hms.nurse.dto.NursePatientDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/nurse")
public class NurseReceptionController {

    private final NurseService nurseService;

    @GetMapping("/reception-list")
    public String receptionList(@RequestParam(name = "status", required = false) String status,
                                @RequestParam(name = "page", defaultValue = "0") int page,
                                Model model) {
        Page<com.smartclinic.hms.nurse.dto.NurseReservationDto> resultPage =
                nurseService.getReceptionPage(status, page);

        String baseUrl = (status != null && !status.isBlank())
                ? "/nurse/reception-list?status=" + status + "&page="
                : "/nurse/reception-list?page=";

        int totalPages = resultPage.getTotalPages();
        List<NursePageLinkDto> pageLinks = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageLinks.add(new NursePageLinkDto(i + 1, i == page, baseUrl + i));
        }

        model.addAttribute("reservations", resultPage.getContent());
        model.addAttribute("statusFilters", nurseService.getStatusFilters(status));
        model.addAttribute("hasPrev", page > 0);
        model.addAttribute("prevUrl", baseUrl + (page - 1));
        model.addAttribute("hasNext", page < totalPages - 1);
        model.addAttribute("nextUrl", baseUrl + (page + 1));
        model.addAttribute("pageLinks", pageLinks);
        model.addAttribute("pageTitle", "오늘 예약 현황");
        return "nurse/reception-list";
    }

    @GetMapping("/patient-detail")
    public String patientDetail(@RequestParam("id") Long id, Model model) {
        NursePatientDto detail = nurseService.getPatientDetail(id);
        model.addAttribute("detail", detail);
        model.addAttribute("pageTitle", "환자 상세 정보");
        return "nurse/patient-detail";
    }

    @PostMapping("/reservation/receive")
    public String receiveReservation(@RequestParam("id") Long id, RedirectAttributes ra) {
        try {
            nurseService.receiveReservation(id);
            ra.addFlashAttribute("message", "환자가 접수되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nurse/reception-list";
    }

    @PostMapping("/patient/update")
    public String updatePatient(@RequestParam("patientId") Long patientId,
                                @RequestParam("reservationId") Long reservationId,
                                @RequestParam("phone") String phone,
                                @RequestParam(name = "address", required = false, defaultValue = "") String address,
                                @RequestParam(name = "note", required = false, defaultValue = "") String note,
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
