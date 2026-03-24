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

import com.smartclinic.hms.item.log.ItemUsageLogDto;

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
    public ResponseEntity<Resp<List<DoctorReservationDto>>> pollTreatmentList(Authentication auth,
                                                                             @RequestParam(name = "query", required = false) String query) {
        List<DoctorReservationDto> list = treatmentService.getTodayReceivedList(auth.getName(), query);
        return Resp.ok(list);
    }

    @GetMapping("/treatment-list")
    public String treatmentList(Authentication auth,
                                @RequestParam(name = "date", required = false) String date,
                                @RequestParam(name = "tab", defaultValue = "waiting") String tab,
                                @RequestParam(name = "query", required = false) String query,
                                @RequestParam(name = "page", defaultValue = "0") int page,
                                Model model) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate searchDate = (date != null && !date.isBlank()) 
                ? java.time.LocalDate.parse(date) 
                : today;

        Page<DoctorReservationDto> resultPage = treatmentService.getTreatmentPage(auth.getName(), searchDate, tab, query, page);
        
        StringBuilder baseUrl = new StringBuilder("/doctor/treatment-list?");
        baseUrl.append("date=").append(searchDate.toString()).append("&");
        baseUrl.append("tab=").append(tab).append("&");
        if (query != null && !query.isBlank()) {
            baseUrl.append("query=").append(query).append("&");
        }
        baseUrl.append("page=");
        
        addPaginationAttributes(model, resultPage, page, baseUrl.toString());
        model.addAttribute("searchDate", searchDate.toString());
        model.addAttribute("currentTab", tab);
        model.addAttribute("isWaitingTab", "waiting".equals(tab));
        model.addAttribute("isCompletedTab", "completed".equals(tab));
        model.addAttribute("isAllTab", "all".equals(tab));
        model.addAttribute("isToday", searchDate.isEqual(today));
        model.addAttribute("isPastDate", searchDate.isBefore(today));
        model.addAttribute("query", query);
        model.addAttribute("totalPatients", resultPage.getTotalElements());
        model.addAttribute("dailyTotalPatients", treatmentService.getDailyTotalCount(auth.getName(), searchDate));
        model.addAttribute("pageTitle", "진료 현황");
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
            List<ItemUsageLogDto> logs = reservationId != null
                    ? itemManagerService.getUsageLogs(reservationId)
                    : List.of();
            return ResponseEntity.ok(Map.of("quantity", newQuantity, "logs", logs));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/item/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelItem(@RequestParam("logId") Long logId,
                                        @RequestParam("reservationId") Long reservationId) {
        try {
            java.util.Map<String, Object> result = itemManagerService.cancelItemUsage(logId);
            List<ItemUsageLogDto> logs = itemManagerService.getUsageLogs(reservationId);
            return ResponseEntity.ok(Map.of(
                "itemId", result.get("itemId"),
                "quantity", result.get("quantity"),
                "logs", logs
            ));
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

    @PostMapping("/treatment/save")
    public String saveTreatment(@RequestParam("id") Long id,
                                @RequestParam("diagnosis") String diagnosis,
                                @RequestParam("prescription") String prescription,
                                @RequestParam(name = "remark", required = false) String remark,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            treatmentService.saveTreatmentRecord(id, auth.getName(), diagnosis, prescription, remark);
            redirectAttributes.addFlashAttribute("message", "진료 기록이 저장되었습니다.");
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
                                @RequestParam(name = "date", required = false) String date,
                                @RequestParam(name = "query", required = false) String query,
                                @RequestParam(name = "page", defaultValue = "0") int page,
                                Model model) {
        java.time.LocalDate searchDate = (date != null && !date.isBlank()) 
                ? java.time.LocalDate.parse(date) 
                : java.time.LocalDate.now();
        
        Page<DoctorReservationDto> resultPage = treatmentService.getCompletedPage(auth.getName(), searchDate, query, page);
        long totalForDate = treatmentService.getCompletedCountForDate(auth.getName(), searchDate);
        
        StringBuilder baseUrl = new StringBuilder("/doctor/completed-list?date=").append(searchDate.toString());
        if (query != null && !query.isBlank()) {
            baseUrl.append("&query=").append(query);
        }
        baseUrl.append("&page=");
        
        addPaginationAttributes(model, resultPage, page, baseUrl.toString());
        
        model.addAttribute("searchDate", searchDate.toString());
        model.addAttribute("query", query);
        model.addAttribute("totalForDate", totalForDate);
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
