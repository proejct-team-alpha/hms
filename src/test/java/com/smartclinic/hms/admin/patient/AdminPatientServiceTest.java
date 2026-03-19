package com.smartclinic.hms.admin.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.domain.Staff;
import com.smartclinic.hms.domain.StaffRole;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(AdminPatientService.class)
@TestPropertySource(properties = "spring.sql.init.mode=never")
class AdminPatientServiceTest {

    @Autowired
    private AdminPatientService adminPatientService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("get patient list filters by name and normalized contact")
    void getPatientList_filtersByNameAndNormalizedContact() {
        // given
        persistPatient("김철수", "010-1234-5678", LocalDateTime.of(2026, 3, 18, 10, 0));
        persistPatient("김영희", "010-8888-9999", LocalDateTime.of(2026, 3, 19, 10, 0));
        persistPatient("이민수", "010-1234-0000", LocalDateTime.of(2026, 3, 17, 10, 0));
        entityManager.flush();
        entityManager.clear();

        // when
        var result = adminPatientService.getPatientList(1, 20, "김철", "0101234");

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.patients()).hasSize(1);
        assertThat(result.patients().getFirst().name()).isEqualTo("김철수");
    }

    @Test
    @DisplayName("get patient list applies default paging and ordering")
    void getPatientList_appliesDefaultPagingAndOrdering() {
        // given
        for (int i = 1; i <= 21; i++) {
            persistPatient("patient-" + i, "010-1000-10" + String.format("%02d", i), LocalDateTime.of(2026, 3, 1, 0, 0).plusDays(i));
        }
        entityManager.flush();
        entityManager.clear();

        // when
        var result = adminPatientService.getPatientList(0, 0, null, null);

        // then
        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalCount()).isEqualTo(21);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.patients()).hasSize(20);
        assertThat(result.patients().getFirst().name()).isEqualTo("patient-21");
    }

    @Test
    @DisplayName("get patient list returns empty result when no patient matches")
    void getPatientList_returnsEmptyResultWhenNoPatientMatches() {
        // given
        persistPatient("김철수", "010-1234-5678", LocalDateTime.of(2026, 3, 18, 10, 0));
        entityManager.flush();
        entityManager.clear();

        // when
        var result = adminPatientService.getPatientList(1, 20, "박", "0109999");

        // then
        assertThat(result.totalCount()).isZero();
        assertThat(result.patients()).isEmpty();
        assertThat(result.hasPages()).isFalse();
        assertThat(result.pageLinks()).isEmpty();
    }

    @Test
    @DisplayName("get patient detail returns patient info with reservation histories")
    void getPatientDetail_returnsPatientInfoWithReservationHistories() {
        // given
        Department department = persistDepartment("치과");
        Staff doctorStaff = persistDoctorStaff("doctor1", "D-001", "김의사", department);
        Doctor doctor = persistDoctor(doctorStaff, department);

        Patient patient = Patient.create("김철수", "010-1234-5678", "kim@example.com");
        patient.updateInfo("김철수", "010-1234-5678", "kim@example.com", "서울시 강남구", "알레르기 주의");
        entityManager.persist(patient);

        persistReservation("R-1002", patient, doctor, department, LocalDate.of(2026, 3, 19), "09:30", ReservationStatus.RECEIVED);
        persistReservation("R-1001", patient, doctor, department, LocalDate.of(2026, 3, 18), "10:00", ReservationStatus.RESERVED);

        entityManager.flush();
        entityManager.clear();

        // when
        var result = adminPatientService.getPatientDetail(patient.getId());

        // then
        assertThat(result.patientId()).isEqualTo(patient.getId());
        assertThat(result.name()).isEqualTo("김철수");
        assertThat(result.phone()).isEqualTo("010-1234-5678");
        assertThat(result.email()).isEqualTo("kim@example.com");
        assertThat(result.address()).isEqualTo("서울시 강남구");
        assertThat(result.note()).isEqualTo("알레르기 주의");
        assertThat(result.reservationHistories()).hasSize(2);
        assertThat(result.reservationHistories().get(0).reservationNumber()).isEqualTo("R-1002");
        assertThat(result.reservationHistories().get(0).statusText()).isEqualTo("접수");
        assertThat(result.reservationHistories().get(1).reservationNumber()).isEqualTo("R-1001");
        assertThat(result.reservationHistories().get(1).statusText()).isEqualTo("예약");
    }

    @Test
    @DisplayName("get patient detail throws when patient is missing")
    void getPatientDetail_throwsWhenPatientIsMissing() {
        // given // when // then
        assertThatThrownBy(() -> adminPatientService.getPatientDetail(999L))
                .isInstanceOf(CustomException.class)
                .hasMessage("환자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("update patient changes name phone note and preserves email address")
    void updatePatient_changesNamePhoneNoteAndPreservesEmailAddress() {
        // given
        Patient patient = Patient.create("김철수", "010-1234-5678", "kim@example.com");
        patient.updateInfo("김철수", "010-1234-5678", "kim@example.com", "서울시 강남구", "기존 메모");
        entityManager.persist(patient);
        entityManager.flush();
        entityManager.clear();

        // when
        var result = adminPatientService.updatePatient(patient.getId(), "  김수정  ", "010-9999-8888", "  수정 메모  ");
        entityManager.flush();
        entityManager.clear();

        // then
        Patient updated = entityManager.find(Patient.class, patient.getId());
        assertThat(result.patientId()).isEqualTo(patient.getId());
        assertThat(result.name()).isEqualTo("김수정");
        assertThat(result.phone()).isEqualTo("010-9999-8888");
        assertThat(result.note()).isEqualTo("수정 메모");
        assertThat(result.message()).isEqualTo("환자 정보가 수정되었습니다.");
        assertThat(updated.getName()).isEqualTo("김수정");
        assertThat(updated.getPhone()).isEqualTo("010-9999-8888");
        assertThat(updated.getNote()).isEqualTo("수정 메모");
        assertThat(updated.getEmail()).isEqualTo("kim@example.com");
        assertThat(updated.getAddress()).isEqualTo("서울시 강남구");
    }

    @Test
    @DisplayName("update patient converts blank note to null")
    void updatePatient_convertsBlankNoteToNull() {
        // given
        Patient patient = Patient.create("김철수", "010-1234-5678", "kim@example.com");
        patient.updateInfo("김철수", "010-1234-5678", "kim@example.com", "서울시 강남구", "기존 메모");
        entityManager.persist(patient);
        entityManager.flush();
        entityManager.clear();

        // when
        var result = adminPatientService.updatePatient(patient.getId(), "김철수", "010-1234-5678", "   ");
        entityManager.flush();
        entityManager.clear();

        // then
        Patient updated = entityManager.find(Patient.class, patient.getId());
        assertThat(result.note()).isNull();
        assertThat(updated.getNote()).isNull();
    }

    @Test
    @DisplayName("update patient throws when patient is missing")
    void updatePatient_throwsWhenPatientIsMissing() {
        // given // when // then
        assertThatThrownBy(() -> adminPatientService.updatePatient(999L, "김철수", "010-1234-5678", "메모"))
                .isInstanceOf(CustomException.class)
                .hasMessage("환자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("update patient throws when phone is duplicated by another patient")
    void updatePatient_throwsWhenPhoneIsDuplicatedByAnotherPatient() {
        // given
        Patient first = Patient.create("김철수", "010-1234-5678", "kim@example.com");
        Patient second = Patient.create("박영희", "010-7777-8888", "park@example.com");
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.flush();
        entityManager.clear();

        // when // then
        assertThatThrownBy(() -> adminPatientService.updatePatient(second.getId(), "박영희", "01012345678", "메모"))
                .isInstanceOf(CustomException.class)
                .hasMessage("이미 사용 중인 연락처입니다.");
    }

    @Test
    @DisplayName("update patient allows keeping the same phone")
    void updatePatient_allowsKeepingTheSamePhone() {
        // given
        Patient patient = Patient.create("김철수", "010-1234-5678", "kim@example.com");
        patient.updateInfo("김철수", "010-1234-5678", "kim@example.com", "서울시 강남구", null);
        entityManager.persist(patient);
        entityManager.flush();
        entityManager.clear();

        // when
        var result = adminPatientService.updatePatient(patient.getId(), "김철수 수정", "010-1234-5678", null);
        entityManager.flush();
        entityManager.clear();

        // then
        Patient updated = entityManager.find(Patient.class, patient.getId());
        assertThat(result.phone()).isEqualTo("010-1234-5678");
        assertThat(updated.getName()).isEqualTo("김철수 수정");
        assertThat(updated.getPhone()).isEqualTo("010-1234-5678");
    }

    private void persistPatient(String name, String phone, LocalDateTime createdAt) {
        Patient patient = Patient.create(name, phone, name + "@example.com");
        entityManager.persist(patient);
        entityManager.flush();
        ReflectionTestUtils.setField(patient, "createdAt", createdAt);
        entityManager.merge(patient);
    }

    private Department persistDepartment(String name) {
        Department department = Department.create(name, true);
        entityManager.persist(department);
        return department;
    }

    private Staff persistDoctorStaff(String username, String employeeNumber, String name, Department department) {
        Staff staff = Staff.create(username, employeeNumber, "encoded-password", name, StaffRole.DOCTOR, department);
        entityManager.persist(staff);
        return staff;
    }

    private Doctor persistDoctor(Staff staff, Department department) {
        Doctor doctor = Doctor.create(staff, department, "MON,TUE", "치과");
        entityManager.persist(doctor);
        return doctor;
    }

    private void persistReservation(
            String reservationNumber,
            Patient patient,
            Doctor doctor,
            Department department,
            LocalDate reservationDate,
            String timeSlot,
            ReservationStatus status) {
        Reservation reservation = Reservation.create(
                reservationNumber,
                patient,
                doctor,
                department,
                reservationDate,
                timeSlot,
                ReservationSource.ONLINE
        );
        entityManager.persist(reservation);
        entityManager.flush();
        ReflectionTestUtils.setField(reservation, "status", status);
        entityManager.merge(reservation);
    }
}
