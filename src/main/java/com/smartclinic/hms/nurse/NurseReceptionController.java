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

@Controller
@RequestMapping("/nurse")
@RequiredArgsConstructor
public class NurseReceptionController {

    private final NurseService nurseService;
    private final ItemManagerService itemManagerService;

    /**
     * 환자 현황 목록 (전체 상태 확인)
     */
    @GetMapping("/reception-list")
    public String receptionList(@RequestParam(name = "date", required = false) String dateStr,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
            @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
            @RequestParam(name = "source", required = false) String source,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        
        java.time.LocalDate date = (dateStr == null || dateStr.isBlank()) 
            ? java.time.LocalDate.now() 
            : java.time.LocalDate.parse(dateStr);

        Page<NursePatientStatusDto> resultPage = nurseService.getReceptionPageWithMultiFilters(
                status, query, deptIds, doctorIds, source, page, date);

        StringBuilder baseUrlBuilder = new StringBuilder("/nurse/reception-list?");
        if (dateStr != null && !dateStr.isBlank()) baseUrlBuilder.append("date=").append(dateStr).append("&");
        if (status != null && !status.isBlank()) baseUrlBuilder.append("status=").append(status).append("&");
        if (query != null && !query.isBlank()) baseUrlBuilder.append("query=").append(query).append("&");
        if (deptIds != null) deptIds.forEach(id -> baseUrlBuilder.append("deptIds=").append(id).append("&"));
        if (doctorIds != null) doctorIds.forEach(id -> baseUrlBuilder.append("doctorIds=").append(id).append("&"));
        if (source != null && !source.isBlank()) baseUrlBuilder.append("source=").append(source).append("&");
        baseUrlBuilder.append("page=");
        String baseUrl = baseUrlBuilder.toString();

        int totalPages = resultPage.getTotalPages();
        List<NursePageLinkDto> pageLinks = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageLinks.add(new NursePageLinkDto(i + 1, i == page, baseUrl + i));
        }

        model.addAttribute("reservations", resultPage.getContent());
        model.addAttribute("statusFilters", nurseService.getStatusFilters(status, query, null, null, source));
        model.addAttribute("searchDate", date.toString());
        model.addAttribute("isToday", date.equals(java.time.LocalDate.now()));
        model.addAttribute("currentStatus", status != null ? status : "");
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("source", source != null ? source : "");

        // 상세 이동 시 유지를 위한 파라미터 조합
        StringBuilder keepParams = new StringBuilder();
        if (dateStr != null) keepParams.append("&date=").append(dateStr);
        if (status != null) keepParams.append("&status=").append(status);
        if (query != null) keepParams.append("&query=").append(query);
        if (deptIds != null) deptIds.forEach(id -> keepParams.append("&deptIds=").append(id));
        if (doctorIds != null) doctorIds.forEach(id -> keepParams.append("&doctorIds=").append(id));
        model.addAttribute("keepParams", keepParams.toString());

        model.addAttribute("departments", nurseService.getAllDepartments().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getName(), "selected", deptIds != null && deptIds.contains(d.getId())))
                .toList());
        model.addAttribute("doctors", nurseService.getAllDoctors().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getDisplayName(), "deptId", d.getDepartmentId(), "selected", doctorIds != null && doctorIds.contains(d.getId())))
                .toList());
        
        model.addAttribute("hasPrev", page > 0);
        model.addAttribute("prevUrl", baseUrl + (page - 1));
        model.addAttribute("hasNext", page < totalPages - 1);
        model.addAttribute("nextUrl", baseUrl + (page + 1));
        model.addAttribute("pageLinks", pageLinks);
        model.addAttribute("pageTitle", "환자 현황");
        return "nurse/reception-list";
    }

    /**
     * 환자 상세 정보 조회 (필터 상태 보존 - Null 안전 처리)
     */
    @GetMapping("/patient-detail")
    public String patientDetail(@RequestParam("id") Long id,
                                @RequestParam(name = "date", required = false) String date,
                                @RequestParam(name = "tab", required = false) String tab,
                                @RequestParam(name = "status", required = false) String status,
                                @RequestParam(name = "query", required = false) String query,
                                @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
                                @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
                                Model model) {
        NursePatientDto detail = nurseService.getPatientDetail(id);
        model.addAttribute("detail", detail);
        
        // null 방지를 위해 빈 문자열/리스트 처리
        model.addAttribute("keepDate", Objects.toString(date, ""));
        model.addAttribute("keepTab", Objects.toString(tab, ""));
        model.addAttribute("keepStatus", Objects.toString(status, ""));
        model.addAttribute("keepQuery", Objects.toString(query, ""));
        model.addAttribute("keepDeptIds", deptIds != null ? deptIds : List.of());
        model.addAttribute("keepDoctorIds", doctorIds != null ? doctorIds : List.of());

        model.addAttribute("filterDepartments", nurseService.getAllDepartments());
        model.addAttribute("filterDoctors", nurseService.getAllDoctors());
        model.addAttribute("items", itemManagerService.getItemList(null));
        model.addAttribute("usageLogs", itemManagerService.getUsageLogs(id));
        model.addAttribute("pageTitle", "환자 상세 정보");
        return "nurse/patient-detail";
    }

    /**
     * 환자 접수 처리 (필터 유지)
     */
    @PostMapping("/reservation/receive")
    public String receiveReservation(@RequestParam("id") Long id, 
                                     @RequestParam(name = "date", required = false) String date,
                                     @RequestParam(name = "status", required = false) String status,
                                     @RequestParam(name = "query", required = false) String query,
                                     @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
                                     @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
                                     RedirectAttributes ra) {
        try {
            nurseService.receiveReservation(id);
            ra.addFlashAttribute("message", "환자가 접수되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        if (date != null) ra.addAttribute("date", date);
        if (status != null) ra.addAttribute("status", status);
        if (query != null) ra.addAttribute("query", query);
        if (deptIds != null) ra.addAttribute("deptIds", deptIds);
        if (doctorIds != null) ra.addAttribute("doctorIds", doctorIds);
        return "redirect:/nurse/reception-list";
    }

    /**
     * 환자 정보 업데이트 (필터 유지)
     */
    @PostMapping("/patient/update")
    public String updatePatient(@RequestParam("patientId") Long patientId,
                                @RequestParam("reservationId") Long reservationId,
                                @RequestParam("phone") String phone,
                                @RequestParam(name = "address", required = false, defaultValue = "") String address,
                                @RequestParam(name = "note", required = false, defaultValue = "") String note,
                                @RequestParam(name = "date", required = false) String date,
                                @RequestParam(name = "tab", required = false) String tab,
                                @RequestParam(name = "query", required = false) String query,
                                @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
                                @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
                                RedirectAttributes ra) {
        try {
            nurseService.updatePatient(patientId, phone, address, note);
            ra.addFlashAttribute("message", "환자 정보가 저장되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        ra.addAttribute("id", reservationId);
        if (date != null) ra.addAttribute("date", date);
        if (tab != null) ra.addAttribute("tab", tab);
        if (query != null) ra.addAttribute("query", query);
        if (deptIds != null) ra.addAttribute("deptIds", deptIds);
        if (doctorIds != null) ra.addAttribute("doctorIds", doctorIds);
        return "redirect:/nurse/patient-detail";
    }

    /**
     * 처치 관리 목록 화면
     */
    @GetMapping("/treatment-list")
    public String treatmentList(@RequestParam(name = "date", required = false) String dateStr,
                                @RequestParam(name = "tab", defaultValue = "pending") String tab,
                                @RequestParam(name = "query", required = false) String query,
                                @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
                                @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
                                @RequestParam(name = "page", defaultValue = "0") int page,
                                Model model) {
        java.time.LocalDate date = (dateStr == null || dateStr.isBlank()) 
            ? java.time.LocalDate.now() 
            : java.time.LocalDate.parse(dateStr);

        Page<NursePatientStatusDto> resultPage = nurseService.getReceptionPageWithMultiFilters(
                "COMPLETED", query, deptIds, doctorIds, null, page, date);

        List<NursePatientStatusDto> filteredContent = resultPage.getContent().stream()
            .filter(p -> {
                if ("pending".equals(tab)) return !p.isTreatmentCompleted();
                if ("completed".equals(tab)) return p.isTreatmentCompleted();
                return true;
            })
            .toList();

        int startSeq = page * 10 + 1;
        for (int i = 0; i < filteredContent.size(); i++) {
            filteredContent.get(i).setSequence(startSeq + i);
        }

        StringBuilder baseUrlBuilder = new StringBuilder("/nurse/treatment-list?");
        if (dateStr != null && !dateStr.isBlank()) baseUrlBuilder.append("date=").append(dateStr).append("&");
        if (tab != null) baseUrlBuilder.append("tab=").append(tab).append("&");
        if (query != null && !query.isBlank()) baseUrlBuilder.append("query=").append(query).append("&");
        if (deptIds != null) deptIds.forEach(id -> baseUrlBuilder.append("deptIds=").append(id).append("&"));
        if (doctorIds != null) doctorIds.forEach(id -> baseUrlBuilder.append("doctorIds=").append(id).append("&"));
        baseUrlBuilder.append("page=");
        String baseUrl = baseUrlBuilder.toString();

        int totalPages = resultPage.getTotalPages();
        List<NursePageLinkDto> pageLinks = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageLinks.add(new NursePageLinkDto(i + 1, i == page, baseUrl + i));
        }

        model.addAttribute("patients", filteredContent);
        model.addAttribute("totalPatients", resultPage.getTotalElements());
        model.addAttribute("searchDate", date.toString());
        model.addAttribute("isToday", date.equals(java.time.LocalDate.now()));
        model.addAttribute("currentTab", tab);
        model.addAttribute("isPendingTab", "pending".equals(tab));
        model.addAttribute("isCompletedTab", "completed".equals(tab));
        model.addAttribute("isAllTab", "all".equals(tab));
        model.addAttribute("query", query != null ? query : "");
        
        StringBuilder keepParams = new StringBuilder();
        if (dateStr != null) keepParams.append("&date=").append(dateStr);
        if (tab != null) keepParams.append("&tab=").append(tab);
        if (query != null) keepParams.append("&query=").append(query);
        if (deptIds != null) deptIds.forEach(id -> keepParams.append("&deptIds=").append(id));
        if (doctorIds != null) doctorIds.forEach(id -> keepParams.append("&doctorIds=").append(id));
        model.addAttribute("keepParams", keepParams.toString());

        model.addAttribute("departments", nurseService.getAllDepartments().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getName(), "selected", deptIds != null && deptIds.contains(d.getId())))
                .toList());
        model.addAttribute("doctors", nurseService.getAllDoctors().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getDisplayName(), "deptId", d.getDepartmentId(), "selected", doctorIds != null && doctorIds.contains(d.getId())))
                .toList());

        model.addAttribute("hasPrev", page > 0);
        model.addAttribute("prevUrl", baseUrl + (page - 1));
        model.addAttribute("hasNext", page < totalPages - 1);
        model.addAttribute("nextUrl", baseUrl + (page + 1));
        model.addAttribute("pageLinks", pageLinks);
        model.addAttribute("pageTitle", "처치 관리");

        return "nurse/treatment-list";
    }

    /**
     * 간호 처치 메모 저장 (필터 유지)
     */
    @PostMapping("/treatment/save-note")
    public String saveNurseNote(@RequestParam("id") Long id,
                                @RequestParam("nurseNote") String nurseNote,
                                @RequestParam(name = "date", required = false) String date,
                                @RequestParam(name = "tab", required = false) String tab,
                                @RequestParam(name = "query", required = false) String query,
                                @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
                                @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
                                RedirectAttributes ra) {
        try {
            nurseService.saveNurseNote(id, nurseNote);
            ra.addFlashAttribute("message", "간호 메모가 저장되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        ra.addAttribute("id", id);
        if (date != null) ra.addAttribute("date", date);
        if (tab != null) ra.addAttribute("tab", tab);
        if (query != null) ra.addAttribute("query", query);
        if (deptIds != null) ra.addAttribute("deptIds", deptIds);
        if (doctorIds != null) ra.addAttribute("doctorIds", doctorIds);
        return "redirect:/nurse/patient-detail";
    }

    /**
     * 간호사 처치 완료 처리 (필터 유지)
     */
    @PostMapping("/treatment/complete")
    public String completeTreatment(@RequestParam("id") Long id,
                                    @RequestParam(name = "date", required = false) String date,
                                    @RequestParam(name = "tab", required = false) String tab,
                                    @RequestParam(name = "query", required = false) String query,
                                    @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
                                    @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
                                    RedirectAttributes ra) {
        try {
            nurseService.completeTreatment(id);
            ra.addFlashAttribute("message", "처치가 완료되었습니다. 이제 수납이 가능합니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        if (date != null) ra.addAttribute("date", date);
        if (tab != null) ra.addAttribute("tab", tab);
        if (query != null) ra.addAttribute("query", query);
        if (deptIds != null) ra.addAttribute("deptIds", deptIds);
        if (doctorIds != null) ra.addAttribute("doctorIds", doctorIds);
        return "redirect:/nurse/treatment-list";
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
}
