package com.smartclinic.hms.admin.department;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smartclinic.hms.domain.Department;

public interface AdminDepartmentRepository extends JpaRepository<Department, Long> {

    Page<Department> findAllByOrderByNameAsc(Pageable pageable);

    List<Department> findAllByOrderByNameAsc();

    List<Department> findByActiveTrueOrderByNameAsc();

    Optional<Department> findByIdAndActiveTrue(Long id);
}