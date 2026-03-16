package com.smartclinic.hms.admin.department;

import com.smartclinic.hms.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminDepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByActiveTrueOrderByNameAsc();

    Optional<Department> findByIdAndActiveTrue(Long id);
}