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
-- doctor01(내과/이영희): [오늘] 08:00 COMPLETED, 09:00 RECEIVED(온라인) / [과거] 2026-03-14 COMPLETED 등
-- doctor02(외과/김민준): 10:00 RECEIVED(전화)
-- doctor04(소아과/최지우): 14:00 CANCELLED
INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
('RES-20260314-001', 1, 1, 1, CURRENT_DATE, '09:00', 'RESERVED',  'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260314-002', 2, 2, 2, CURRENT_DATE, '10:00', 'RECEIVED',  'PHONE',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260314-003', 3, 4, 3, CURRENT_DATE, '14:00', 'CANCELLED', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260314-004', 1, 5, 4, CURRENT_DATE, '11:00', 'RESERVED',  'WALKIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260317-001', 1, 1, 1, CURRENT_DATE + 1, '09:00', 'RESERVED',  'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260318-001', 2, 2, 2, CURRENT_DATE + 2, '10:00', 'RESERVED',  'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260319-001', 3, 4, 3, CURRENT_DATE + 2, '14:00', 'RESERVED',  'PHONE',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 진료 기록 (RES-20260314-000: 과거 완료 / RES-20260317-000: 오늘 완료 — 진료완료 목록 테스트)
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '상기도염', '해열제 3일치, 항생제 5일치 처방', '3일 후 재진 권고', CURRENT_TIMESTAMP
FROM reservation r WHERE r.reservation_number = 'RES-20260314-000';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '급성 위염', '제산제 7일치 처방', '', CURRENT_TIMESTAMP
FROM reservation r WHERE r.reservation_number = 'RES-20260317-000';

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
-- [추가] 히스토리 테스트용 더미 데이터 (김명준 환자: 010-1111-2222)
-- ==============================================================================

-- 1. 과거 예약 데이터 (상태: COMPLETED)
INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
('RES-20260310-999', 1, 1, 1, '2026-03-10', '10:30', 'COMPLETED', 'PHONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260312-999', 1, 5, 4, '2026-03-12', '15:00', 'COMPLETED', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260305-999', 1, 2, 2, '2026-03-05', '11:00', 'COMPLETED', 'WALKIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260308-999', 1, 1, 1, '2026-03-08', '09:30', 'COMPLETED', 'PHONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- 추가: 취소된 내역 (원무과 히스토리 확인용)
('RES-20260220-000', 1, 2, 2, '2026-02-20', '14:00', 'CANCELLED', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-20260301-000', 1, 5, 4, '2026-03-01', '16:30', 'CANCELLED', 'WALKIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- 추가: 아주 오래된 완료 내역 (페이징 2페이지 이상 확인용)
('RES-20260115-000', 1, 1, 1, '2026-01-15', '10:00', 'COMPLETED', 'PHONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- 추가: 이서연 환자(ID:2) 재진 테스트용 (완료 기록 1건 추가)
('RES-20260301-002', 2, 2, 2, '2026-03-01', '11:00', 'COMPLETED', 'PHONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. 위 예약에 대한 진료 기록 (Treatment Record)
-- 2026-03-10 내과 진료 기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '만성 위염', '알마겔 1포, 무코스타 1정 (7일분 처방)', '자극적인 음식 피하고 식후 30분 복용 지도함 (SOAP TEST)', CURRENT_TIMESTAMP
FROM reservation r WHERE r.reservation_number = 'RES-20260310-999';

-- 2026-03-12 이비인후과 진료 기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 5, '알레르기성 비염', '씨잘정 1정, 나조넥스 나잘스프레이 (14일분)', '침구류 세탁 및 환기 자주 할 것을 권고함 (SOAP TEST)', CURRENT_TIMESTAMP
FROM reservation r WHERE r.reservation_number = 'RES-20260312-999';

-- 2026-03-05 외과 진료 기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 2, '장염 의증', '스멕타 1포, 에어탈 1정 (3일분)', '탈수 예방 위해 미지근한 물 자주 섭취 지도함 (SOAP TEST)', CURRENT_TIMESTAMP
FROM reservation r WHERE r.reservation_number = 'RES-20260305-999';

-- 2026-03-08 내과 진료 기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '단순 몸살 감기', '타이레놀 ER 1정 (2일분)', '충분한 휴식과 수면 권고함 (SOAP TEST)', CURRENT_TIMESTAMP
FROM reservation r WHERE r.reservation_number = 'RES-20260308-999';

-- 2026-01-15 내과 진료 기록 (김명준 환자 오래된 기록)
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '역류성 식도염', '넥시움정 20mg (28일분)', '취침 3시간 전 음식 섭취 금지 안내 (SOAP TEST)', CURRENT_TIMESTAMP
FROM reservation r WHERE r.reservation_number = 'RES-20260115-000';

-- 2026-03-01 외과 진료 기록 (이서연 환자 완료 기록)
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 2, '찰과상', '드레싱 및 항생제 연고 도포', '상처 부위 물 닿지 않게 주의 당부함', CURRENT_TIMESTAMP
FROM reservation r WHERE r.reservation_number = 'RES-20260301-002';

-- 3. 취소 사유 업데이트 (김명준 환자 취소 건)
UPDATE reservation SET cancellation_reason = '갑작스러운 출장으로 인한 일정 변경' WHERE reservation_number = 'RES-20260220-000';
UPDATE reservation SET cancellation_reason = '단순 변심 및 타 병원 방문' WHERE reservation_number = 'RES-20260301-000';

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
--
