package com.smartclinic.hms.reservation.reservation;

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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationNumber(String reservationNumber);

    // '-' 제거 정규화 전화번호와 이름으로 조회
    @Query("SELECT r FROM Reservation r WHERE REPLACE(r.patient.phone, '-', '') = :phone AND r.patient.name = :name")
    List<Reservation> findByNormalizedPhoneAndName(@Param("phone") String phone, @Param("name") String name);

    boolean existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
            Long doctorId, LocalDate reservationDate, String timeSlot, ReservationStatus status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByReservationDate(LocalDate reservationDate);
}
