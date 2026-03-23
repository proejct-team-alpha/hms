package com.smartclinic.hms._sample;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * [샘플] SampleReservationRepository — Spring Data JPA Repository 작성 가이드
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 패키지 위치 (실제 구현 시)
 *   com.smartclinic.hms.reservation.ReservationRepository
 *   com.smartclinic.hms.admin.reservation.AdminReservationRepository  등
 *
 * ■ Repository 작성 핵심 규칙
 *   1. JpaRepository<Entity, PK타입> 상속 — 기본 CRUD 자동 제공
 *   2. 메서드 이름 파생 쿼리 — 간단한 조회는 메서드 이름으로 표현
 *   3. @Query (JPQL) — 복잡한 조회·조인·집계 시 사용
 *   4. @Query(nativeQuery = true) — 특정 DB 최적화 쿼리가 필요한 경우만 사용
 *   5. @Modifying — UPDATE·DELETE 쿼리에 반드시 추가 (+ @Transactional)
 *   6. 비즈니스 로직 작성 금지 — Repository는 DB 접근만 담당
 *
 * ■ JpaRepository 기본 제공 메서드 (별도 구현 불필요)
 *   - save(entity)          : 저장·수정
 *   - findById(id)          : ID로 단건 조회 → Optional<T>
 *   - findAll()             : 전체 목록 조회
 *   - findAll(pageable)     : 페이징 조회
 *   - existsById(id)        : 존재 여부 확인
 *   - deleteById(id)        : ID로 삭제
 *   - count()               : 전체 건수
 * ════════════════════════════════════════════════════════════════════════════
 */
@Profile("dev")
public interface SampleReservationRepository extends JpaRepository<SampleReservation, Long> {

    // ════════════════════════════════════════════════════════════════════════
    // 1. 메서드 이름 파생 쿼리 (Derived Query Methods)
    //    — 단순 조건 조회는 메서드 이름으로 쿼리 자동 생성
    // ════════════════════════════════════════════════════════════════════════

    /** 예약번호로 단건 조회 */
    Optional<SampleReservation> findByReservationNumber(String reservationNumber);

    /** 특정 날짜의 예약 목록 (시간 순 정렬) */
    List<SampleReservation> findByReservationDateOrderByTimeSlotAsc(LocalDate reservationDate);

    /** 중복 예약 확인 — 의사 + 날짜 + 시간 슬롯이 동일한 예약이 있는지 체크 */
    boolean existsByDoctorIdAndReservationDateAndTimeSlot(
            Long doctorId, LocalDate reservationDate, String timeSlot);

    /** 특정 날짜의 특정 상태 예약 목록 */
    List<SampleReservation> findByReservationDateAndStatus(
            LocalDate reservationDate, SampleReservation.ReservationStatus status);

    /** 환자 이름 + 연락처로 예약 이력 조회 (환자 확인용) */
    List<SampleReservation> findByPatientNameAndPatientPhoneOrderByReservationDateDesc(
            String patientName, String patientPhone);

    // ════════════════════════════════════════════════════════════════════════
    // 2. JPQL 쿼리 (@Query)
    //    — 복잡한 조회, 조인, 집계, 특정 필드만 select 시 사용
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 오늘 날짜의 RESERVED 상태 예약 목록
     * — 접수 직원 화면(GET /staff/reception/list)에서 사용
     */
    @Query("SELECT r FROM SampleReservation r " +
           "WHERE r.reservationDate = :date " +
           "AND r.status = 'RESERVED' " +
           "ORDER BY r.timeSlot ASC")
    List<SampleReservation> findTodayReserved(@Param("date") LocalDate date);

    /**
     * 특정 의사의 당일 RECEIVED 상태 환자 목록
     * — 의사 진료 목록 화면(GET /doctor/treatment/list)에서 사용
     */
    @Query("SELECT r FROM SampleReservation r " +
           "WHERE r.doctorId = :doctorId " +
           "AND r.reservationDate = :date " +
           "AND r.status = 'RECEIVED' " +
           "ORDER BY r.timeSlot ASC")
    List<SampleReservation> findTodayTreatmentList(
            @Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    /**
     * 전체 예약 목록 (페이징) — 관리자 화면
     * — 날짜·상태·의사 조건 필터 포함
     * — 실제 구현 시 동적 쿼리(Specification 또는 QueryDSL) 권장
     *
     * [페이징 주의] count 쿼리를 JOIN 없이 분리하여 성능 최적화
     */
    @Query(
        value      = "SELECT r FROM SampleReservation r " +
                     "WHERE (:date IS NULL OR r.reservationDate = :date) " +
                     "AND (:status IS NULL OR r.status = :status) " +
                     "AND (:doctorId IS NULL OR r.doctorId = :doctorId) " +
                     "ORDER BY r.reservationDate DESC, r.timeSlot ASC",
        countQuery = "SELECT COUNT(r) FROM SampleReservation r " +
                     "WHERE (:date IS NULL OR r.reservationDate = :date) " +
                     "AND (:status IS NULL OR r.status = :status) " +
                     "AND (:doctorId IS NULL OR r.doctorId = :doctorId)"
    )
    Page<SampleReservation> findAllWithFilter(
            @Param("date") LocalDate date,
            @Param("status") SampleReservation.ReservationStatus status,
            @Param("doctorId") Long doctorId,
            Pageable pageable);

    /**
     * 대시보드 통계 — 오늘 예약 총 건수
     * SELECT COUNT(*) WHERE reservation_date = TODAY
     */
    @Query("SELECT COUNT(r) FROM SampleReservation r WHERE r.reservationDate = :today")
    long countTodayReservations(@Param("today") LocalDate today);

    /**
     * 대시보드 통계 — 진료과별 예약 건수
     * — DTO Projection 방식 (Interface 기반 Projection 예시)
     *
     * 실제 구현 시 아래와 같이 Interface Projection 정의:
     *   public interface DepartmentStatProjection {
     *       Long getDepartmentId();
     *       Long getCount();
     *   }
     */
    @Query("SELECT r.departmentId AS departmentId, COUNT(r) AS count " +
           "FROM SampleReservation r " +
           "GROUP BY r.departmentId " +
           "ORDER BY count DESC")
    List<Object[]> countGroupByDepartment();

    // ════════════════════════════════════════════════════════════════════════
    // 3. 네이티브 쿼리 (nativeQuery = true)
    //    — 특정 DB 기능(함수, 힌트 등)이 꼭 필요한 경우만 사용
    //    — JPQL로 해결 가능하면 JPQL 우선
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 특정 의사의 날짜별 예약된 시간 슬롯 목록 조회
     * — 예약 가능 시간 슬롯 계산 시 사용 (GET /reservation/getSlots)
     */
    @Query(value = "SELECT time_slot FROM sample_reservation " +
                   "WHERE doctor_id = :doctorId " +
                   "AND reservation_date = :date " +
                   "AND status != 'CANCELLED'",
           nativeQuery = true)
    List<String> findBookedSlotsByDoctorAndDate(
            @Param("doctorId") Long doctorId, @Param("date") String date);

    // ════════════════════════════════════════════════════════════════════════
    // 4. @Modifying — 벌크 UPDATE/DELETE (단, 영속성 컨텍스트 주의)
    //    실제 구현에서는 엔티티 메서드를 통한 상태 변경 권장
    //    (Dirty Checking 방식이 안전)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 특정 날짜 이전의 RESERVED 상태 예약을 일괄 취소
     * — 스케줄러에서 배치 처리 시 사용 예시
     *
     * [주의] @Modifying 사용 시 영속성 컨텍스트와 DB가 불일치할 수 있으므로
     *        @Transactional 과 함께 사용, clearAutomatically = true 권장
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SampleReservation r SET r.status = 'CANCELLED' " +
           "WHERE r.reservationDate < :date AND r.status = 'RESERVED'")
    int cancelExpiredReservations(@Param("date") LocalDate date);
}
