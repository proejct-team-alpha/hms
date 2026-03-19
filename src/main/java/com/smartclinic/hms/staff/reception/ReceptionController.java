package com.smartclinic.hms.staff.reception;

import java.util.HashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.staff.dto.StaffReservationDto;
import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/staff/reception")
@RequiredArgsConstructor
public class ReceptionController {

    private final ReceptionService receptionService;

    private static final int PAGE_SIZE = 10;

    // 접수 목록
    @GetMapping("/list")
    public String list(
            @RequestParam(name = "status", defaultValue = "") String status,
            @RequestParam(name = "date", required = false) String date,
            @RequestParam(name = "query", required = false) String query,
            @ModelAttribute("date") String flashDate,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {

        if ((date == null || date.isBlank()) && flashDate != null && !flashDate.isBlank()) {
            date = flashDate;
        }
        model.addAttribute("isStaffReception", true);
        // date 없으면 null → 오늘 이후 전체 조회
        LocalDate selectedDate = (date == null || date.isBlank()) ? null : LocalDate.parse(date);
        String dateStr = selectedDate != null ? selectedDate.toString() : "";

        List<StaffReservationDto> all = receptionService.getReservations(selectedDate, status, query);

        // 페이징
        int total = all.size();
        int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(1, Math.min(page, totalPages));
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        List<StaffReservationDto> paged = all.subList(from, to);

        // 페이지 URL 기본 경로 (date, status, query 포함)
        String q = (query == null) ? "" : query;
        String baseUrl = "/staff/reception/list?date=" + dateStr + "&status=" + status + "&query=" + q;

        model.addAttribute("reservations", paged);
        model.addAttribute("filters", receptionService.getStatusFilters(status, dateStr, query));
        // 날짜 네비게이션
        model.addAttribute("hasDate", selectedDate != null);
        model.addAttribute("todayDate", LocalDate.now().toString());
        model.addAttribute("selectedDate", selectedDate != null ? selectedDate.toString() : "오늘 이후 전체");
        model.addAttribute("prevDate", selectedDate != null ? selectedDate.minusDays(1).toString() : "");
        model.addAttribute("nextDate", selectedDate != null ? selectedDate.plusDays(1).toString() : "");
        model.addAttribute("currentDate", dateStr);
        model.addAttribute("currentStatus", status);
        model.addAttribute("query", q);
        // 페이징
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", total);
        model.addAttribute("hasPrev", page > 1);
        model.addAttribute("hasNext", page < totalPages);
        model.addAttribute("prevPageUrl", baseUrl + "&page=" + (page - 1));
        model.addAttribute("nextPageUrl", baseUrl + "&page=" + (page + 1));
        model.addAttribute("pageInfo", page + " / " + totalPages);
        return "staff/reception-list";
    }

    // 접수 상세
    @GetMapping("/detail")
    public String detail(@RequestParam("id") Long id, 
                        @RequestParam(name = "status", defaultValue = "") String status,
                        @RequestParam(name = "date", required = false) String date,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        Model model) {
        model.addAttribute("isStaffReception", true);
        model.addAttribute("detail", receptionService.getDetail(id));
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentDate", date != null ? date : "");
        model.addAttribute("currentPage", page);
        return "staff/reception-detail";
    }

    // 접수 처리
    @PostMapping("/receive")
    public String receive(@Valid @ModelAttribute ReceptionUpdateRequest request,
            RedirectAttributes redirectAttributes) {
        receptionService.receive(request);
        redirectAttributes.addFlashAttribute("message", "접수가 완료되었습니다.");

        redirectAttributes.addAttribute("status", request.getStatus());
        redirectAttributes.addAttribute("date", request.getDate());
        redirectAttributes.addAttribute("page", request.getPage());

        return "redirect:/staff/reception/list";
    }

    @PostMapping("/receive-ajax")
    @ResponseBody
    public Map<String, Object> receiveAjax(@RequestBody ReceptionUpdateRequest request) {

        receptionService.receive(request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("statusText", "진료 대기");
        result.put("statusClass", "bg-yellow-100 text-yellow-700");

        return result;
    }

    // 예약 취소
    @PostMapping("/cancel")
    public String cancel(@RequestParam("id") Long id, 
                        @RequestParam(name = "reason", defaultValue = "") String reason,
                        @RequestParam(name = "date", required = false) String date,
                        @RequestParam(name = "status", required = false) String status,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        RedirectAttributes redirectAttributes) {
        receptionService.cancel(id, reason);
        redirectAttributes.addFlashAttribute("message", "예약이 취소되었습니다.");
        
        // 보던 화면으로 돌아가기 위해 파라미터 전달
        redirectAttributes.addAttribute("date", date);
        redirectAttributes.addAttribute("status", status);
        redirectAttributes.addAttribute("page", page);
        
        return "redirect:/staff/reception/list";
    }

    @PostMapping("/cancel-ajax")
    @ResponseBody
    public Map<String, Object> cancelAjax(@RequestBody Map<String, Object> request) {

        Long id = Long.valueOf(request.get("id").toString());
        String reason = (String) request.getOrDefault("reason", "");

        Reservation r = receptionService.cancel(id, reason);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("newStatus", r.getStatus().name());
        
        // 프론트엔드 배지 업데이트를 위한 정보
        if (r.getStatus() == ReservationStatus.RESERVED) {
            result.put("statusText", "접수 대기");
            result.put("statusClass", "bg-blue-100 text-blue-700");
        } else if (r.getStatus() == ReservationStatus.CANCELLED) {
            result.put("statusText", "취소");
            result.put("statusClass", "bg-red-100 text-red-700");
        }

        return result;
    }

}
