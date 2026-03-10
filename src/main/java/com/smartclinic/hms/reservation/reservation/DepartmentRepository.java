package com.smartclinic.hms.reservation.reservation;

// [W2-#4 작업 목록]
// DONE 1. JpaRepository<Department, Long> 구현

import com.smartclinic.hms.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
