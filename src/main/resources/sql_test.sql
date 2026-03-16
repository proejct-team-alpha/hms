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

-- 환자 샘플
INSERT INTO patient (id, name, phone, email, created_at) VALUES
(1, '김명준', '010-1111-2222', 'minjun@test.com', CURRENT_TIMESTAMP),
(2, '이서연', '010-3333-4444', 'seoyeon@test.com', CURRENT_TIMESTAMP),
(3, '박지호', '010-5555-6666', 'jiho@test.com', CURRENT_TIMESTAMP);

-- 예약 샘플 (RES-YYYYMMDD-XXX 형식)
-- doctor01(내과/이영희): 08:00 COMPLETED, 09:00 RECEIVED(온라인), 11:00 RECEIVED(방문)
-- doctor02(외과/김민준): 10:00 RECEIVED(전화)
-- doctor04(소아과/최지우): 14:00 CANCELLED
INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
('RES-20260314-000', 3, 1, 1, CURRENT_DATE, '08:00', 'COMPLETED', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260314-001', 1, 1, 1, CURRENT_DATE, '09:00', 'RECEIVED',  'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260314-002', 2, 2, 2, CURRENT_DATE, '10:00', 'RECEIVED',  'PHONE',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260314-003', 3, 4, 3, CURRENT_DATE, '14:00', 'CANCELLED', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260314-004', 1, 1, 1, CURRENT_DATE, '11:00', 'RECEIVED',  'WALKIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260317-001', 1, 1, 1, '2026-03-17', '09:00', 'RESERVED',  'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260318-001', 2, 2, 2, '2026-03-18', '10:00', 'RESERVED',  'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260319-001', 3, 4, 3, '2026-03-19', '14:00', 'RESERVED',  'PHONE',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 진료 기록 (RES-20260314-000: 박지호 08:00 완료 — 진료완료 목록 연동)
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '상기도염', '해열제 3일치, 항생제 5일치 처방', '3일 후 재진 권고', CURRENT_TIMESTAMP
FROM reservation r WHERE r.reservation_number = 'RES-20260314-000';

-- 물품 샘플 (item01 물품 담당자 기능 테스트용)
-- 재고 부족(quantity < min_quantity): 주사기, 알코올솜, 혈압계
-- 정상 재고: 나머지
INSERT INTO item (name, category, quantity, min_quantity, created_at, updated_at) VALUES
('주사기 (5ml)',       'MEDICAL_SUPPLIES',  8,  50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('알코올솜',           'MEDICAL_SUPPLIES',  20, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('일회용 장갑 (M)',    'MEDICAL_SUPPLIES',  150, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('거즈 패드',          'MEDICAL_SUPPLIES',  200, 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('혈압계',             'MEDICAL_EQUIPMENT', 2,  5,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('체온계',             'MEDICAL_EQUIPMENT', 10, 5,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('청진기',             'MEDICAL_EQUIPMENT', 8,  3,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('A4 용지 (박스)',     'GENERAL_SUPPLIES',  5,  2,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('볼펜',               'GENERAL_SUPPLIES',  30, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('마스크 (KF94)',      'MEDICAL_SUPPLIES',  3,  50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

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
