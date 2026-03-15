package com.smartclinic.hms.reservation.reservation;

import com.smartclinic.hms.domain.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class PatientRepositoryTest {

    @Autowired PatientRepository patientRepository;

    @Test
    @DisplayName("전화번호로 환자 조회 - 존재하는 경우")
    void findByPhone_exists() {
        // given
        patientRepository.save(Patient.create("홍길동", "01012345678", null));

        // when
        Optional<Patient> result = patientRepository.findByPhone("01012345678");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("홍길동");
        assertThat(result.get().getPhone()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("전화번호로 환자 조회 - 없는 경우 Optional.empty() 반환")
    void findByPhone_notExists() {
        // when
        Optional<Patient> result = patientRepository.findByPhone("01099999999");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("신규 환자 저장 시 ID 자동 생성")
    void save_newPatient_assignsId() {
        // given
        Patient patient = Patient.create("이순신", "01087654321", "test@test.com");

        // when
        Patient saved = patientRepository.save(patient);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("이순신");
        assertThat(saved.getPhone()).isEqualTo("01087654321");
    }
}
