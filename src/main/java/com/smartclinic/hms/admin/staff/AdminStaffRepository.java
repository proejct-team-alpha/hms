package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminStaffRepository extends JpaRepository<Staff, Long> {

    long countByActiveTrue();
}
