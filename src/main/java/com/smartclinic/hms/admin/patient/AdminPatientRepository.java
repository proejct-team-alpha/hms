package com.smartclinic.hms.admin.patient;

import com.smartclinic.hms.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminPatientRepository extends JpaRepository<Patient, Long> {

    @Query("""
            SELECT p
            FROM Patient p
            WHERE (:nameKeyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :nameKeyword, '%')))
              AND (:contactKeyword = '' OR REPLACE(p.phone, '-', '') LIKE CONCAT('%', :contactKeyword, '%'))
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    Page<Patient> search(
            @Param("nameKeyword") String nameKeyword,
            @Param("contactKeyword") String contactKeyword,
            Pageable pageable);
}
