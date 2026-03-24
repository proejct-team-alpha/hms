package com.smartclinic.hms.auth;

import com.smartclinic.hms.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Staff 엔티티 조회 Repository — Spring Security 인증 연동용
 */
public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUsername(String username);

    Optional<Staff> findByUsernameAndActiveTrue(String username);
}
