package com.smartclinic.hms.reservation.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

// [W2-#4 작업 목록]
// DONE 1. JpaRepository<Reservation, Long> 구현

// [W2-#5 작업 목록]
// DONE 1. findByReservationNumber(String) — 예약번호로 단건 조회
// DONE 2. findByPatient_PhoneAndPatient_Name(String, String) — 전화번호+이름으로 목록 조회
// DONE 3. existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(...) — 중복 체크

// [W2-#5.1 작업 목록]
// DONE 1. countByCreatedAtBetween() — 당월 예약 카운트 (예약번호 생성용)
// DONE 2. findByNormalizedPhoneAndName() — '-' 제거 정규화 전화번호 + 이름 조회

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

        Optional<Reservation> findByReservationNumber(String reservationNumber);

        // '-' 제거 정규화 전화번호와 이름으로 조회
        @Query("SELECT r FROM Reservation r WHERE REPLACE(r.patient.phone, '-', '') = :phone AND r.patient.name = :name")
        List<Reservation> findByNormalizedPhoneAndName(@Param("phone") String phone, @Param("name") String name);

        boolean existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                        Long doctorId, LocalDate reservationDate, String timeSlot, ReservationStatus status);

        long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

        long countByReservationDate(LocalDate reservationDate);

        @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.reservationDate = :date AND r.status <> :excluded ORDER BY r.timeSlot")
        List<Reservation> findTodayExcludingStatus(@Param("date") LocalDate date,
                        @Param("excluded") ReservationStatus excluded);

        @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.reservationDate = :date AND r.status = :status ORDER BY r.timeSlot")
        List<Reservation> findTodayByStatus(@Param("date") LocalDate date, @Param("status") ReservationStatus status);

        @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.reservationDate >= :fromDate AND r.status <> :excluded ORDER BY r.reservationDate, r.timeSlot")
        List<Reservation> findFromDateExcludingStatus(@Param("fromDate") LocalDate fromDate,
                        @Param("excluded") ReservationStatus excluded);

        @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.reservationDate >= :fromDate AND r.status = :status ORDER BY r.reservationDate, r.timeSlot")
        List<Reservation> findFromDateByStatus(@Param("fromDate") LocalDate fromDate,
                        @Param("status") ReservationStatus status);

        @Query("SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.doctor d JOIN FETCH d.staff JOIN FETCH r.department WHERE r.id = :id")
        Optional<Reservation> findByIdWithDetails(@Param("id") Long id);

        // H-03: 비관적 락 — 예약 변경 시 동시 수정 직렬화
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT r FROM Reservation r WHERE r.id = :id")
        Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

        // 직접 예약 페이지용 — CANCELLED 제외한 예약된 슬롯 조회
        @Query("SELECT r.timeSlot FROM Reservation r " +
               "WHERE r.doctor.id = :doctorId " +
               "AND r.reservationDate = :date " +
               "AND r.status <> :excluded")
        List<String> findBookedTimeSlots(
                @Param("doctorId") Long doctorId,
                @Param("date") LocalDate date,
                @Param("excluded") ReservationStatus excluded);

        // LLM 예약 슬롯 중복 체크 — startTime 기반
        long countByDoctor_IdAndReservationDateAndStartTime(
                Long doctorId, java.time.LocalDate date, java.time.LocalTime startTime);

        // 예약 변경 페이지용 — 현재 수정 중인 예약 제외
        @Query("SELECT r.timeSlot FROM Reservation r " +
               "WHERE r.doctor.id = :doctorId " +
               "AND r.reservationDate = :date " +
               "AND r.status <> :excluded " +
               "AND r.id <> :excludeId")
        List<String> findBookedTimeSlotsExcluding(
                @Param("doctorId") Long doctorId,
                @Param("date") LocalDate date,
                @Param("excluded") ReservationStatus excluded,
                @Param("excludeId") Long excludeId);
}
