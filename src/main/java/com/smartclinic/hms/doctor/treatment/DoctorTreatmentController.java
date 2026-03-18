package com.smartclinic.hms.doctor.treatment;

import com.smartclinic.hms.common.util.Resp;
import com.smartclinic.hms.doctor.treatment.dto.DoctorPageLinkDto;
import com.smartclinic.hms.doctor.treatment.dto.DoctorReservationDto;
import com.smartclinic.hms.doctor.treatment.dto.DoctorTreatmentDetailDto;
import com.smartclinic.hms.item.ItemManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/doctor")
public class DoctorTreatmentController {

    private final DoctorTreatmentService treatmentService;
    private final ItemManagerService itemManagerService;

    // [W3-1] GET /doctor/treatment-list/poll — 5초 폴링 AJAX 엔드포인트
    @GetMapping("/treatment-list/poll")
    @ResponseBody
    public ResponseEntity<Resp<List<DoctorReservationDto>>> pollTreatmentList(Authentication auth) {
        List<DoctorReservationDto> list = treatmentService.getTodayReceivedList(auth.getName());
        return Resp.ok(list);
    }

    @GetMapping("/treatment-list")
    public String treatmentList(Authentication auth,
                                @RequestParam(name = "page", defaultValue = "0") int page,
                                Model model) {
        Page<DoctorReservationDto> resultPage = treatmentService.getTreatmentPage(auth.getName(), page);
        addPaginationAttributes(model, resultPage, page, "/doctor/treatment-list?page=");
        model.addAttribute("pageTitle", "진료 목록");
        return "doctor/treatment-list";
    }

    @GetMapping("/treatment-detail")
    public String treatmentDetail(@RequestParam("id") Long id, Authentication auth, Model model) {
        DoctorTreatmentDetailDto detail = treatmentService.getTreatmentDetail(id, auth.getName());
        model.addAttribute("detail", detail);
        model.addAttribute("items", itemManagerService.getItemList(null));
        model.addAttribute("usageLogs", itemManagerService.getUsageLogs(id));
        model.addAttribute("pageTitle", "진료실");
        return "doctor/treatment-detail";
    }

    // 진료 완료 목록에서 진료 차트 조회 — /doctor/completed-list 사이드바 활성화
    @GetMapping("/completed-detail")
    public String completedDetail(@RequestParam("id") Long id, Authentication auth, Model model) {
        DoctorTreatmentDetailDto detail = treatmentService.getTreatmentDetail(id, auth.getName());
        model.addAttribute("detail", detail);
        model.addAttribute("usageLogs", itemManagerService.getUsageLogs(id));
        model.addAttribute("pageTitle", "진료 완료 기록");
        return "doctor/treatment-detail";
    }

    @PostMapping("/item/use")
    @ResponseBody
    public ResponseEntity<?> useItem(@RequestParam("id") Long id,
                                     @RequestParam("amount") String amountStr,
                                     @RequestParam(name = "reservationId", required = false) Long reservationId) {
        try {
            long parsed = Long.parseLong(amountStr.trim());
            if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
                return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
            }
            int newQuantity = itemManagerService.useItem(id, (int) parsed, reservationId);
            return ResponseEntity.ok(Map.of("quantity", newQuantity));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/treatment/start")
    public String startTreatment(@RequestParam("id") Long id,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            treatmentService.startTreatment(id, auth.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/doctor/treatment-detail?id=" + id;
    }

    @PostMapping("/treatment/complete")
    public String completeTreatment(@RequestParam("id") Long id,
                                    @RequestParam("diagnosis") String diagnosis,
                                    @RequestParam("prescription") String prescription,
                                    @RequestParam(name = "remark", required = false) String remark,
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
                                @RequestParam(name = "page", defaultValue = "0") int page,
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
