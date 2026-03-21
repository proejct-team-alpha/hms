package com.smartclinic.hms.nurse;

import com.smartclinic.hms.item.ItemManagerService;
import com.smartclinic.hms.item.log.ItemUsageLogDto;
import com.smartclinic.hms.nurse.dto.NursePageLinkDto;
import com.smartclinic.hms.nurse.dto.NursePatientDto;
import com.smartclinic.hms.nurse.dto.NursePatientStatusDto;

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
import java.util.Objects;

/**
 * 간호사 접수 및 처치 관리 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/nurse")
public class NurseReceptionController {

    private final NurseService nurseService;
    private final ItemManagerService itemManagerService;

    /**
     * 환자 현황 목록 (전체 상태 확인)
     */
    @GetMapping("/reception-list")
    public String receptionList(@RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "deptId", required = false) Long deptId,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "source", required = false) String source,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        Page<NursePatientStatusDto> resultPage = nurseService.getReceptionPage(status, query, deptId, doctorId, source,
                page);

        StringBuilder baseUrlBuilder = new StringBuilder("/nurse/reception-list?");
        if (status != null && !status.isBlank())
            baseUrlBuilder.append("status=").append(status).append("&");
        if (query != null && !query.isBlank())
            baseUrlBuilder.append("query=").append(query).append("&");
        if (deptId != null)
            baseUrlBuilder.append("deptId=").append(deptId).append("&");
        if (doctorId != null)
            baseUrlBuilder.append("doctorId=").append(doctorId).append("&");
        if (source != null && !source.isBlank())
            baseUrlBuilder.append("source=").append(source).append("&");
        baseUrlBuilder.append("page=");
        String baseUrl = baseUrlBuilder.toString();

        int totalPages = resultPage.getTotalPages();
        List<NursePageLinkDto> pageLinks = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageLinks.add(new NursePageLinkDto(i + 1, i == page, baseUrl + i));
        }

        model.addAttribute("reservations", resultPage.getContent());
        model.addAttribute("statusFilters", nurseService.getStatusFilters(status, query, deptId, doctorId, source));
        model.addAttribute("currentStatus", status != null ? status : "");
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("deptId", deptId);
        model.addAttribute("doctorId", doctorId);
        model.addAttribute("source", source != null ? source : "");

        // 필터 데이터 (null 안전한 비교를 위해 Objects.equals 사용)
        model.addAttribute("departments", nurseService.getAllDepartments().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getName(), "selected", Objects.equals(d.getId(), deptId)))
                .toList());
        model.addAttribute("doctors", nurseService.getAllDoctors().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getDisplayName(), "selected", Objects.equals(d.getId(), doctorId)))
                .toList());
        model.addAttribute("sources", List.of(
                Map.of("value", "ONLINE", "label", "온라인", "selected", "ONLINE".equals(source)),
                Map.of("value", "PHONE", "label", "전화", "selected", "PHONE".equals(source)),
                Map.of("value", "WALKIN", "label", "방문", "selected", "WALKIN".equals(source))));
        model.addAttribute("hasPrev", page > 0);
        model.addAttribute("prevUrl", baseUrl + (page - 1));
        model.addAttribute("hasNext", page < totalPages - 1);
        model.addAttribute("nextUrl", baseUrl + (page + 1));
        model.addAttribute("pageLinks", pageLinks);
        model.addAttribute("pageTitle", "오늘 환자 현황");
        return "nurse/reception-list";
    }

    /**
     * 환자 상세 정보 조회
     */
    @GetMapping("/patient-detail")
    public String patientDetail(@RequestParam("id") Long id, Model model) {
        NursePatientDto detail = nurseService.getPatientDetail(id);
        model.addAttribute("detail", detail);
        model.addAttribute("items", itemManagerService.getItemList(null));
        model.addAttribute("usageLogs", itemManagerService.getUsageLogs(id));
        model.addAttribute("pageTitle", "환자 상세 정보");
        return "nurse/patient-detail";
    }

    /**
     * 물품 사용 처리 (AJAX)
     */
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

    /**
     * 환자 접수 처리
     */
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

    /**
     * 환자 정보 업데이트
     */
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

    /**
     * 처치 관리 목록 화면 (의사 진료 현황 스타일의 달력 + 리스트)
     */
    @GetMapping("/treatment-list")
    public String treatmentList(@RequestParam(name = "date", required = false) String dateStr,
                                @RequestParam(name = "tab", defaultValue = "pending") String tab,
                                @RequestParam(name = "query", required = false) String query,
                                Model model) {
        java.time.LocalDate date = (dateStr == null || dateStr.isBlank()) 
            ? java.time.LocalDate.now() 
            : java.time.LocalDate.parse(dateStr);

        java.time.LocalDate today = java.time.LocalDate.now();
        boolean isToday = date.equals(today);
        boolean isPastDate = date.isBefore(today);

        // 검색 및 탭 필터링 로직
        List<NursePatientStatusDto> allPatients = nurseService.getReceptionPage("COMPLETED", query, null, null, null, 0).getContent();

        List<NursePatientStatusDto> filteredPatients = allPatients.stream()
            .filter(p -> {
                if ("pending".equals(tab)) return !p.isTreatmentCompleted();
                if ("completed".equals(tab)) return p.isTreatmentCompleted();
                return true;
            })
            .toList();

        // 1부터 시작하는 순번 부여
        for (int i = 0; i < filteredPatients.size(); i++) {
            filteredPatients.get(i).setSequence(i + 1);
        }

        model.addAttribute("patients", filteredPatients);
        model.addAttribute("dailyTotalPatients", allPatients.size());
        model.addAttribute("totalPatients", filteredPatients.size());
        model.addAttribute("searchDate", date.toString());
        model.addAttribute("isToday", isToday);
        model.addAttribute("isPastDate", isPastDate);
        model.addAttribute("currentTab", tab);
        model.addAttribute("isPendingTab", "pending".equals(tab));
        model.addAttribute("isCompletedTab", "completed".equals(tab));
        model.addAttribute("isAllTab", "all".equals(tab));
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("items", itemManagerService.getItemList(null));
        model.addAttribute("pageTitle", "처치 관리");

        return "nurse/treatment-list";
    }

    /**
     * 간호사 처치 완료 처리
     */
    @PostMapping("/treatment/complete")
    public String completeTreatment(@RequestParam("id") Long id, RedirectAttributes ra) {
        try {
            nurseService.completeTreatment(id);
            ra.addFlashAttribute("message", "처치가 완료되었습니다. 이제 수납이 가능합니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nurse/treatment-list";
    }
}
