package com.smartclinic.hms.nurse;

import com.smartclinic.hms.common.util.Resp;
import com.smartclinic.hms.item.ItemManagerService;
import com.smartclinic.hms.item.log.ItemUsageLogDto;
import com.smartclinic.hms.nurse.dto.NursePageLinkDto;
import com.smartclinic.hms.nurse.dto.NursePatientDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/nurse")
public class NurseReceptionController {

    private final NurseService nurseService;
    private final ItemManagerService itemManagerService;

    @GetMapping("/reception-list")
    public String receptionList(@RequestParam(name = "status", required = false) String status,
                                @RequestParam(name = "query", required = false) String query,
                                @RequestParam(name = "deptId", required = false) Long deptId,
                                @RequestParam(name = "doctorId", required = false) Long doctorId,
                                @RequestParam(name = "source", required = false) String source,
                                @RequestParam(name = "page", defaultValue = "0") int page,
                                Model model) {
        Page<com.smartclinic.hms.nurse.dto.NurseReservationDto> resultPage =
                nurseService.getReceptionPage(status, query, deptId, doctorId, source, page);

        StringBuilder baseUrlBuilder = new StringBuilder("/nurse/reception-list?");
        if (status != null && !status.isBlank()) baseUrlBuilder.append("status=").append(status).append("&");
        if (query != null && !query.isBlank()) baseUrlBuilder.append("query=").append(query).append("&");
        if (deptId != null) baseUrlBuilder.append("deptId=").append(deptId).append("&");
        if (doctorId != null) baseUrlBuilder.append("doctorId=").append(doctorId).append("&");
        if (source != null && !source.isBlank()) baseUrlBuilder.append("source=").append(source).append("&");
        baseUrlBuilder.append("page=");
        String baseUrl = baseUrlBuilder.toString();

        int totalPages = resultPage.getTotalPages();
        List<NursePageLinkDto> pageLinks = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageLinks.add(new NursePageLinkDto(i + 1, i == page, baseUrl + i));
        }

        model.addAttribute("reservations", resultPage.getContent());
        model.addAttribute("statusFilters", nurseService.getStatusFilters(status, query, deptId, doctorId, source));
        model.addAttribute("currentStatus", status);
        model.addAttribute("query", query);
        model.addAttribute("deptId", deptId);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("source", source);
        
        // 필터 데이터
        model.addAttribute("departments", nurseService.getAllDepartments().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getName(), "selected", d.getId().equals(deptId)))
                .toList());
        model.addAttribute("doctors", nurseService.getAllDoctors().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getDisplayName(), "selected", d.getId().equals(doctorId)))
                .toList());
        model.addAttribute("sources", List.of(
            Map.of("value", "ONLINE", "label", "온라인", "selected", "ONLINE".equals(source)),
            Map.of("value", "PHONE", "label", "전화", "selected", "PHONE".equals(source)),
            Map.of("value", "WALKIN", "label", "방문", "selected", "WALKIN".equals(source))
        ));
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
        model.addAttribute("items", itemManagerService.getItemList(null));
        model.addAttribute("usageLogs", itemManagerService.getUsageLogs(id));
        model.addAttribute("pageTitle", "환자 상세 정보");
        return "nurse/patient-detail";
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
