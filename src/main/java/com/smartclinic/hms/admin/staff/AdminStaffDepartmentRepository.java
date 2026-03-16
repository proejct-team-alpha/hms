package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminStaffDepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByActiveTrueOrderByNameAsc();

    Optional<Department> findByIdAndActiveTrue(Long id);
}
