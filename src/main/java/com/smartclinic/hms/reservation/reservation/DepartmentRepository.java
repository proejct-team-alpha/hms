package com.smartclinic.hms.reservation.reservation;

// [W2-#4 작업 목록]
// DONE 1. JpaRepository<Department, Long> 구현

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartclinic.hms.domain.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
}
