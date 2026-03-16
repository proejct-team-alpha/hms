package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.domain.Staff;
import com.smartclinic.hms.domain.StaffRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminStaffRepository extends JpaRepository<Staff, Long> {

    long countByActiveTrue();

    @Query("""
            select
                s.id as id,
                s.name as name,
                s.username as username,
                s.employeeNumber as employeeNumber,
                s.role as role,
                d.name as departmentName,
                s.active as active
            from Staff s
            left join s.department d
            where (:keyword is null
                or lower(s.name) like lower(concat('%', :keyword, '%'))
                or lower(s.username) like lower(concat('%', :keyword, '%')))
              and (:role is null or s.role = :role)
              and (:active is null or s.active = :active)
            """)
    Page<AdminStaffListProjection> findStaffListPage(
            @Param("keyword") String keyword,
            @Param("role") StaffRole role,
            @Param("active") Boolean active,
            Pageable pageable
    );

    interface AdminStaffListProjection {
        Long getId();

        String getName();

        String getUsername();

        String getEmployeeNumber();

        StaffRole getRole();

        String getDepartmentName();

        boolean isActive();
    }
}
