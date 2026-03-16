package com.smartclinic.hms.doctor.treatment;

import com.smartclinic.hms.domain.TreatmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorTreatmentRecordRepository extends JpaRepository<TreatmentRecord, Long> {

    Optional<TreatmentRecord> findByReservation_Id(Long reservationId);
}
