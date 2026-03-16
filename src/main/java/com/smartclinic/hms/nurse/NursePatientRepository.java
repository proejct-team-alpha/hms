package com.smartclinic.hms.nurse;

import com.smartclinic.hms.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NursePatientRepository extends JpaRepository<Patient, Long> {
}
