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

-- [기본 예약 샘플] (시간대가 겹치지 않게 주의)
INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
('RES-BASE-001', 1, 1, 1, CURRENT_DATE, '14:00', 'RESERVED',  'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-BASE-002', 2, 2, 2, CURRENT_DATE, '10:00', 'RECEIVED',  'PHONE',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-BASE-003', 3, 4, 3, CURRENT_DATE, '14:30', 'CANCELLED', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-BASE-004', 1, 5, 4, CURRENT_DATE, '11:00', 'RESERVED',  'WALKIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-BASE-005', 1, 1, 1, CURRENT_DATE + 1, '09:00', 'RESERVED',  'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [오늘 완료 데이터] 의사 이영희 (시간: 18:00, 18:30, 19:00 - 다른 데이터와 절대 안겹침)
INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
('RES-DONE-001', 1, 1, 1, CURRENT_DATE, '18:00', 'COMPLETED', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-DONE-002', 2, 1, 1, CURRENT_DATE, '18:30', 'COMPLETED', 'PHONE',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-DONE-003', 3, 1, 1, CURRENT_DATE, '19:00', 'COMPLETED', 'WALKIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [과거 완료 데이터] 히스토리 확인용 (김명준 환자)
INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
('RES-PAST-001', 1, 1, 1, '2026-03-10', '10:30', 'COMPLETED', 'PHONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-PAST-002', 1, 5, 4, '2026-03-12', '15:00', 'COMPLETED', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-PAST-003', 1, 2, 2, '2026-03-05', '11:00', 'COMPLETED', 'WALKIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-PAST-004', 1, 1, 1, '2026-03-08', '09:30', 'COMPLETED', 'PHONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-PAST-005', 1, 1, 1, '2026-01-15', '10:00', 'COMPLETED', 'PHONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-PAST-006', 1, 2, 2, '2026-02-20', '14:00', 'CANCELLED', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-PAST-007', 1, 5, 4, '2026-03-01', '16:30', 'CANCELLED', 'WALKIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 진료 기록 (Treatment Record)
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '급성 위염', '큐란 1정, 가스모틴 1정', '안정 필요', CURRENT_TIMESTAMP FROM reservation r WHERE r.reservation_number = 'RES-DONE-001';
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '결막염', '레보플록사신 점안액', '렌즈 금지', CURRENT_TIMESTAMP FROM reservation r WHERE r.reservation_number = 'RES-DONE-002';
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '피부염', '베포리진 1정', '긁지 말 것', CURRENT_TIMESTAMP FROM reservation r WHERE r.reservation_number = 'RES-DONE-003';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '만성 위염', '알마겔 1포', '식후 복용', CURRENT_TIMESTAMP FROM reservation r WHERE r.reservation_number = 'RES-PAST-001';
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 5, '비염', '씨잘정 1정', '환기 자주', CURRENT_TIMESTAMP FROM reservation r WHERE r.reservation_number = 'RES-PAST-002';
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 2, '장염', '스멕타 1포', '수분 섭취', CURRENT_TIMESTAMP FROM reservation r WHERE r.reservation_number = 'RES-PAST-003';
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '몸살', '타이레놀 ER', '휴식', CURRENT_TIMESTAMP FROM reservation r WHERE r.reservation_number = 'RES-PAST-004';
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '식도염', '넥시움정', '금식', CURRENT_TIMESTAMP FROM reservation r WHERE r.reservation_number = 'RES-PAST-005';

-- 취소 사유
UPDATE reservation SET cancellation_reason = '개인 사정' WHERE reservation_number = 'RES-PAST-006';
UPDATE reservation SET cancellation_reason = '단순 변심' WHERE reservation_number = 'RES-PAST-007';

-- 물품 샘플
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
