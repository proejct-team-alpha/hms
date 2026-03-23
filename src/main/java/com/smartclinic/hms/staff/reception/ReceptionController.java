package com.smartclinic.hms.staff.reception;

import java.util.HashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.smartclinic.hms.staff.reception.dto.PatientInfoUpdateRequest;
import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 원무과 접수 관리 컨트롤러
 * Nurse 스타일의 현대화된 목록 UI와 Staff 전용 기능을 제공합니다.
 */
@Controller
@RequestMapping("/staff/reception")
@RequiredArgsConstructor
public class ReceptionController {

    private final ReceptionService receptionService;

    private static final int PAGE_SIZE = 10;

    // 접수 목록 (현대화된 버전 - 다중 필터 및 탭 지원)
    @GetMapping("/list")
    public String list(
            @RequestParam(name = "tab", defaultValue = "all") String tab,
            @RequestParam(name = "date", required = false) String dateStr,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
            @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
            @RequestParam(name = "source", required = false) String source,
            @ModelAttribute("date") String flashDate,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {

        // 중복 파라미터 방어 로직: 쉼표가 포함되어 있으면 첫 번째 값만 사용
        if (dateStr != null && dateStr.contains(",")) dateStr = dateStr.split(",")[0];
        if (tab != null && tab.contains(",")) tab = tab.split(",")[0];

        if ((dateStr == null || dateStr.isBlank()) && flashDate != null && !flashDate.isBlank()) {
            dateStr = flashDate;
        }
        model.addAttribute("isStaffReception", true);
        
        // 날짜 처리 (기본값: 오늘)
        LocalDate selectedDate = (dateStr == null || dateStr.isBlank()) 
            ? LocalDate.now() 
            : LocalDate.parse(dateStr);
        String finalDateStr = selectedDate.toString();

        // 요일 정보 추가 (월, 화, 수...)
        String dayOfWeek = selectedDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.KOREAN);
        String formattedDate = finalDateStr + " (" + dayOfWeek + ")";
        model.addAttribute("formattedDate", formattedDate);

        // 탭에 따른 실제 DB 상태값 매핑 (Nurse 컨트롤러 스타일)
        String dbStatus = switch (tab) {
            case "reserved" -> "RESERVED";
            case "received" -> "RECEIVED";
            case "in_treatment" -> "IN_TREATMENT";
            case "completed", "treatment_done" -> "COMPLETED";
            case "cancelled" -> "CANCELLED";
            default -> null; // 전체보기 (all)
        };

        // 데이터 조회 (다중 필터 적용)
        List<StaffReservationDto> all = receptionService.getReservations(selectedDate, dbStatus, query, deptIds, doctorIds, source);

        // 탭별 정밀 필터링 (Nurse 로직 참고)
        List<StaffReservationDto> filtered = all.stream()
            .filter(r -> {
                // 현재 StaffReservationDto는 처치 완료 여부 필드가 없으므로 상태값으로만 필터링
                return true;
            })
            .collect(Collectors.toList());

        // 페이징 처리
        int total = filtered.size();
        int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int finalPage = Math.max(1, Math.min(page, totalPages));
        int from = (finalPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        List<StaffReservationDto> paged = filtered.subList(from, to);

        // 검색/필터 유지를 위한 파라미터 구성 (date와 tab은 링크에서 직접 처리하므로 제외)
        StringBuilder keepParams = new StringBuilder();
        if (query != null && !query.isBlank()) keepParams.append("&query=").append(query);
        if (deptIds != null) deptIds.forEach(id -> keepParams.append("&deptIds=").append(id));
        if (doctorIds != null) doctorIds.forEach(id -> keepParams.append("&doctorIds=").append(id));
        if (source != null && !source.isBlank()) keepParams.append("&source=").append(source);
        String params = keepParams.toString();

        model.addAttribute("reservations", paged);
        model.addAttribute("searchDate", finalDateStr);
        model.addAttribute("isToday", selectedDate.equals(LocalDate.now()));
        model.addAttribute("currentTab", tab);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("keepParams", params);

        // 탭 활성화 상태 전달
        model.addAttribute("isAllTab", "all".equals(tab));
        model.addAttribute("isReservedTab", "reserved".equals(tab));
        model.addAttribute("isReceivedTab", "received".equals(tab));
        model.addAttribute("isInTreatmentTab", "in_treatment".equals(tab));
        model.addAttribute("isCompletedTab", "completed".equals(tab));
        model.addAttribute("isTreatmentDoneTab", "treatment_done".equals(tab));
        model.addAttribute("isPaidTab", "paid".equals(tab));
        model.addAttribute("isCancelledTab", "cancelled".equals(tab));

        // 필터 옵션 데이터
        model.addAttribute("departments", receptionService.getAllDepartments().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getName(), "selected", deptIds != null && deptIds.contains(d.getId())))
                .collect(Collectors.toList()));
        model.addAttribute("doctors", receptionService.getAllDoctors().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getDisplayName(), "deptId", d.getDepartmentId(), "selected", doctorIds != null && doctorIds.contains(d.getId())))
                .collect(Collectors.toList()));
        model.addAttribute("sources", List.of(
            Map.of("value", "ONLINE", "label", "온라인", "selected", "ONLINE".equals(source)),
            Map.of("value", "PHONE", "label", "전화", "selected", "PHONE".equals(source)),
            Map.of("value", "WALKIN", "label", "방문", "selected", "WALKIN".equals(source))
        ));

        // 페이징 정보
        model.addAttribute("currentPage", finalPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", total);
        model.addAttribute("hasPrev", finalPage > 1);
        model.addAttribute("hasNext", finalPage < totalPages);
        model.addAttribute("prevPageUrl", "/staff/reception/list?page=" + (finalPage - 1) + params);
        model.addAttribute("nextPageUrl", "/staff/reception/list?page=" + (finalPage + 1) + params);
        model.addAttribute("pageInfo", finalPage + " / " + totalPages);

        return "staff/reception-list";
    }

    // 접수 상세
    @GetMapping("/detail")
    public String detail(@RequestParam("id") Long id, 
                        @RequestParam(name = "tab", defaultValue = "all") String tab,
                        @RequestParam(name = "date", required = false) String date,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        @RequestParam(name = "query", required = false) String query,
                        @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
                        @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
                        @RequestParam(name = "source", required = false) String source,
                        Model model) {
        
        // [주석] 중복 파라미터 방어 (쉼표로 구분된 문자열 등이 들어올 경우 대비)
        if (date != null && date.contains(",")) date = date.split(",")[0];
        if (tab != null && tab.contains(",")) tab = tab.split(",")[0];
        if (source != null && source.contains(",")) source = source.split(",")[0];

        model.addAttribute("isStaffReception", true);
        StaffReservationDto dto = receptionService.getDetail(id);
        model.addAttribute("detail", dto);
        model.addAttribute("currentTab", tab);
        model.addAttribute("currentDate", date != null ? date : "");
        model.addAttribute("currentPage", page);
        
        // [주석] 목록 필터링 상태 유지를 위한 파라미터 추가
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("deptIds", deptIds);
        model.addAttribute("doctorIds", doctorIds);
        model.addAttribute("source", source != null ? source : "");

        // 상세 화면 수정을 위한 리스트 데이터 추가
        model.addAttribute("departments", receptionService.getAllDepartments());
        model.addAttribute("doctors", receptionService.getAllDoctors().stream()
                .map(d -> Map.of("id", d.getId(), "name", d.getDisplayName(), "deptId", d.getDepartmentId()))
                .collect(Collectors.toList()));

        return "staff/reception-detail";
    }

    // 접수 처리 (폼 제출용)
    @PostMapping("/receive")
    public String receive(@Valid @ModelAttribute ReceptionUpdateRequest request,
                        @RequestParam(name = "tab", defaultValue = "all") String tab,
                        RedirectAttributes redirectAttributes) {
        receptionService.receive(request);
        redirectAttributes.addFlashAttribute("message", "접수가 완료되었습니다.");

        redirectAttributes.addAttribute("tab", tab);
        redirectAttributes.addAttribute("date", request.getDate());
        redirectAttributes.addAttribute("page", request.getPage());

        return "redirect:/staff/reception/list";
    }

    // 접수 처리 (비동기 AJAX용)
    @PostMapping("/receive-ajax")
    @ResponseBody
    public Map<String, Object> receiveAjax(@RequestBody ReceptionUpdateRequest request) {
        receptionService.receive(request);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("statusText", "진료 대기");
        result.put("statusClass", "bg-orange-100 text-orange-700");
        return result;
    }

    // 예약 취소
    @PostMapping("/cancel")
    public String cancel(@RequestParam("id") Long id, 
                        @RequestParam(name = "reason", defaultValue = "") String reason,
                        @RequestParam(name = "date", required = false) String date,
                        @RequestParam(name = "tab", defaultValue = "all") String tab,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        RedirectAttributes redirectAttributes) {
        receptionService.cancel(id, reason);
        redirectAttributes.addFlashAttribute("message", "예약이 취소되었습니다.");
        
        redirectAttributes.addAttribute("date", date);
        redirectAttributes.addAttribute("tab", tab);
        redirectAttributes.addAttribute("page", page);
        
        return "redirect:/staff/reception/list";
    }

    // 예약 취소 (비동기 AJAX용)
    @PostMapping("/cancel-ajax")
    @ResponseBody
    public Map<String, Object> cancelAjax(@RequestBody Map<String, Object> request) {
        Long id = Long.valueOf(request.get("id").toString());
        String reason = (String) request.getOrDefault("reason", "");

        Reservation r = receptionService.cancel(id, reason);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("newStatus", r.getStatus().name());
        
        if (r.getStatus() == ReservationStatus.RESERVED) {
            result.put("statusText", "예약");
            result.put("statusClass", "bg-blue-100 text-blue-700");
        } else if (r.getStatus() == ReservationStatus.CANCELLED) {
            result.put("statusText", "취소");
            result.put("statusClass", "bg-red-100 text-red-700");
        }
        return result;
    }

    // 환자 정보 수정 (주소, 특이사항)
    @PostMapping("/update-patient-info")
    @ResponseBody
    public Map<String, Object> updatePatientInfo(@RequestBody @Valid PatientInfoUpdateRequest request) {
        receptionService.updatePatientInfo(request);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "환자 정보가 수정되었습니다.");
        return result;
    }
}
