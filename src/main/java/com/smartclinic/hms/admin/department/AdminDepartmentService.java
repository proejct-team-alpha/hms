package com.smartclinic.hms.admin.department;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.domain.Department;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDepartmentService {

    private final AdminDepartmentRepository adminDepartmentRepository;

    public List<AdminDepartmentDto> getDepartmentList() {
        return adminDepartmentRepository.findAllByOrderByNameAsc()
                .stream()
                .map(AdminDepartmentDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createDepartment(String name) {
        adminDepartmentRepository.save(Department.create(name, true));
    }
}
