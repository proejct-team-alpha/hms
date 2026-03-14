package com.smartclinic.hms.admin.staff;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStaffService {

    private final AdminStaffRepository adminStaffRepository;

    public List<AdminStaffDto> getStaffList() {
        return adminStaffRepository.findAllWithDepartment()
                .stream()
                .map(AdminStaffDto::new)
                .collect(Collectors.toList());
    }
}
