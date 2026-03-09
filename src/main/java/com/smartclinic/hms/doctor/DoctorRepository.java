package com.smartclinic.hms.doctor;

// [임시] B 작업자가 정식 구현하기 전까지 A가 임시 생성. 추후 B가 교체.

import com.smartclinic.hms.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByDepartment_Id(Long departmentId);
}
