package com.smartclinic.hms.doctor;

// [임시] B 작업자가 정식 구현하기 전까지 A가 임시 생성. 추후 B가 교체.

import com.smartclinic.hms.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("SELECT d FROM Doctor d JOIN FETCH d.staff WHERE d.department.id = :departmentId")
    List<Doctor> findByDepartment_Id(@Param("departmentId") Long departmentId);

    Optional<Doctor> findByStaff_Username(String username);

    @Query("SELECT d FROM Doctor d JOIN FETCH d.staff JOIN FETCH d.department")
    List<Doctor> findAllWithDetails();
}
