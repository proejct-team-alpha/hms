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
-- 병원 규칙 더미데이터 (카테고리별 60건, 총 300건)
INSERT INTO hospital_rule (title, content, category, is_active, created_at, updated_at)
SELECT
    '응급 대응 프로토콜 ' || CAST(n.x AS VARCHAR),
    '응급 호출 접수 후 1분 이내 담당 의사와 간호사에게 전파하고, 환자 상태 분류, 응급 카트 준비, 처치 기록을 순서대로 수행한다. 점검 코드 ER-' || CAST(n.x AS VARCHAR),
    'EMERGENCY',
    CASE WHEN MOD(n.x, 10) = 0 THEN FALSE ELSE TRUE END,
    DATEADD('MINUTE', -n.x, CURRENT_TIMESTAMP),
    DATEADD('MINUTE', -n.x, CURRENT_TIMESTAMP)
FROM SYSTEM_RANGE(1, 60) n;

INSERT INTO hospital_rule (title, content, category, is_active, created_at, updated_at)
SELECT
    '물품 재고 점검 규칙 ' || CAST(n.x AS VARCHAR),
    '일일 물품 라운드 시 재고 수량, 유효기간, 보관 위치, 멸균 상태를 함께 확인하고 부족 물품은 교대 종료 전까지 발주 요청한다. 점검 코드 SP-' || CAST(n.x AS VARCHAR),
    'SUPPLY',
    CASE WHEN MOD(n.x, 8) = 0 THEN FALSE ELSE TRUE END,
    DATEADD('MINUTE', -(60 + n.x), CURRENT_TIMESTAMP),
    DATEADD('MINUTE', -(60 + n.x), CURRENT_TIMESTAMP)
FROM SYSTEM_RANGE(1, 60) n;

INSERT INTO hospital_rule (title, content, category, is_active, created_at, updated_at)
SELECT
    '근무 인수인계 기준 ' || CAST(n.x AS VARCHAR),
    '교대 시작 전 환자 상태, 우선 처치 대상, 미완료 업무, 특이사항을 인수인계 체크리스트에 기록하고 담당자 서명을 남긴다. 점검 코드 DU-' || CAST(n.x AS VARCHAR),
    'DUTY',
    CASE WHEN MOD(n.x, 12) = 0 THEN FALSE ELSE TRUE END,
    DATEADD('MINUTE', -(120 + n.x), CURRENT_TIMESTAMP),
    DATEADD('MINUTE', -(120 + n.x), CURRENT_TIMESTAMP)
FROM SYSTEM_RANGE(1, 60) n;

INSERT INTO hospital_rule (title, content, category, is_active, created_at, updated_at)
SELECT
    '위생 관리 체크 규칙 ' || CAST(n.x AS VARCHAR),
    '진료 전후 손 위생을 수행하고 표면 소독, 폐기물 분리배출, 보호구 교체 여부를 체크리스트에 남긴다. 점검 코드 HY-' || CAST(n.x AS VARCHAR),
    'HYGIENE',
    CASE WHEN MOD(n.x, 15) = 0 THEN FALSE ELSE TRUE END,
    DATEADD('MINUTE', -(180 + n.x), CURRENT_TIMESTAMP),
    DATEADD('MINUTE', -(180 + n.x), CURRENT_TIMESTAMP)
FROM SYSTEM_RANGE(1, 60) n;

INSERT INTO hospital_rule (title, content, category, is_active, created_at, updated_at)
SELECT
    '병원 운영 공통 안내 ' || CAST(n.x AS VARCHAR),
    '내부 공지 확인, 문서 보관, 환자 응대, 보안 점검, 시설 이상 보고 절차를 숙지하고 근무 중 변경 사항은 즉시 공유한다. 점검 코드 OT-' || CAST(n.x AS VARCHAR),
    'OTHER',
    CASE WHEN MOD(n.x, 20) = 0 THEN FALSE ELSE TRUE END,
    DATEADD('MINUTE', -(240 + n.x), CURRENT_TIMESTAMP),
    DATEADD('MINUTE', -(240 + n.x), CURRENT_TIMESTAMP)
FROM SYSTEM_RANGE(1, 60) n;
-- ==============================================================================
