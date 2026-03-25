package com.smartclinic.hms.nurse;

import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.item.ItemManagerService;
import com.smartclinic.hms.item.log.ItemUsageLogDto;
import com.smartclinic.hms.nurse.dto.NursePageLinkDto;
import com.smartclinic.hms.nurse.dto.NursePatientDto;
import com.smartclinic.hms.nurse.dto.NursePatientStatusDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
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
     * 환자 현황 목록 (세션 필터 유지 - 람다 에러 수리 완료)
     */
    @GetMapping("/reception-list")
    public String receptionList(@RequestParam(name = "date", required = false) String dateStr,
            @RequestParam(name = "tab", defaultValue = "all") String tab,
            @RequestParam(name = "query", required = false) String queryParam,
            @RequestParam(name = "deptIds", required = false) List<Long> deptIdsParam,
            @RequestParam(name = "doctorIds", required = false) List<Long> doctorIdsParam,
            @RequestParam(name = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        // [수리] 파라미터 재할당 방지를 위한 로컬 변수 사용
        String query = queryParam;
        if (query == null && session.getAttribute("filterQuery") != null) {
            query = (String) session.getAttribute("filterQuery");
        }
        
        @SuppressWarnings("unchecked")
        List<Long> deptIds = deptIdsParam != null ? deptIdsParam : (List<Long>) session.getAttribute("filterDeptIds");
        @SuppressWarnings("unchecked")
        List<Long> doctorIds = doctorIdsParam != null ? doctorIdsParam : (List<Long>) session.getAttribute("filterDoctorIds");

        // 세션 갱신
        session.setAttribute("filterQuery", query);
        session.setAttribute("filterDeptIds", deptIds);
        session.setAttribute("filterDoctorIds", doctorIds);

        LocalDate date = (dateStr == null || dateStr.isBlank()) ? LocalDate.now() : LocalDate.parse(dateStr);

        String dbStatus = switch (tab) {
            case "reserved" -> "RESERVED";
            case "received" -> "RECEIVED";
            case "in_treatment" -> "IN_TREATMENT";
            case "completed", "treatment_done" -> "COMPLETED";
            default -> null;
        };

        Page<NursePatientStatusDto> resultPage = nurseService.getReceptionPageWithMultiFilters(
                dbStatus, query, deptIds, doctorIds, null, page, date);

        List<NursePatientStatusDto> content = resultPage.getContent().stream()
            .filter(p -> {
                if ("completed".equals(tab)) return !p.isTreatmentCompleted();
                if ("treatment_done".equals(tab)) return p.isTreatmentCompleted();
                return true;
            })
            .toList();

        // 람다에서 사용할 '사실상 final' 변수들
        final List<Long> finalDeptIds = deptIds;
        final List<Long> finalDoctorIds = doctorIds;

        StringBuilder baseUrlBuilder = new StringBuilder("/nurse/reception-list?");
        if (dateStr != null) baseUrlBuilder.append("date=").append(dateStr).append("&");
        baseUrlBuilder.append("tab=").append(tab).append("&");
        if (query != null) baseUrlBuilder.append("query=").append(query).append("&");
        if (finalDeptIds != null) finalDeptIds.forEach(id -> baseUrlBuilder.append("deptIds=").append(id).append("&"));
        if (finalDoctorIds != null) finalDoctorIds.forEach(id -> baseUrlBuilder.append("doctorIds=").append(id).append("&"));
        baseUrlBuilder.append("page=");
        String baseUrl = baseUrlBuilder.toString();

        int totalPages = resultPage.getTotalPages();
        List<NursePageLinkDto> pageLinks = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageLinks.add(new NursePageLinkDto(i + 1, i == page, baseUrl + i));
        }

        model.addAttribute("reservations", content);
        model.addAttribute("searchDate", date.toString());
        model.addAttribute("isToday", date.equals(LocalDate.now()));
        model.addAttribute("currentTab", tab);
        model.addAttribute("query", Objects.toString(query, ""));

        model.addAttribute("isAllTab", "all".equals(tab));
        model.addAttribute("isReservedTab", "reserved".equals(tab));
        model.addAttribute("isReceivedTab", "received".equals(tab));
        model.addAttribute("isInTreatmentTab", "in_treatment".equals(tab));
        model.addAttribute("isCompletedTab", "completed".equals(tab));
        model.addAttribute("isTreatmentDoneTab", "treatment_done".equals(tab));
        model.addAttribute("isPaidTab", "paid".equals(tab));

        StringBuilder keepParams = new StringBuilder();
        keepParams.append("&date=").append(date.toString());
        keepParams.append("&tab=").append(tab);
        if (query != null) keepParams.append("&query=").append(query);
        if (finalDeptIds != null) finalDeptIds.forEach(id -> keepParams.append("&deptIds=").append(id));
        if (finalDoctorIds != null) finalDoctorIds.forEach(id -> keepParams.append("&doctorIds=").append(id));
        model.addAttribute("keepParams", keepParams.toString());

        model.addAttribute("departments", nurseService.getAllDepartments().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getName(), "selected", finalDeptIds != null && finalDeptIds.contains(d.getId())))
                .toList());
        model.addAttribute("doctors", nurseService.getAllDoctors().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getDisplayName(), "deptId", d.getDepartmentId(), "selected", finalDoctorIds != null && finalDoctorIds.contains(d.getId())))
                .toList());

        model.addAttribute("hasPrev", page > 0);
        model.addAttribute("prevUrl", baseUrl + (page - 1));
        model.addAttribute("hasNext", page < totalPages - 1);
        model.addAttribute("nextUrl", baseUrl + (page + 1));
        model.addAttribute("pageLinks", pageLinks);
        model.addAttribute("pageTitle", "환자 현황");
        model.addAttribute("isNurseReception", true);
        return "nurse/reception-list";
    }

    /**
     * 처치 관리 목록 (세션 필터 유지 - 람다 에러 수리 완료)
     */
    @GetMapping("/treatment-list")
    public String treatmentList(@RequestParam(name = "date", required = false) String dateStr,
                                @RequestParam(name = "tab", defaultValue = "pending") String tab,
                                @RequestParam(name = "query", required = false) String queryParam,
                                @RequestParam(name = "deptIds", required = false) List<Long> deptIdsParam,
                                @RequestParam(name = "doctorIds", required = false) List<Long> doctorIdsParam,
                                @RequestParam(name = "page", defaultValue = "0") int page,
                                HttpSession session,
                                Model model) {
        
        String query = queryParam;
        if (query == null && session.getAttribute("filterQuery") != null) {
            query = (String) session.getAttribute("filterQuery");
        }
        
        @SuppressWarnings("unchecked")
        List<Long> deptIds = deptIdsParam != null ? deptIdsParam : (List<Long>) session.getAttribute("filterDeptIds");
        @SuppressWarnings("unchecked")
        List<Long> doctorIds = doctorIdsParam != null ? doctorIdsParam : (List<Long>) session.getAttribute("filterDoctorIds");

        session.setAttribute("filterQuery", query);
        session.setAttribute("filterDeptIds", deptIds);
        session.setAttribute("filterDoctorIds", doctorIds);

        LocalDate date = (dateStr == null || dateStr.isBlank()) ? LocalDate.now() : LocalDate.parse(dateStr);

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

        final List<Long> finalDeptIds = deptIds;
        final List<Long> finalDoctorIds = doctorIds;

        StringBuilder baseUrlBuilder = new StringBuilder("/nurse/treatment-list?");
        if (dateStr != null && !dateStr.isBlank()) baseUrlBuilder.append("date=").append(dateStr).append("&");
        if (tab != null) baseUrlBuilder.append("tab=").append(tab).append("&");
        if (query != null) baseUrlBuilder.append("query=").append(query).append("&");
        if (finalDeptIds != null) finalDeptIds.forEach(id -> baseUrlBuilder.append("deptIds=").append(id).append("&"));
        if (finalDoctorIds != null) finalDoctorIds.forEach(id -> baseUrlBuilder.append("doctorIds=").append(id).append("&"));
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
        model.addAttribute("isToday", date.equals(LocalDate.now()));
        model.addAttribute("currentTab", tab);
        model.addAttribute("isPendingTab", "pending".equals(tab));
        model.addAttribute("isCompletedTab", "completed".equals(tab));
        model.addAttribute("isAllTab", "all".equals(tab));
        model.addAttribute("query", query != null ? query : "");
        
        StringBuilder keepParams = new StringBuilder();
        if (dateStr != null) keepParams.append("&date=").append(dateStr);
        if (tab != null) keepParams.append("&tab=").append(tab);
        if (query != null) keepParams.append("&query=").append(query);
        if (finalDeptIds != null) finalDeptIds.forEach(id -> keepParams.append("&deptIds=").append(id));
        if (finalDoctorIds != null) finalDoctorIds.forEach(id -> keepParams.append("&doctorIds=").append(id));
        model.addAttribute("keepParams", keepParams.toString());

        model.addAttribute("departments", nurseService.getAllDepartments().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getName(), "selected", finalDeptIds != null && finalDeptIds.contains(d.getId())))
                .toList());
        model.addAttribute("doctors", nurseService.getAllDoctors().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getDisplayName(), "deptId", d.getDepartmentId(), "selected", finalDoctorIds != null && finalDoctorIds.contains(d.getId())))
                .toList());

        model.addAttribute("hasPrev", page > 0);
        model.addAttribute("prevUrl", baseUrl + (page - 1));
        model.addAttribute("hasNext", page < totalPages - 1);
        model.addAttribute("nextUrl", baseUrl + (page + 1));
        model.addAttribute("pageLinks", pageLinks);
        model.addAttribute("pageTitle", "처치 관리");
        model.addAttribute("isNurseTreatment", true);

        return "nurse/treatment-list";
    }

    /**
     * 환자 상세 정보 조회 (사이드바 업무 맥락 유지)
     */
    @GetMapping("/patient-detail")
    public String patientDetail(@RequestParam("id") Long id,
                                @RequestParam(name = "menu", required = false) String menu,
                                @RequestParam(name = "date", required = false) String date,
                                @RequestParam(name = "tab", required = false) String tab,
                                @RequestParam(name = "status", required = false) String status,
                                @RequestParam(name = "query", required = false) String query,
                                @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
                                @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
                                HttpSession session,
                                Model model) {
        NursePatientDto detail = nurseService.getPatientDetail(id);
        model.addAttribute("detail", detail);
        
        if (query != null) session.setAttribute("filterQuery", query);
        if (deptIds != null) session.setAttribute("filterDeptIds", deptIds);
        if (doctorIds != null) session.setAttribute("filterDoctorIds", doctorIds);

        if ("reception".equals(menu)) {
            model.addAttribute("isNurseReception", true);
        } else {
            model.addAttribute("isNurseTreatment", true);
        }
        model.addAttribute("currentMenu", menu);

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

    @PostMapping("/patient/update")
    public String updatePatient(@RequestParam("patientId") Long patientId,
                                @RequestParam("reservationId") Long reservationId,
                                @RequestParam("phone") String phone,
                                @RequestParam(name = "address", required = false, defaultValue = "") String address,
                                @RequestParam(name = "note", required = false, defaultValue = "") String note,
                                @RequestParam(name = "menu", required = false) String menu,
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
        if (menu != null) ra.addAttribute("menu", menu);
        if (date != null) ra.addAttribute("date", date);
        if (tab != null) ra.addAttribute("tab", tab);
        if (query != null) ra.addAttribute("query", query);
        if (deptIds != null) ra.addAttribute("deptIds", deptIds);
        if (doctorIds != null) ra.addAttribute("doctorIds", doctorIds);
        return "redirect:/nurse/patient-detail";
    }

    @PostMapping("/treatment/save-note")
    public String saveNurseNote(@RequestParam("id") Long id,
                                @RequestParam("nurseNote") String nurseNote,
                                @RequestParam(name = "menu", required = false) String menu,
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
        if (menu != null) ra.addAttribute("menu", menu);
        if (date != null) ra.addAttribute("date", date);
        if (tab != null) ra.addAttribute("tab", tab);
        if (query != null) ra.addAttribute("query", query);
        if (deptIds != null) ra.addAttribute("deptIds", deptIds);
        if (doctorIds != null) ra.addAttribute("doctorIds", doctorIds);
        return "redirect:/nurse/patient-detail";
    }

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
    public ResponseEntity<?> cancelUsage(@RequestParam("logId") Long logId,
                                         @RequestParam("reservationId") Long reservationId) {
        try {
            itemManagerService.cancelItemUsage(logId);
            List<ItemUsageLogDto> logs = itemManagerService.getUsageLogs(reservationId);
            return ResponseEntity.ok(Map.of("success", true, "logs", logs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
