package com.smartclinic.hms.admin.department;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<AdminDepartmentDto> getDepartmentList() {
        return departmentRepository.findAll()
                .stream()
                .map(AdminDepartmentDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createDepartment(String name) {
        departmentRepository.save(Department.create(name, true));
    }
}
