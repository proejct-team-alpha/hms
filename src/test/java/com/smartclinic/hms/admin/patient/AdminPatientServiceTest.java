package com.smartclinic.hms.admin.patient;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.smartclinic.hms.domain.Patient;

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
        var result = adminPatientService.getPatientList(1, 20, "김", "0101234");

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

    private void persistPatient(String name, String phone, LocalDateTime createdAt) {
        Patient patient = Patient.create(name, phone, name + "@example.com");
        entityManager.persist(patient);
        entityManager.flush();
        ReflectionTestUtils.setField(patient, "createdAt", createdAt);
        entityManager.merge(patient);
    }
}
