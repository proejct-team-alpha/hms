package com.smartclinic.hms.staff.reception;

import java.util.HashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
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
 */
@Controller
@RequestMapping("/staff/reception")
@RequiredArgsConstructor
public class ReceptionController {

    private final ReceptionService receptionService;

    @GetMapping("/list")
    public String list(
            @RequestParam(name = "tab", defaultValue = "all") String tabParam,
            @RequestParam(name = "date", required = false) String dateStr,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
            @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
            @RequestParam(name = "source", required = false) String source,
            @ModelAttribute("date") String flashDate,
            Model model) {

        // 중복 파라미터 방어 및 변수명 명확화 (tabParam 사용)
        String currentTab = tabParam;
        if (currentTab != null && currentTab.contains(",")) {
            currentTab = currentTab.split(",")[0];
        }

        String searchDateStr = dateStr;
        if (searchDateStr != null && searchDateStr.contains(",")) {
            searchDateStr = searchDateStr.split(",")[0];
        }

        if ((searchDateStr == null || searchDateStr.isEmpty()) && flashDate != null && !flashDate.isEmpty()) {
            searchDateStr = flashDate;
        }

        model.addAttribute("isStaffReception", true);
        
        LocalDate selectedDate;
        try {
            selectedDate = (searchDateStr == null || searchDateStr.isEmpty()) 
                ? LocalDate.now() 
                : LocalDate.parse(searchDateStr);
        } catch (Exception e) {
            // [방어 로직] 날짜 형식이 잘못된 경우 에러를 내지 않고 오늘 날짜를 기본값으로 사용
            selectedDate = LocalDate.now();
        }
        String finalDateStr = selectedDate.toString();

        String dayOfWeek = selectedDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.KOREAN);
        model.addAttribute("formattedDate", finalDateStr + " (" + dayOfWeek + ")");

        // 탭에 따른 DB 상태값 매핑 (호환성 높은 switch 문 사용)
        String dbStatus = null;
        if (currentTab != null) {
            switch (currentTab) {
                case "reserved": dbStatus = "RESERVED"; break;
                case "received": dbStatus = "RECEIVED"; break;
                case "cancelled": dbStatus = "CANCELLED"; break;
                default: dbStatus = null; break;
            }
        }

        List<StaffReservationDto> all = receptionService.getReservations(selectedDate, dbStatus, query, deptIds, doctorIds, source);

        // 탭별 정밀 필터링 로직
        final String activeTab = currentTab;
        List<StaffReservationDto> filtered = all.stream()
            .filter(r -> {
                if ("treatment_status".equals(activeTab)) {
                    // [기능 개선] 진료현황 탭: 진료중, 진료 완료, 처치 완료 환자 모두 노출
                    return (r.getStatusText().equals("진료중") || r.getStatusText().equals("진료 완료") || r.getStatusText().equals("처치 완료")) 
                           && !r.isPaid();
                }
                if ("paid".equals(activeTab)) {
                    // [기능 개선] 수납 탭: 처치 완료(수납 대기) 환자 + 이미 수납 완료된 환자 통합 노출
                    return r.isPaid() || r.getStatusText().equals("처치 완료");
                }
                return true;
            })
            .map(r -> {
                // [명칭 변경] 수납 탭에서는 '처치 완료'를 '수납 대기'로 표시
                if ("paid".equals(activeTab)) {
                    if (r.isPaid()) {
                        r.setStatusText("수납 완료");
                        r.setStatusBadgeClass("bg-green-100 text-green-700");
                    } else if ("처치 완료".equals(r.getStatusText())) {
                        r.setStatusText("수납 대기");
                        r.setStatusBadgeClass("bg-orange-100 text-orange-700");
                    }
                }
                return r;
            })
            .collect(Collectors.toList());

        // [기능 추가] 목록 표시용 순번(rowNum) 부여
        for (int i = 0; i < filtered.size(); i++) {
            filtered.get(i).setRowNum(i + 1);
        }

        // [상태 유지] 모든 검색 파라미터를 유지하기 위한 빌더
        StringBuilder keepParams = new StringBuilder();
        if (query != null && !query.isEmpty()) keepParams.append("&query=").append(query);
        if (deptIds != null && !deptIds.isEmpty()) {
            for (Long id : deptIds) keepParams.append("&deptIds=").append(id);
        }
        if (doctorIds != null && !doctorIds.isEmpty()) {
            for (Long id : doctorIds) keepParams.append("&doctorIds=").append(id);
        }
        if (source != null && !source.isEmpty()) keepParams.append("&source=").append(source);
        String params = keepParams.toString();

        model.addAttribute("reservations", filtered);
        model.addAttribute("searchDate", finalDateStr);
        model.addAttribute("isToday", selectedDate.equals(LocalDate.now()));
        model.addAttribute("currentTab", activeTab);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("keepParams", params);

        // 탭 활성화 상태 전달
        model.addAttribute("isAllTab", "all".equals(activeTab));
        model.addAttribute("isReservedTab", "reserved".equals(activeTab));
        model.addAttribute("isReceivedTab", "received".equals(activeTab));
        model.addAttribute("isTreatmentStatusTab", "treatment_status".equals(activeTab));
        model.addAttribute("isPaidTab", "paid".equals(activeTab));
        model.addAttribute("isCancelledTab", "cancelled".equals(activeTab));

        model.addAttribute("departments", receptionService.getAllDepartments().stream()
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", d.getId());
                    map.put("name", d.getName());
                    map.put("selected", deptIds != null && deptIds.contains(d.getId()));
                    return map;
                })
                .collect(Collectors.toList()));

        model.addAttribute("doctors", receptionService.getAllDoctors().stream()
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", d.getId());
                    map.put("name", d.getDisplayName());
                    map.put("deptId", d.getDepartmentId());
                    map.put("selected", doctorIds != null && doctorIds.contains(d.getId()));
                    return map;
                })
                .collect(Collectors.toList()));

        List<Map<String, Object>> sourceOptions = new ArrayList<>();
        String[] sources = {"ONLINE", "PHONE", "WALKIN"};
        String[] sourceLabels = {"온라인", "전화", "방문"};
        for (int i = 0; i < sources.length; i++) {
            Map<String, Object> opt = new HashMap<>();
            opt.put("value", sources[i]);
            opt.put("label", sourceLabels[i]);
            opt.put("selected", sources[i].equals(source));
            sourceOptions.add(opt);
        }
        model.addAttribute("sources", sourceOptions);

        return "staff/reception-list";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam("id") Long id, 
                        @RequestParam(name = "tab", defaultValue = "all") String tab,
                        @RequestParam(name = "date", required = false) String date,
                        @RequestParam(name = "query", required = false) String query,
                        @RequestParam(name = "deptIds", required = false) List<Long> deptIds,
                        @RequestParam(name = "doctorIds", required = false) List<Long> doctorIds,
                        @RequestParam(name = "source", required = false) String source,
                        Model model) {
        
        String currentTab = tab;
        if (currentTab != null && currentTab.contains(",")) currentTab = currentTab.split(",")[0];
        String currentDate = date;
        if (currentDate != null && currentDate.contains(",")) currentDate = currentDate.split(",")[0];

        model.addAttribute("isStaffReception", true);
        StaffReservationDto dto = receptionService.getDetail(id);
        model.addAttribute("detail", dto);
        model.addAttribute("currentTab", currentTab);
        model.addAttribute("currentDate", currentDate != null ? currentDate : "");
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("deptIds", deptIds);
        model.addAttribute("doctorIds", doctorIds);
        model.addAttribute("source", source != null ? source : "");

        model.addAttribute("departments", receptionService.getAllDepartments());
        model.addAttribute("doctors", receptionService.getAllDoctors().stream()
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", d.getId());
                    map.put("name", d.getDisplayName());
                    map.put("deptId", d.getDepartmentId());
                    return map;
                })
                .collect(Collectors.toList()));

        return "staff/reception-detail";
    }

    @PostMapping("/receive")
    public String receive(@Valid @ModelAttribute ReceptionUpdateRequest request,
                        @RequestParam(name = "tab", defaultValue = "all") String tab,
                        RedirectAttributes redirectAttributes) {
        receptionService.receive(request);
        redirectAttributes.addFlashAttribute("message", "접수가 완료되었습니다.");
        redirectAttributes.addAttribute("tab", tab);
        redirectAttributes.addAttribute("date", request.getDate());
        return "redirect:/staff/reception/list";
    }

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

    @PostMapping("/cancel")
    public String cancel(@RequestParam("id") Long id, 
                        @RequestParam(name = "reason", defaultValue = "") String reason,
                        @RequestParam(name = "date", required = false) String date,
                        @RequestParam(name = "tab", defaultValue = "all") String tab,
                        RedirectAttributes redirectAttributes) {
        receptionService.cancel(id, reason);
        redirectAttributes.addFlashAttribute("message", "예약이 취소되었습니다.");
        redirectAttributes.addAttribute("date", date);
        redirectAttributes.addAttribute("tab", tab);
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
        
        if (r.getStatus() == ReservationStatus.RESERVED) {
            result.put("statusText", "예약");
            result.put("statusClass", "bg-blue-100 text-blue-700");
        } else if (r.getStatus() == ReservationStatus.CANCELLED) {
            result.put("statusText", "취소");
            result.put("statusClass", "bg-red-100 text-red-700");
        }
        return result;
    }

    // [기능 추가] 수납 즉시 처리 AJAX
    @PostMapping("/pay-ajax")
    @ResponseBody
    public Map<String, Object> payAjax(@RequestBody Map<String, Object> request) {
        Long id = Long.valueOf(request.get("id").toString());
        receptionService.completePayment(id); // 수납 완료 처리 서비스 호출
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "수납이 완료되었습니다.");
        return result;
    }

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
