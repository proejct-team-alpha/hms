package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdminStaffRepository extends JpaRepository<Staff, Long> {

    long countByActiveTrue();

    @Query("SELECT s FROM Staff s LEFT JOIN FETCH s.department ORDER BY s.name")
    List<Staff> findAllWithDepartment();
}
