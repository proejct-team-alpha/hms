-- ==============================================================================
-- HMS H2 테스트 데이터 & 테스트 로그인 정보
-- ==============================================================================
-- 비밀번호: password123 (BCrypt 해시)
-- 실행: Spring Boot dev 프로필 + spring.sql.init.data-locations
-- ==============================================================================

-- 진료과
INSERT INTO department (id, name, is_active) VALUES
(1, '내과', true),
(2, '외과', true),
(3, '소아과', true),
(4, '이비인후과', true);

-- 직원 (password123 BCrypt: $2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa)
INSERT INTO staff (id, username, employee_number, password, name, role, department_id, is_active, created_at) VALUES
(1, 'admin01', 'A-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '관리자홍길동', 'ADMIN', null, true, CURRENT_TIMESTAMP),
(2, 'staff01', 'S-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '접수김철수', 'STAFF', 1, true, CURRENT_TIMESTAMP),
(3, 'doctor01', 'D-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사이영희', 'DOCTOR', 1, true, CURRENT_TIMESTAMP),
(4, 'nurse01', 'N-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '간호사박민수', 'NURSE', 1, true, CURRENT_TIMESTAMP),
(5, 'item01', 'I-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '물품담당최지훈', 'ITEM_MANAGER', null, true, CURRENT_TIMESTAMP),
(6, 'doctor02', 'D-20260102', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사김민준', 'DOCTOR', 2, true, CURRENT_TIMESTAMP),
(7, 'doctor03', 'D-20260103', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사박서연', 'DOCTOR', 2, true, CURRENT_TIMESTAMP),
(8, 'doctor04', 'D-20260104', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사최지우', 'DOCTOR', 3, true, CURRENT_TIMESTAMP),
(9, 'doctor05', 'D-20260105', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사이준혁', 'DOCTOR', 4, true, CURRENT_TIMESTAMP);

-- 의사
INSERT INTO doctor (id, staff_id, department_id, available_days, specialty) VALUES
(1, 3, 1, 'MON,TUE,WED,THU,FRI', '소화기내과'),
(2, 6, 2, 'MON,WED,FRI',         '외상외과'),
(3, 7, 2, 'TUE,THU',             '복강경외과'),
(4, 8, 3, 'MON,TUE,WED,THU,FRI', '소아일반'),
(5, 9, 4, 'MON,TUE,WED,THU,FRI', '이비인후일반');

-- ==============================================================================
-- 테스트 로그인 정보
-- ==============================================================================
-- | 아이디   | 비밀번호    | 역할        |
-- |----------|-------------|-------------|
-- | admin01  | password123 | ADMIN       |
-- | staff01  | password123 | STAFF       |
-- | doctor01 | password123 | DOCTOR      |
-- | nurse01  | password123 | NURSE       |
-- | item01   | password123 | ITEM_MANAGER|
-- ==============================================================================
