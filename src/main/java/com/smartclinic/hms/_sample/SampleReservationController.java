package com.smartclinic.hms._sample;

import com.smartclinic.hms._sample.dto.SampleReservationCreateRequest;
import com.smartclinic.hms._sample.dto.SampleReservationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * [샘플] SampleReservationController — Controller 레이어 작성 가이드
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 패키지 위치 (실제 구현 시)
 *   com.smartclinic.hms.reservation.ReservationController
 *   com.smartclinic.hms.staff.reception.ReceptionController  등
 *
 * ■ Controller 핵심 규칙 (API 명세서 v2.0 + rule_spring.md 기준)
 *   1. URL 구조: /{역할}/{자원}/{액션} — RPC 스타일 계층형
 *      예) GET /staff/reception/list, POST /staff/reception/receive
 *   2. GET  → 화면 렌더링 (Mustache SSR) 또는 AJAX JSON 조회
 *   3. POST → 데이터 처리 후 리다이렉트 (PRG 패턴) 또는 JSON 에러
 *   4. Path Variable 사용 안 함 — ID는 Query Parameter 또는 Request Body
 *   5. 비즈니스 로직 작성 금지 — Service에 위임
 *   6. @Validated — 클래스 레벨 (Query Param 검증 활성화)
 *   7. @Valid — @RequestBody JSON 검증
 *
 * ■ 응답 패턴 구분 (API 명세서 §1.5)
 *   [SSR 화면 GET]    → Model에 데이터 추가 후 템플릿 이름 반환
 *   [폼 제출 POST]    → 성공: redirect:URL + flash message
 *                      실패: JSON { success, errorCode, message } — GlobalExceptionHandler 처리
 *   [AJAX 비동기]     → @ResponseBody → ResponseEntity<Map> JSON 반환 (항상 JSON)
 *
 * ■ 인증 설정 (SecurityConfig에서 처리)
 *   - /reservation/**  : 인증 불필요 (비회원 환자)
 *   - /staff/**        : ROLE_STAFF, ROLE_ADMIN
 *   - /doctor/**       : ROLE_DOCTOR, ROLE_ADMIN
 *   - /nurse/**        : ROLE_NURSE, ROLE_ADMIN
 *   - /admin/**        : ROLE_ADMIN 전용
 * ════════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Controller
@Validated                              // Query Param·Path Param 검증 활성화
@RequiredArgsConstructor
@RequestMapping("/sample/reservation")  // 실제 구현 시 /reservation, /staff/reception 등으로 변경
public class SampleReservationController {

    private final SampleReservationService reservationService;

    // ════════════════════════════════════════════════════════════════════════
    // GET — 화면 렌더링 (Mustache SSR)
    // Model에 데이터 추가 → 템플릿 이름 반환 (String)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 예약 방식 선택 화면 (GET /reservation)
     * — 비회원 환자 진입점. AI 추천 / 직접 선택 분기 화면.
     * — 실제 구현: GET /reservation
     */
    @GetMapping
    public String reservationHome() {
        return "sample/reservation/home";   // templates/sample/reservation/home.mustache
    }

    /**
     * 예약 목록 화면 (접수 직원용)
     * — 당일 RESERVED 상태 예약 목록 렌더링
     * — 실제 구현: GET /staff/reception/list (ROLE_STAFF, ROLE_ADMIN)
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<SampleReservationResponse> reservations = reservationService.getTodayReservations();

        // Model에 데이터 추가 → Mustache 템플릿에서 {{reservations}}, {{totalCount}} 로 접근
        model.addAttribute("reservations", reservations);
        model.addAttribute("totalCount", reservations.size());
        model.addAttribute("today", LocalDate.now().toString());

        // flash attribute 메시지 수신 (redirect 후 성공/실패 메시지)
        // Mustache: {{#message}}<div class="alert">{{message}}</div>{{/message}}

        return "sample/reservation/list";   // templates/sample/reservation/list.mustache
    }

    /**
     * 예약 상세·접수 처리 화면
     * — ID는 Query Parameter로 전달 (Path Variable 사용 안 함 — RPC 스타일)
     * — 실제 구현: GET /staff/reception/detail?reservationId={id}
     */
    @GetMapping("/detail")
    public String detail(
            @RequestParam Long reservationId,
            Model model) {

        SampleReservationResponse reservation = reservationService.getById(reservationId);
        model.addAttribute("reservation", reservation);

        return "sample/reservation/detail";
    }

    /**
     * 직접 선택 예약 화면
     * — LLM 추천 결과를 Query Param으로 받아 폼에 자동 입력 (선택)
     * — 실제 구현: GET /reservation/direct
     */
    @GetMapping("/direct")
    public String directReservation(
            @RequestParam(required = false) String recommendedDept,
            @RequestParam(required = false) String recommendedDoctor,
            @RequestParam(required = false) String recommendedTime,
            Model model) {

        // LLM 추천 결과가 있으면 폼에 자동 채움
        model.addAttribute("prefilledDept", recommendedDept);
        model.addAttribute("prefilledDoctor", recommendedDoctor);
        model.addAttribute("prefilledTime", recommendedTime);

        // 진료과 목록 (실제 구현: DepartmentRepository에서 활성 진료과 조회)
        // model.addAttribute("departments", departmentService.getActiveDepartments());

        return "sample/reservation/direct";
    }

    /**
     * 예약 완료 화면
     * — POST /reservation/create 성공 후 redirect: 대상
     * — reservationNumber가 없거나 미존재 시 /reservation 으로 리다이렉트
     * — 실제 구현: GET /reservation/complete?reservationNumber={번호}
     */
    @GetMapping("/complete")
    public String complete(
            @RequestParam(required = false) String reservationNumber,
            Model model) {

        if (reservationNumber == null || reservationNumber.isBlank()) {
            return "redirect:/sample/reservation";   // 잘못된 접근 → 홈으로
        }

        try {
            SampleReservationResponse reservation =
                    reservationService.getByReservationNumber(reservationNumber);
            model.addAttribute("reservation", reservation);
        } catch (SampleBusinessException e) {
            return "redirect:/sample/reservation";   // 미존재 → 홈으로
        }

        return "sample/reservation/complete";
    }

    // ════════════════════════════════════════════════════════════════════════
    // POST — 데이터 처리 후 리다이렉트 (PRG 패턴)
    // 성공: redirect:URL + RedirectAttributes.addFlashAttribute(message)
    // 실패: BusinessException → GlobalExceptionHandler → JSON 에러 응답
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 예약 생성 처리 (POST /reservation/create)
     *
     * [요청] JSON Body → @Valid @RequestBody SampleReservationCreateRequest
     *        (HTML form 제출 시 @ModelAttribute 로 변경 가능)
     *
     * [성공] redirect:/sample/reservation/complete?reservationNumber={번호}
     *        + flash: "예약이 완료되었습니다."
     *
     * [실패] GlobalExceptionHandler → JSON 응답:
     *        { "success": false, "errorCode": "DUPLICATE_RESERVATION", "message": "..." }
     *
     * ── HTML Form 제출 방식 vs JSON(AJAX) 방식 ───────────────────────────
     *   Form 제출: @ModelAttribute SampleReservationCreateRequest request
     *   JSON AJAX: @Valid @RequestBody SampleReservationCreateRequest request
     *
     *   이 샘플은 JSON Body 방식 사용. HTML Form 방식은 아래 주석 참고.
     */
    @PostMapping("/create")
    public String create(
            @Valid @RequestBody SampleReservationCreateRequest request,
            RedirectAttributes redirectAttrs) {

        String reservationNumber = reservationService.create(request);

        // 성공 메시지 — Mustache: {{#message}}<p class="alert-success">{{message}}</p>{{/message}}
        redirectAttrs.addFlashAttribute("message", "예약이 완료되었습니다.");

        // reservationNumber를 Query Param으로 전달 (addAttribute → URL 포함)
        redirectAttrs.addAttribute("reservationNumber", reservationNumber);
        return "redirect:/sample/reservation/complete";

        /*
         * ── HTML Form 방식 예시 (주석 해제 후 @RequestBody → @ModelAttribute 변경) ──
         *
         * @PostMapping("/create")
         * public String createByForm(
         *         @Valid @ModelAttribute SampleReservationCreateRequest request,
         *         BindingResult bindingResult,
         *         RedirectAttributes redirectAttrs,
         *         Model model) {
         *
         *     if (bindingResult.hasErrors()) {
         *         // 검증 실패 → 폼 화면 재렌더링 (입력값 유지)
         *         model.addAttribute("departments", ...);
         *         return "sample/reservation/direct";
         *     }
         *
         *     String reservationNumber = reservationService.create(request);
         *     redirectAttrs.addFlashAttribute("message", "예약이 완료되었습니다.");
         *     redirectAttrs.addAttribute("reservationNumber", reservationNumber);
         *     return "redirect:/sample/reservation/complete";
         * }
         */
    }

    /**
     * 접수 처리 — 상태 변경 (POST /staff/reception/receive)
     * RESERVED → RECEIVED
     *
     * [성공] redirect:/sample/reservation/list + flash message
     * [실패] INVALID_STATUS_TRANSITION → JSON 에러 (GlobalExceptionHandler)
     *
     * 실제 구현: POST /staff/reception/receive (ROLE_STAFF, ROLE_ADMIN)
     */
    @PostMapping("/receive")
    public String receive(
            @RequestParam Long reservationId,
            RedirectAttributes redirectAttrs) {

        reservationService.receive(reservationId);
        redirectAttrs.addFlashAttribute("message", "접수가 완료되었습니다.");
        return "redirect:/sample/reservation/list";
    }

    /**
     * 예약 취소 처리 (POST /admin/reservation/cancel)
     *
     * [성공] redirect:/sample/reservation/list + flash message
     * [실패] CANNOT_CANCEL_COMPLETED → JSON 에러 (GlobalExceptionHandler)
     *
     * 실제 구현: POST /admin/reservation/cancel (ROLE_ADMIN)
     */
    @PostMapping("/cancel")
    public String cancel(
            @RequestParam Long reservationId,
            RedirectAttributes redirectAttrs) {

        reservationService.cancel(reservationId);
        redirectAttrs.addFlashAttribute("message", "예약이 취소되었습니다.");
        return "redirect:/sample/reservation/list";
    }

    /**
     * 관리자용 예약 목록 화면 (페이징·필터)
     * — 실제 구현: GET /admin/reservation/list (ROLE_ADMIN)
     */
    @GetMapping("/admin/list")
    public String adminList(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) SampleReservation.ReservationStatus status,
            @RequestParam(required = false) Long doctorId,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        Page<SampleReservationResponse> page =
                reservationService.getAdminReservationList(date, status, doctorId, pageable);

        model.addAttribute("reservations", page.getContent());
        model.addAttribute("totalCount", page.getTotalElements());
        model.addAttribute("page", page.getNumber());
        model.addAttribute("size", page.getSize());
        model.addAttribute("totalPages", page.getTotalPages());

        // 필터 값 유지 (폼에 표시)
        model.addAttribute("filterDate", date);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterDoctorId", doctorId);

        return "sample/reservation/admin-list";
    }

    // ════════════════════════════════════════════════════════════════════════
    // AJAX 비동기 요청 — @ResponseBody (항상 JSON 반환)
    // 성공/실패 모두 JSON. 리다이렉트 없음.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 예약 가능 시간 슬롯 조회 (AJAX)
     * — 직접 선택 예약 화면에서 의사·날짜 선택 시 JavaScript fetch()로 호출
     * — 실제 구현: GET /reservation/getSlots (인증 불필요)
     *
     * 응답 형식:
     * {
     *   "success": true,
     *   "data": {
     *     "availableSlots": ["09:00", "09:30"],
     *     "unavailableSlots": ["10:30"]
     *   }
     * }
     */
    @GetMapping("/getSlots")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSlots(
            @RequestParam Long doctorId,
            @RequestParam String date) {

        // 실제 구현: SlotService(공통 서비스)에서 예약 가능 시간 슬롯 계산
        // List<String> booked = reservationRepository.findBookedSlotsByDoctorAndDate(doctorId, date);
        // List<String> available = slotService.getAvailableSlots(doctorId, date, booked);

        // 샘플 응답 (고정값)
        List<String> availableSlots   = List.of("09:00", "09:30", "10:00", "11:00", "14:00");
        List<String> unavailableSlots = List.of("10:30", "11:30", "15:00");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "availableSlots",   availableSlots,
                        "unavailableSlots", unavailableSlots
                )
        ));
    }

    /**
     * 진료과별 의사 목록 조회 (AJAX)
     * — 직접 선택 예약 화면에서 진료과 선택 시 JavaScript fetch()로 호출
     * — 실제 구현: GET /reservation/getDoctors (인증 불필요)
     *
     * 응답 형식:
     * {
     *   "success": true,
     *   "data": {
     *     "doctors": [{ "id": 1, "name": "김철수", "specialty": "소화기내과", ... }]
     *   }
     * }
     */
    @GetMapping("/getDoctors")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDoctors(
            @RequestParam Long departmentId,
            @RequestParam(required = false) String date) {

        // 실제 구현: DoctorRepository에서 진료과 소속 의사 조회
        // 날짜 지정 시 available_days 기반 필터링
        // List<DoctorResponse> doctors = doctorService.getDoctorsByDepartment(departmentId, date);

        // 샘플 응답 (고정값)
        List<Map<String, Object>> doctors = List.of(
                Map.of(
                        "id", 1L,
                        "name", "김철수",
                        "specialty", "소화기내과",
                        "availableDays", List.of("MON", "WED", "FRI")
                ),
                Map.of(
                        "id", 2L,
                        "name", "이민지",
                        "specialty", "외상외과",
                        "availableDays", List.of("TUE", "THU")
                )
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("doctors", doctors)
        ));
    }

    /*
     * ════════════════════════════════════════════════════════════════════════
     * [참고] Spring Security 인증 정보 접근 방법
     * ════════════════════════════════════════════════════════════════════════
     *
     * 로그인한 사용자 정보를 Controller에서 접근하는 방법:
     *
     * import org.springframework.security.core.annotation.AuthenticationPrincipal;
     * import org.springframework.security.core.userdetails.UserDetails;
     *
     * @GetMapping("/doctor/treatment/list")
     * public String doctorTreatmentList(
     *         @AuthenticationPrincipal UserDetails userDetails,
     *         Model model) {
     *
     *     String username = userDetails.getUsername();  // 로그인한 의사 ID
     *     // Staff 엔티티에서 doctorId 조회 후 서비스 호출
     *     ...
     * }
     *
     * ════════════════════════════════════════════════════════════════════════
     * [참고] Mustache 템플릿 데이터 바인딩 예시
     * ════════════════════════════════════════════════════════════════════════
     *
     * Controller: model.addAttribute("reservations", list);
     *
     * templates/sample/reservation/list.mustache:
     * {{#reservations}}
     *   <tr>
     *     <td>{{reservationNumber}}</td>
     *     <td>{{patientName}}</td>
     *     <td>{{timeSlot}}</td>
     *     <td>{{statusLabel}}</td>
     *   </tr>
     * {{/reservations}}
     * {{^reservations}}
     *   <tr><td colspan="4">예약이 없습니다.</td></tr>
     * {{/reservations}}
     *
     * flash message 표시:
     * {{#message}}
     *   <div class="alert alert-success">{{message}}</div>
     * {{/message}}
     */
}
