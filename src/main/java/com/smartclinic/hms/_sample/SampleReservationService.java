package com.smartclinic.hms._sample;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms._sample.dto.SampleReservationCreateRequest;
import com.smartclinic.hms._sample.dto.SampleReservationResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * [샘플] SampleReservationService — Service 레이어 작성 가이드
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 패키지 위치 (실제 구현 시)
 * com.smartclinic.hms.reservation.ReservationService
 * com.smartclinic.hms.staff.reception.ReceptionService 등
 *
 * ■ Service 레이어 핵심 규칙 (rule_spring.md §5)
 * 1. @Transactional(readOnly = true) — 클래스 레벨 기본값: 읽기 전용
 * → Hibernate dirty-checking 비활성화로 성능 최적화
 * 2. @Transactional — 쓰기 메서드에만 개별 선언 (readOnly 오버라이드)
 * 3. 비즈니스 로직 집중 — Controller는 입출력 변환만 담당
 * 4. HTTP 객체(HttpServletRequest 등) 직접 사용 금지
 * 5. @Slf4j 로깅 — 중요 비즈니스 이벤트 로그 기록
 * 6. BusinessException — 도메인 의미의 예외로 변환 후 throw
 *
 * ■ 트랜잭션 설계
 * - 읽기 전용 메서드: 클래스 레벨 @Transactional(readOnly = true) 상속
 * - 쓰기 메서드: @Transactional 개별 선언
 * - 복합 트랜잭션(2개 이상 Repository 참여): @Transactional 단일 트랜잭션 유지
 * ════════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본값: 읽기 전용 (성능 최적화)
public class SampleReservationService {

    private final SampleReservationRepository reservationRepository;
    /*
     * 실제 구현 시 추가할 의존성:
     * private final PatientRepository patientRepository;
     * private final DoctorRepository doctorRepository;
     * private final SlotService slotService; // common.service
     * private final ReservationNumberGenerator numberGenerator; // common.util
     * private final LlmRecommendationRepository llmRecommendationRepository;
     */

    // ════════════════════════════════════════════════════════════════════════
    // 쓰기 메서드 — @Transactional 개별 선언 (readOnly 오버라이드)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 예약 생성 (POST /reservation/create)
     *
     * 처리 흐름:
     * 1. 의사 진료 가능 요일 확인 (DOCTOR_NOT_AVAILABLE)
     * 2. 중복 예약 확인 (DUPLICATE_RESERVATION)
     * 3. 환자 조회 또는 신규 생성 (Patient upsert)
     * 4. 예약 번호 채번 (ReservationNumberGenerator)
     * 5. Reservation 엔티티 저장
     * 6. LLM 추천 ID가 있으면 is_used = TRUE 업데이트
     *
     * @return 발급된 예약번호 (리다이렉트 URL에 포함)
     */
    @Transactional
    public String create(SampleReservationCreateRequest req) {

        // ── 1. 중복 예약 체크 ──────────────────────────────────────────────
        boolean isDuplicate = reservationRepository.existsByDoctorIdAndReservationDateAndTimeSlot(
                req.doctorId(), req.reservationDate(), req.timeSlot());

        if (isDuplicate) {
            throw new SampleBusinessException(
                    "DUPLICATE_RESERVATION",
                    "해당 시간대는 이미 예약이 완료되었습니다.",
                    org.springframework.http.HttpStatus.CONFLICT);
        }

        // ── 2. 예약 번호 채번 ─────────────────────────────────────────────
        // 실제 구현: ReservationNumberGenerator.generate(date) — 동시성 안전 채번
        String reservationNumber = generateReservationNumber(req.reservationDate());

        // ── 3. 예약 엔티티 생성 및 저장 ───────────────────────────────────
        // 정적 팩토리 메서드 사용 (new SampleReservation() 직접 생성 금지)
        SampleReservation reservation = SampleReservation.create(
                reservationNumber,
                req.patientName(),
                req.patientPhone(),
                req.patientEmail(),
                req.departmentId(),
                req.doctorId(),
                req.reservationDate(),
                req.timeSlot(),
                req.llmRecommendationId());
        reservationRepository.save(reservation);

        log.info("[예약 생성] reservationNumber={}, doctorId={}, date={}, slot={}",
                reservationNumber, req.doctorId(), req.reservationDate(), req.timeSlot());

        return reservationNumber;
    }

    /**
     * 접수 처리 (POST /staff/reception/receive)
     * RESERVED → RECEIVED 상태 전이
     *
     * @param reservationId 예약 ID (Request Body에서 전달)
     */
    @Transactional
    public void receive(Long reservationId) {
        SampleReservation reservation = findById(reservationId);

        /*
         * ── [보안] IDOR 방지 — 객체 수준 권한 검증 (실제 구현 시 필수) ──
         * ID만으로 리소스에 접근하면 다른 사용자의 데이터를 조작할 수 있음.
         * SecurityContextHolder에서 현재 로그인 사용자를 확인하여 소유권/권한 검증 필요.
         *
         * 예시:
         * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
         * String username = auth.getName();
         * Staff currentStaff = staffRepository.findByUsername(username)
         *         .orElseThrow(() -> CustomException.unauthorized("인증 정보를 찾을 수 없습니다."));
         *
         * // 접수 직원은 자기 부서 예약만 접수 가능하도록 제한
         * if (!currentStaff.getDepartment().getId().equals(reservation.getDepartmentId())) {
         *     throw CustomException.forbidden("해당 예약에 대한 접수 권한이 없습니다.");
         * }
         */

        // 도메인 메서드가 상태 검증 포함 — 잘못된 상태 시 IllegalStateException
        try {
            reservation.receive();
        } catch (IllegalStateException e) {
            throw SampleBusinessException.invalidStatusTransition(e.getMessage());
        }

        log.info("[접수 처리] reservationId={}, status=RESERVED→RECEIVED", reservationId);
    }

    /**
     * 진료 완료 처리 (POST /doctor/treatment/complete)
     * RECEIVED → COMPLETED 상태 전이
     *
     * @param reservationId 예약 ID
     */
    @Transactional
    public void complete(Long reservationId) {
        SampleReservation reservation = findById(reservationId);

        try {
            reservation.complete();
        } catch (IllegalStateException e) {
            throw SampleBusinessException.invalidStatusTransition(e.getMessage());
        }

        /*
         * 실제 구현 시 TreatmentRecord 저장도 동일 트랜잭션에서 처리:
         *
         * TreatmentRecord record = TreatmentRecord.create(
         * reservation, diagnosisNote, loggedInDoctor);
         * treatmentRecordRepository.save(record);
         */

        log.info("[진료 완료] reservationId={}, status=RECEIVED→COMPLETED", reservationId);
    }

    /**
     * 예약 취소 (POST /admin/reservation/cancel)
     * COMPLETED 상태는 취소 불가
     *
     * @param reservationId 예약 ID
     */
    @Transactional
    public void cancel(Long reservationId) {
        SampleReservation reservation = findById(reservationId);

        /*
         * ── [보안] IDOR 방지 — 객체 수준 권한 검증 (실제 구현 시 필수) ──
         * 관리자 전용 취소의 경우에도 로그인 사용자의 ROLE 검증 권장.
         *
         * 예시:
         * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
         * boolean isAdmin = auth.getAuthorities().stream()
         *         .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
         * if (!isAdmin) {
         *     throw CustomException.forbidden("예약 취소 권한이 없습니다.");
         * }
         */

        try {
            reservation.cancel();
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            String errorCode = message.contains("완료") ? "CANNOT_CANCEL_COMPLETED" : "ALREADY_CANCELLED";
            throw new SampleBusinessException(errorCode, message, org.springframework.http.HttpStatus.CONFLICT);
        }

        log.info("[예약 취소] reservationId={}, status→CANCELLED", reservationId);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 읽기 메서드 — 클래스 레벨 @Transactional(readOnly = true) 상속
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 예약번호로 단건 조회
     * — 예약 완료 화면(GET /reservation/complete)에서 사용
     */
    public SampleReservationResponse getByReservationNumber(String reservationNumber) {
        SampleReservation reservation = reservationRepository
                .findByReservationNumber(reservationNumber)
                .orElseThrow(() -> SampleBusinessException.notFound(
                        "예약을 찾을 수 없습니다: " + reservationNumber));

        return SampleReservationResponse.from(reservation);
    }

    /**
     * 오늘 날짜 RESERVED 상태 예약 목록
     * — 접수 직원 목록 화면(GET /staff/reception/list)에서 사용
     */
    public List<SampleReservationResponse> getTodayReservations() {
        return reservationRepository.findTodayReserved(LocalDate.now())
                .stream()
                .map(SampleReservationResponse::from)
                .toList();
    }

    /**
     * 전체 예약 목록 (페이징·필터)
     * — 관리자 화면(GET /admin/reservation/list)에서 사용
     *
     * @param date     날짜 필터 (null 시 전체)
     * @param status   상태 필터 (null 시 전체)
     * @param doctorId 의사 필터 (null 시 전체)
     * @param pageable 페이징 정보 (기본값: 20건)
     */
    public Page<SampleReservationResponse> getAdminReservationList(
            LocalDate date,
            SampleReservation.ReservationStatus status,
            Long doctorId,
            Pageable pageable) {

        return reservationRepository
                .findAllWithFilter(date, status, doctorId, pageable)
                .map(SampleReservationResponse::from);
    }

    /**
     * 예약 단건 조회 (접수 처리 화면용)
     * — GET /staff/reception/detail?reservationId={id}
     */
    public SampleReservationResponse getById(Long reservationId) {
        return SampleReservationResponse.from(findById(reservationId));
    }

    // ════════════════════════════════════════════════════════════════════════
    // private 공통 메서드
    // ════════════════════════════════════════════════════════════════════════

    private SampleReservation findById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> SampleBusinessException.notFound(
                        "예약을 찾을 수 없습니다. ID: " + reservationId));
    }

    /**
     * 예약 번호 채번 — 실제 구현에서는 common.util.ReservationNumberGenerator 사용
     * 현재는 샘플용 단순 구현 (동시성 비안전 — 실제 사용 금지)
     */
    private String generateReservationNumber(LocalDate date) {
        String datePart = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = reservationRepository.countTodayReservations(date) + 1;
        return String.format("RES-%s-%03d", datePart, seq);
    }
}
