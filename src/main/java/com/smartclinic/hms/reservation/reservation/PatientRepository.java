package com.smartclinic.hms.reservation.reservation;

// [W2-#4 작업 목록]
// DONE 1. JpaRepository<Patient, Long> 구현
// DONE 2. findByPhone() 추가

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartclinic.hms.domain.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPhone(String phone);
}
