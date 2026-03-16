package com.smartclinic.hms.staff.reception;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String list(@RequestParam(name = "status", defaultValue = "") String status,
                       @RequestParam(name = "date", required = false) String date,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       Model model) {
        // date 없으면 null → 오늘 이후 전체 조회
        LocalDate selectedDate = (date == null || date.isBlank()) ? null : LocalDate.parse(date);
        String dateStr = selectedDate != null ? selectedDate.toString() : "";

        List<StaffReservationDto> all = receptionService.getReservations(selectedDate, status);

        // 페이징
        int total = all.size();
        int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(1, Math.min(page, totalPages));
        int from = (page - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        List<StaffReservationDto> paged = all.subList(from, to);

        // 페이지 URL 기본 경로 (date, status 포함)
        String baseUrl = "/staff/reception/list?date=" + dateStr + "&status=" + status;

        model.addAttribute("reservations", paged);
        model.addAttribute("filters", receptionService.getStatusFilters(status, dateStr));
        // 날짜 네비게이션
        model.addAttribute("hasDate", selectedDate != null);
        model.addAttribute("todayDate", LocalDate.now().toString());
        model.addAttribute("selectedDate", selectedDate != null ? selectedDate.toString() : "오늘 이후 전체");
        model.addAttribute("prevDate", selectedDate != null ? selectedDate.minusDays(1).toString() : "");
        model.addAttribute("nextDate", selectedDate != null ? selectedDate.plusDays(1).toString() : "");
        model.addAttribute("currentStatus", status);
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
    public String detail(@RequestParam Long id, Model model) {
        model.addAttribute("detail", receptionService.getDetail(id));
        return "staff/reception-detail";
    }

    // 접수 처리
    @PostMapping("/receive")
    public String receive(@Valid @ModelAttribute ReceptionUpdateRequest request,
            RedirectAttributes redirectAttributes) {
        receptionService.receive(request);
        redirectAttributes.addFlashAttribute("message", "접수가 완료되었습니다.");
        return "redirect:/staff/reception/list";
    }

    // 예약 취소
    @PostMapping("/cancel")
    public String cancel(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        receptionService.cancel(id);
        redirectAttributes.addFlashAttribute("message", "예약이 취소되었습니다.");
        return "redirect:/staff/reception/list";
    }
}
