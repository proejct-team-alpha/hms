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
(3, 'doctor01', 'D-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '이영희', 'DOCTOR', 1, true, CURRENT_TIMESTAMP),
(4, 'nurse01', 'N-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '간호사박민수', 'NURSE', 1, true, CURRENT_TIMESTAMP),
(5, 'item01', 'I-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '물품담당최지훈', 'ITEM_MANAGER', null, true, CURRENT_TIMESTAMP),
(6, 'doctor02', 'D-20260102', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '김민준', 'DOCTOR', 2, true, CURRENT_TIMESTAMP),
(7, 'doctor03', 'D-20260103', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '박서연', 'DOCTOR', 2, true, CURRENT_TIMESTAMP),
(8, 'doctor04', 'D-20260104', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '최지우', 'DOCTOR', 3, true, CURRENT_TIMESTAMP),
(9, 'doctor05', 'D-20260105', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '이준혁', 'DOCTOR', 4, true, CURRENT_TIMESTAMP);

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

-- [이영희 의사 전용 오늘 오전 진료 데이터 - 테스트용]
INSERT INTO patient (id, name, phone, email, created_at) VALUES
(201, '강호동', '010-1111-1111', 'hodong@test.com', CURRENT_TIMESTAMP),
(202, '유재석', '010-2222-2222', 'jaeseok@test.com', CURRENT_TIMESTAMP),
(203, '신동엽', '010-3333-3333', 'dongyeop@test.com', CURRENT_TIMESTAMP),
(204, '김구라', '010-4444-4444', 'gura@test.com', CURRENT_TIMESTAMP);

INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
('RES-LEE-004', 204, 1, 1, CURRENT_DATE, '10:30', 'RESERVED',  'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [이영희 의사 전용 대규모 테스트 데이터]
-- 환자 데이터 보강 (birth_info 필드명 변경 적용)
INSERT INTO patient (id, name, phone, email, birth_info, visit_reason, created_at) VALUES
(301, '박노인', '010-3333-0001', 'park@test.com', '600101-1', '만성 고혈압 정기 검진', CURRENT_TIMESTAMP),
(302, '이지혜', '010-3333-0002', 'lee@test.com',  '880808-2', '심한 편두통 및 어지럼증', CURRENT_TIMESTAMP),
(303, '최청년', '010-3333-0003', 'choi@test.com', '050505-3', '축구 중 발목 염좌', CURRENT_TIMESTAMP),
(304, '김학생', '010-3333-0004', 'kim@test.com',  '101010-4', '환절기 알레르기 비염', CURRENT_TIMESTAMP),
(305, '정중년', '010-3333-0005', 'jung@test.com', '750505-1', '건강검진 결과 상담', CURRENT_TIMESTAMP),
(306, '한미소', '010-3333-0006', 'han@test.com',  '991231-2', '갑작스러운 복통과 구토', CURRENT_TIMESTAMP),
(307, '미래인', '010-3333-0007', 'future@test.com','910101-1', '내일 예정된 수술 상담', CURRENT_TIMESTAMP);

-- 과거 데이터 (그저께 완료)
INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
('RES-DUMMY-001', 301, 1, 1, DATEADD('DAY', -2, CURRENT_DATE), '09:00', 'COMPLETED', 'PHONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-DUMMY-002', 302, 1, 1, DATEADD('DAY', -2, CURRENT_DATE), '10:00', 'COMPLETED', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at) VALUES
((SELECT id FROM reservation WHERE reservation_number='RES-DUMMY-001'), 1, '본태성 고혈압', '암로디핀 5mg 1일 1회', '혈압 안정적임, 지속 관찰 요망', CURRENT_TIMESTAMP),
((SELECT id FROM reservation WHERE reservation_number='RES-DUMMY-002'), 1, '긴장성 두통', '타이레놀 ER 650mg', '스트레스 관리 및 충분한 휴식 권고', CURRENT_TIMESTAMP);

-- 과거 데이터 (어제 완료)
INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
('RES-DUMMY-003', 303, 1, 1, DATEADD('DAY', -1, CURRENT_DATE), '14:00', 'COMPLETED', 'WALKIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-DUMMY-004', 304, 1, 1, DATEADD('DAY', -1, CURRENT_DATE), '15:30', 'COMPLETED', 'PHONE',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at) VALUES
((SELECT id FROM reservation WHERE reservation_number='RES-DUMMY-003'), 1, '발목 외측 인대 염좌', '소염진통제 및 물리치료', '반깁스 1주일 유지 권장', CURRENT_TIMESTAMP),
((SELECT id FROM reservation WHERE reservation_number='RES-DUMMY-004'), 1, '알레르기성 비염', '항히스타민제 제제', '외출 시 마스크 착용 당부', CURRENT_TIMESTAMP);

-- 오늘 데이터 추가 (진료 중 및 대기)
INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
('RES-NOW-001', 305, 1, 1, CURRENT_DATE, '11:00', 'IN_TREATMENT', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('RES-NOW-002', 306, 1, 1, CURRENT_DATE, '11:30', 'RECEIVED',     'WALKIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 미래 데이터 (내일 예약)
INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
('RES-FUTURE-001', 307, 1, 1, DATEADD('DAY', 1, CURRENT_DATE), '09:30', 'RESERVED', 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


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