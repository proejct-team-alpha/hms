package com.smartclinic.hms.reservation.reservation;

// [W2-#4 작업 목록]
// DONE 1. POST /reservation/create 추가 (PRG 패턴)
// DONE 2. 예약 저장 후 완료 화면으로 리다이렉트 (name, department, doctor, date, time 파라미터 전달)

// [W2-#5 작업 목록]
// DONE 1. GET /reservation/cancel — 예약번호 조회 + 취소 확인 화면
// DONE 2. POST /reservation/cancel/{id} — 취소 처리 후 redirect:/reservation
// DONE 3. GET /reservation/modify — 예약번호 조회 + 변경 폼 화면
// DONE 4. POST /reservation/modify/{id} — 변경 처리 후 redirect:/reservation/complete

// [W2-#5.1 작업 목록]
// DONE 1. POST /reservation/create — reservationNumber RedirectAttributes 추가
// DONE 2. GET /reservation/lookup — 예약번호 단건 / 이름+전화번호 목록 조회

// [W2-#8 작업 목록]
// DONE 1. POST /reservation/create — @Valid + BindingResult 적용, 에러 시 폼 재표시

import com.smartclinic.hms.common.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/reservation")
@RequiredArgsConstructor
@Controller
public class ReservationController {

    private final ReservationService reservationService;

    // 예약 방식 선택 화면 (AI 증상 분석 / 직접 선택)
    @GetMapping("")
    public String patientChoice(HttpServletRequest request) {
        request.setAttribute("pageTitle", "진료 예약");
        return "reservation/patient-choice";
    }

    // AI 증상 분석 예약 화면 (증상 입력 → LLM 추천 → 폼 자동 채움)
    @GetMapping("/symptom-reservation")
    public String symptomReservation(Model model) {
        model.addAttribute("pageTitle", "AI 증상 분석 예약");
        return "reservation/symptom-reservation";
    }

    // 직접 선택 예약 폼 화면 (진료과 목록을 DepartmentDto 리스트로 전달)
    @GetMapping("/direct-reservation")
    public String directReservation(HttpServletRequest request) {
        request.setAttribute("pageTitle", "직접 선택 예약");
        // 진료과 목록을 DepartmentDto 리스트로 변환하여 뷰에 전달
        request.setAttribute("departments", reservationService.getDepartments());
        return "reservation/direct-reservation";
    }

    // 예약 완료 화면 (flash attribute로 전달된 ReservationCompleteInfo DTO를 뷰에서 {{#info}}로 접근)
    @GetMapping("/complete")
    public String reservationComplete(HttpServletRequest request) {
        request.setAttribute("pageTitle", "예약 완료");
        return "reservation/reservation-complete";
    }

    // 예약 생성 처리 (PRG 패턴 — POST → redirect → GET)
    @PostMapping("/create")
    public String createReservation(@Valid @ModelAttribute CreateReservationRequest form,
                                    BindingResult bindingResult,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        // 유효성 검사 실패 시: 오류 메시지와 함께 폼 화면 재표시
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining(" "));
            request.setAttribute("pageTitle", "직접 선택 예약");
            request.setAttribute("errorMessage", errorMessage);
            // 진료과 목록 다시 전달 (폼 재렌더링에 필요)
            request.setAttribute("departments", reservationService.getDepartments());
            return "reservation/direct-reservation";
        }

        try {
            // 예약 생성 → ReservationCompleteInfo DTO 반환
            ReservationCompleteInfo info = reservationService.createReservation(form);
            // flash attribute로 DTO 통째로 전달 (redirect 후 뷰에서 {{#info}}로 접근)
            redirectAttributes.addFlashAttribute("info", info);
            return "redirect:/reservation/complete";
        } catch (CustomException e) {
            // 중복 예약 등 비즈니스 예외 시: 오류 메시지와 함께 폼 화면 재표시
            request.setAttribute("pageTitle", "직접 선택 예약");
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("departments", reservationService.getDepartments());
            return "reservation/direct-reservation";
        }
    }

    // 예약 조회 화면 (예약번호 단건 조회 또는 이름+전화번호 목록 조회)
    @GetMapping("/lookup")
    public String lookupPage(HttpServletRequest request) {
        String reservationNumber = request.getParameter("reservationNumber");
        String name  = request.getParameter("name");
        String phone = request.getParameter("phone");

        // 예약번호로 단건 조회 → ReservationInfoDto로 변환하여 뷰에 전달
        if (reservationNumber != null && !reservationNumber.isBlank()) {
            reservationService.findByReservationNumber(reservationNumber)
                    .ifPresentOrElse(
                            dto -> request.setAttribute("reservation", dto),
                            () -> request.setAttribute("errorMessage", "예약을 찾을 수 없습니다.")
                    );
        // 이름 + 전화번호로 목록 조회 → ReservationInfoDto 리스트로 변환하여 뷰에 전달
        } else if (name != null && !name.isBlank() && phone != null && !phone.isBlank()) {
            List<ReservationInfoDto> list = reservationService.findByPhoneAndName(phone, name);
            if (list.isEmpty()) {
                request.setAttribute("errorMessage", "예약을 찾을 수 없습니다.");
            } else {
                request.setAttribute("reservations", list);
            }
        }
        request.setAttribute("pageTitle", "예약 조회");
        return "reservation/reservation-lookup";
    }

    // 예약 취소 확인 화면 (예약번호 조회 결과를 ReservationInfoDto로 전달)
    @GetMapping("/cancel")
    public String cancelPage(HttpServletRequest request) {
        String reservationNumber = request.getParameter("reservationNumber");
        if (reservationNumber != null && !reservationNumber.isBlank()) {
            reservationService.findByReservationNumber(reservationNumber)
                    .ifPresentOrElse(
                            r -> request.setAttribute("reservation", r),  // ReservationInfoDto 전달
                            () -> request.setAttribute("errorMessage", "예약을 찾을 수 없습니다.")
                    );
        }
        request.setAttribute("pageTitle", "예약 취소");
        return "reservation/reservation-cancel";
    }

    // 예약 취소 처리 (PRG 패턴 — POST → redirect → GET)
    @PostMapping("/cancel/{id}")
    public String cancelReservation(@PathVariable("id") Long id,
                                    @RequestParam("phone") String phone,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        try {
            // 취소 처리 → ReservationCompleteInfo DTO 반환
            ReservationCompleteInfo info = reservationService.cancelReservation(id, phone);
            // flash attribute로 DTO 통째로 전달 (redirect 후 뷰에서 {{#info}}로 접근)
            redirectAttributes.addFlashAttribute("info", info);
            return "redirect:/reservation/cancel-complete";
        } catch (CustomException e) {
            // 소유권 검증 실패 등 비즈니스 예외 시: 오류 메시지와 함께 취소 화면 재표시
            request.setAttribute("pageTitle", "예약 취소");
            request.setAttribute("errorMessage", e.getMessage());
            return "reservation/reservation-cancel";
        }
    }

    // 예약 취소 완료 화면 (flash attribute로 전달된 ReservationCompleteInfo DTO를 뷰에서 {{#info}}로 접근)
    @GetMapping("/cancel-complete")
    public String cancelComplete(HttpServletRequest request) {
        request.setAttribute("pageTitle", "예약 취소 완료");
        return "reservation/reservation-cancel-complete";
    }

    // 예약 변경 폼 화면 (예약번호 조회 결과를 ReservationInfoDto로 전달)
    @GetMapping("/modify")
    public String modifyPage(HttpServletRequest request) {
        String reservationNumber = request.getParameter("reservationNumber");
        if (reservationNumber != null && !reservationNumber.isBlank()) {
            reservationService.findByReservationNumber(reservationNumber)
                    .ifPresentOrElse(
                            r -> request.setAttribute("reservation", r),  // ReservationInfoDto 전달
                            () -> request.setAttribute("errorMessage", "예약을 찾을 수 없습니다.")
                    );
        }
        request.setAttribute("pageTitle", "예약 변경");
        return "reservation/reservation-modify";
    }

    // 예약 변경 처리 (PRG 패턴 — POST → redirect → GET)
    @PostMapping("/modify/{id}")
    public String modifyReservation(@PathVariable("id") Long id,
                                    @RequestParam("phone") String phone,
                                    @Valid @ModelAttribute UpdateReservationRequest form,
                                    BindingResult bindingResult,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        // 유효성 검사 실패 시: 기존 예약 정보와 함께 폼 재표시
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining(" "));
            request.setAttribute("pageTitle", "예약 변경");
            request.setAttribute("errorMessage", errorMessage);
            // 기존 예약 정보를 ReservationInfoDto로 다시 전달
            reservationService.findById(id)
                    .ifPresent(r -> request.setAttribute("reservation", r));
            return "reservation/reservation-modify";
        }
        try {
            // 변경 처리 → ReservationCompleteInfo DTO 반환
            ReservationCompleteInfo info = reservationService.updateReservation(id, phone, form);
            // flash attribute로 DTO 통째로 전달 (redirect 후 뷰에서 {{#info}}로 접근)
            redirectAttributes.addFlashAttribute("info", info);
            return "redirect:/reservation/modify-complete";
        } catch (CustomException e) {
            // 소유권 검증 실패, 중복 시간대 등 비즈니스 예외 시: 오류 메시지와 함께 폼 재표시
            request.setAttribute("pageTitle", "예약 변경");
            request.setAttribute("errorMessage", e.getMessage());
            reservationService.findById(id)
                    .ifPresent(r -> request.setAttribute("reservation", r));
            return "reservation/reservation-modify";
        }
    }

    // 예약 변경 완료 화면 (flash attribute로 전달된 ReservationCompleteInfo DTO를 뷰에서 {{#info}}로 접근)
    @GetMapping("/modify-complete")
    public String modifyComplete(HttpServletRequest request) {
        request.setAttribute("pageTitle", "예약 변경 완료");
        return "reservation/reservation-modify-complete";
    }
}
