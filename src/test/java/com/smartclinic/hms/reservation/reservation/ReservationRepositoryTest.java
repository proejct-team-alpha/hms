package com.smartclinic.hms.reservation.reservation;

import com.smartclinic.hms.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class ReservationRepositoryTest {

    @Autowired ReservationRepository reservationRepository;
    @Autowired EntityManager entityManager;

    private Doctor doctor;
    private Department department;
    private Patient patient;

    @BeforeEach
    void setUp() {
        department = Department.create("내과", true);
        entityManager.persist(department);

        Staff staff = Staff.create("doctor1", "D001", "{noop}pw", "김내과", StaffRole.DOCTOR, department);
        entityManager.persist(staff);

        doctor = Doctor.create(staff, department, "MON,TUE,WED,THU,FRI", "내과");
        entityManager.persist(doctor);

        patient = Patient.create("홍길동", "01012345678", null);
        entityManager.persist(patient);

        entityManager.flush();
    }

    private Reservation persistReservation(String reservationNumber, LocalDate date, String timeSlot) {
        Reservation r = Reservation.create(reservationNumber, patient, doctor, department,
                date, timeSlot, ReservationSource.ONLINE);
        entityManager.persist(r);
        entityManager.flush();
        return r;
    }

    @Test
    @DisplayName("예약번호로 단건 조회 성공")
    void findByReservationNumber_found() {
        // given
        persistReservation("RES-20260401-001", LocalDate.of(2026, 4, 1), "09:00");

        // when
        Optional<Reservation> result = reservationRepository.findByReservationNumber("RES-20260401-001");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTimeSlot()).isEqualTo("09:00");
        assertThat(result.get().getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("존재하지 않는 예약번호 조회 시 Optional.empty() 반환")
    void findByReservationNumber_notFound() {
        // when
        Optional<Reservation> result = reservationRepository.findByReservationNumber("RES-NOTEXIST");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("동일 의사+날짜+시간에 RESERVED 예약 존재 시 중복 감지")
    void existsByDoctor_duplicateReserved_detected() {
        // given
        persistReservation("RES-20260401-001", LocalDate.of(2026, 4, 1), "09:00");

        // when
        boolean exists = reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                doctor.getId(), LocalDate.of(2026, 4, 1), "09:00", ReservationStatus.CANCELLED);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("CANCELLED 예약은 중복 체크 대상에서 제외")
    void existsByDoctor_cancelledExcluded() {
        // given
        Reservation r = persistReservation("RES-20260401-001", LocalDate.of(2026, 4, 1), "09:00");
        r.cancel();
        entityManager.flush();

        // when
        boolean exists = reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                doctor.getId(), LocalDate.of(2026, 4, 1), "09:00", ReservationStatus.CANCELLED);

        // then
        assertThat(exists).isFalse();
    }
}
