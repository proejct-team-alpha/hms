package com.smartclinic.hms.admin.mypage;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.admin.mypage.dto.AdminMypageResponse;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMypageService {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminMypageResponse getMypage(String username) {
        return staffRepository.findByUsernameAndActiveTrue(username)
                .map(AdminMypageResponse::new)
                .orElseThrow(() -> CustomException.notFound("관리자 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public void updateMypage(String username, String name, String email, String phone,
                             String currentPassword, String newPassword, String confirmPassword) {
        Staff staff = staffRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> CustomException.notFound("관리자 정보를 찾을 수 없습니다."));

        if (name != null && !name.isBlank()) {
            staff.update(name, staff.getDepartment(), staff.isActive());
        }
        staff.updateContact(email, phone);

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || currentPassword.isBlank()) {
                throw CustomException.badRequest("VALIDATION_ERROR", "현재 비밀번호를 입력해주세요.");
            }
            if (!passwordEncoder.matches(currentPassword, staff.getPassword())) {
                throw CustomException.badRequest("VALIDATION_ERROR", "현재 비밀번호가 일치하지 않습니다.");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw CustomException.badRequest("VALIDATION_ERROR", "새 비밀번호가 일치하지 않습니다.");
            }
            staff.updatePassword(passwordEncoder.encode(newPassword));
        }
    }
}
