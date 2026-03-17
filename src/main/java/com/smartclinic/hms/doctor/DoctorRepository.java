package com.smartclinic.hms.doctor;

import com.smartclinic.hms.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("select d from Doctor d join fetch d.staff where d.department.id = :departmentId")
    List<Doctor> findByDepartment_Id(@Param("departmentId") Long departmentId);

    @Query("select d from Doctor d join fetch d.staff join fetch d.department where d.staff.id = :staffId")
    Optional<Doctor> findByStaffId(@Param("staffId") Long staffId);

    Optional<Doctor> findByStaff_Username(String username);

    @Query("SELECT d FROM Doctor d JOIN FETCH d.staff JOIN FETCH d.department")
    List<Doctor> findAllWithDetails();
}
