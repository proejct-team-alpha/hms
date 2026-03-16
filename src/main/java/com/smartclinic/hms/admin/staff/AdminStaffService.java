package com.smartclinic.hms.admin.staff;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Staff;
import com.smartclinic.hms.domain.StaffRole;
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStaffService {

    private final AdminStaffRepository adminStaffRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public List<AdminStaffDto> getStaffList() {
        return adminStaffRepository.findAllWithDepartment()
                .stream()
                .map(AdminStaffDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createStaff(String username, String employeeNumber, String rawPassword,
                             String name, String role, Long departmentId) {
        Department department = departmentId != null
                ? departmentRepository.findById(departmentId).orElse(null)
                : null;
        Staff staff = Staff.create(username, employeeNumber, passwordEncoder.encode(rawPassword),
                name, StaffRole.valueOf(role), department);
        adminStaffRepository.save(staff);
    }
}
