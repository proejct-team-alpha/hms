-- ==============================================================================
-- HMS MySQL 테스트 데이터 & 테스트 로그인 정보
-- ==============================================================================
-- 비밀번호: password123 (BCrypt 해시)
-- 실행: Spring Boot prod 프로필 + spring.sql.init.data-locations
-- 대상 DB: MySQL 8.0+
-- ==============================================================================

-- FK 제약 임시 비활성화 (데이터 삽입 순서 보장)
SET FOREIGN_KEY_CHECKS = 0;

-- ==============================================================================
-- 0. 기존 데이터 전체 삭제 (FK 역순)
-- ==============================================================================
DELETE FROM item_usage_log;
DELETE FROM item_stock_log;
DELETE FROM treatment_record;
DELETE FROM reservation;
DELETE FROM doctor_schedule;
DELETE FROM doctor;
DELETE FROM chatbot_history;
DELETE FROM medical_history;
DELETE FROM llm_recommendation;
DELETE FROM hospital_rule;
DELETE FROM item;
DELETE FROM patient;
DELETE FROM staff;
DELETE FROM department;

-- AUTO_INCREMENT 초기화
ALTER TABLE item_usage_log    AUTO_INCREMENT = 1;
ALTER TABLE item_stock_log    AUTO_INCREMENT = 1;
ALTER TABLE treatment_record  AUTO_INCREMENT = 1;
ALTER TABLE reservation       AUTO_INCREMENT = 1;
ALTER TABLE doctor_schedule   AUTO_INCREMENT = 1;
ALTER TABLE doctor            AUTO_INCREMENT = 1;
ALTER TABLE chatbot_history   AUTO_INCREMENT = 1;
ALTER TABLE medical_history   AUTO_INCREMENT = 1;
ALTER TABLE llm_recommendation AUTO_INCREMENT = 1;
ALTER TABLE hospital_rule     AUTO_INCREMENT = 1;
ALTER TABLE item              AUTO_INCREMENT = 1;
ALTER TABLE patient           AUTO_INCREMENT = 1;
ALTER TABLE staff             AUTO_INCREMENT = 1;
ALTER TABLE department        AUTO_INCREMENT = 1;

-- ==============================================================================
-- 1. 진료과 (Department)
-- ==============================================================================
INSERT INTO department (id, name, is_active) VALUES
(1, '내과',       TRUE),
(2, '외과',       TRUE),
(3, '소아과',     TRUE),
(4, '이비인후과', TRUE),
(5, '정형외과',   TRUE),
(6, '피부과',     TRUE),
(7, '안과',       TRUE),
(8, '산부인과',   TRUE);

-- ==============================================================================
-- 2. 직원 (Staff)
--    password123 BCrypt: $2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa
-- ==============================================================================
INSERT INTO staff (id, username, employee_number, password, name, role, department_id, is_active, email, phone, created_at) VALUES
-- 관리자
(1,  'admin01',  'A-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '관리자홍길동', 'ADMIN',        NULL, TRUE, 'admin01@smartclinic.com',  '010-0000-0001', NOW()),
(2,  'admin02',  'A-20260102', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '관리자김영수', 'ADMIN',        NULL, TRUE, 'admin02@smartclinic.com',  '010-0000-0002', NOW()),
-- 접수 직원
(3,  'staff01',  'S-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '접수김철수',   'STAFF',        1,    TRUE, 'staff01@smartclinic.com',  '010-1000-0001', NOW()),
(4,  'staff02',  'S-20260102', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '접수박영희',   'STAFF',        2,    TRUE, 'staff02@smartclinic.com',  '010-1000-0002', NOW()),
(5,  'staff03',  'S-20260103', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '접수이민지',   'STAFF',        3,    TRUE, 'staff03@smartclinic.com',  '010-1000-0003', NOW()),
-- 의사
(6,  'doctor01', 'D-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사이영희',   'DOCTOR',       1,    TRUE, 'doctor01@smartclinic.com', '010-2000-0001', NOW()),
(7,  'doctor02', 'D-20260102', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사김민준',   'DOCTOR',       2,    TRUE, 'doctor02@smartclinic.com', '010-2000-0002', NOW()),
(8,  'doctor03', 'D-20260103', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사박서연',   'DOCTOR',       2,    TRUE, 'doctor03@smartclinic.com', '010-2000-0003', NOW()),
(9,  'doctor04', 'D-20260104', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사최지우',   'DOCTOR',       3,    TRUE, 'doctor04@smartclinic.com', '010-2000-0004', NOW()),
(10, 'doctor05', 'D-20260105', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사이준혁',   'DOCTOR',       4,    TRUE, 'doctor05@smartclinic.com', '010-2000-0005', NOW()),
(11, 'doctor06', 'D-20260106', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사한지민',   'DOCTOR',       5,    TRUE, 'doctor06@smartclinic.com', '010-2000-0006', NOW()),
(12, 'doctor07', 'D-20260107', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사정우성',   'DOCTOR',       6,    TRUE, 'doctor07@smartclinic.com', '010-2000-0007', NOW()),
(13, 'doctor08', 'D-20260108', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사송혜교',   'DOCTOR',       7,    TRUE, 'doctor08@smartclinic.com', '010-2000-0008', NOW()),
(14, 'doctor09', 'D-20260109', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사공유진',   'DOCTOR',       8,    TRUE, 'doctor09@smartclinic.com', '010-2000-0009', NOW()),
(15, 'doctor10', 'D-20260110', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사김태희',   'DOCTOR',       1,    TRUE, 'doctor10@smartclinic.com', '010-2000-0010', NOW()),
-- 간호사
(16, 'nurse01',  'N-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '간호사박민수', 'NURSE',        1,    TRUE, 'nurse01@smartclinic.com',  '010-3000-0001', NOW()),
(17, 'nurse02',  'N-20260102', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '간호사김소연', 'NURSE',        2,    TRUE, 'nurse02@smartclinic.com',  '010-3000-0002', NOW()),
(18, 'nurse03',  'N-20260103', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '간호사이하늘', 'NURSE',        3,    TRUE, 'nurse03@smartclinic.com',  '010-3000-0003', NOW()),
(19, 'nurse04',  'N-20260104', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '간호사정다은', 'NURSE',        4,    TRUE, 'nurse04@smartclinic.com',  '010-3000-0004', NOW()),
(20, 'nurse05',  'N-20260105', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '간호사최예진', 'NURSE',        5,    TRUE, 'nurse05@smartclinic.com',  '010-3000-0005', NOW()),
-- 물품 관리자
(21, 'item01',   'I-20260101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '물품담당최지훈', 'ITEM_MANAGER', NULL, TRUE, 'item01@smartclinic.com', '010-4000-0001', NOW()),
(22, 'item02',   'I-20260102', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '물품담당양서윤', 'ITEM_MANAGER', NULL, TRUE, 'item02@smartclinic.com', '010-4000-0002', NOW()),
-- 비활성 직원 (퇴직)
(23, 'doctor99', 'D-20250101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '전직의사퇴직', 'DOCTOR',       1,    FALSE, NULL, NULL, NOW()),
(24, 'staff99',  'S-20250101', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '전직접수퇴직', 'STAFF',        2,    FALSE, NULL, NULL, NOW());

-- ==============================================================================
-- 3. 의사 (Doctor)
-- ==============================================================================
INSERT INTO doctor (id, staff_id, department_id, available_days, specialty) VALUES
(1,  6,  1, 'MON,TUE,WED,THU,FRI', '소화기내과'),
(2,  7,  2, 'MON,WED,FRI',          '외상외과'),
(3,  8,  2, 'TUE,THU',              '복강경외과'),
(4,  9,  3, 'MON,TUE,WED,THU,FRI', '소아일반'),
(5,  10, 4, 'MON,TUE,WED,THU,FRI', '이비인후일반'),
(6,  11, 5, 'MON,WED,FRI',          '관절외과'),
(7,  12, 6, 'TUE,THU,FRI',          '일반피부과'),
(8,  13, 7, 'MON,TUE,WED,THU',     '안과일반'),
(9,  14, 8, 'MON,WED,FRI',          '산부인과일반'),
(10, 15, 1, 'MON,TUE,WED',          '호흡기내과');

-- ==============================================================================
-- 4. 의사 스케줄 (Doctor Schedule)
-- ==============================================================================
INSERT INTO doctor_schedule (doctor_id, day_of_week, start_time, end_time, max_patients, is_available, note) VALUES
-- 의사 이영희 (소화기내과)
(1, 'MON', '09:00', '12:00', 20, TRUE,  '오전 진료'),
(1, 'MON', '14:00', '18:00', 15, TRUE,  '오후 진료'),
(1, 'TUE', '09:00', '12:00', 20, TRUE,  NULL),
(1, 'TUE', '14:00', '18:00', 15, TRUE,  NULL),
(1, 'WED', '09:00', '12:00', 20, TRUE,  NULL),
(1, 'WED', '14:00', '18:00', 15, TRUE,  NULL),
(1, 'THU', '09:00', '12:00', 20, TRUE,  NULL),
(1, 'THU', '14:00', '18:00', 15, TRUE,  NULL),
(1, 'FRI', '09:00', '12:00', 20, TRUE,  NULL),
(1, 'FRI', '14:00', '17:00', 10, TRUE,  '금요일 조기 마감'),
-- 의사 김민준 (외상외과)
(2, 'MON', '09:00', '12:00', 15, TRUE,  NULL),
(2, 'MON', '14:00', '18:00', 15, TRUE,  NULL),
(2, 'WED', '09:00', '12:00', 15, TRUE,  NULL),
(2, 'WED', '14:00', '18:00', 15, TRUE,  NULL),
(2, 'FRI', '09:00', '12:00', 15, TRUE,  NULL),
-- 의사 박서연 (복강경외과)
(3, 'TUE', '09:00', '17:00', 12, TRUE,  '종일 진료'),
(3, 'THU', '09:00', '17:00', 12, TRUE,  '종일 진료'),
-- 의사 최지우 (소아일반)
(4, 'MON', '09:00', '12:00', 25, TRUE,  NULL),
(4, 'MON', '14:00', '18:00', 20, TRUE,  NULL),
(4, 'TUE', '09:00', '12:00', 25, TRUE,  NULL),
(4, 'WED', '09:00', '12:00', 25, TRUE,  NULL),
(4, 'THU', '09:00', '12:00', 25, TRUE,  NULL),
(4, 'FRI', '09:00', '12:00', 25, TRUE,  NULL),
-- 의사 이준혁 (이비인후일반)
(5, 'MON', '09:00', '12:00', 18, TRUE,  NULL),
(5, 'TUE', '09:00', '12:00', 18, TRUE,  NULL),
(5, 'WED', '09:00', '12:00', 18, TRUE,  NULL),
(5, 'THU', '09:00', '12:00', 18, TRUE,  NULL),
(5, 'FRI', '09:00', '12:00', 18, TRUE,  NULL),
-- 의사 한지민 (관절외과)
(6, 'MON', '10:00', '13:00', 12, TRUE,  NULL),
(6, 'WED', '10:00', '13:00', 12, TRUE,  NULL),
(6, 'FRI', '10:00', '13:00', 12, TRUE,  NULL),
-- 의사 정우성 (일반피부과)
(7, 'TUE', '09:00', '12:00', 20, TRUE,  NULL),
(7, 'THU', '09:00', '12:00', 20, TRUE,  NULL),
(7, 'FRI', '14:00', '17:00', 15, TRUE,  NULL),
-- 의사 송혜교 (안과일반)
(8, 'MON', '09:00', '12:00', 16, TRUE,  NULL),
(8, 'TUE', '09:00', '12:00', 16, TRUE,  NULL),
(8, 'WED', '09:00', '12:00', 16, TRUE,  NULL),
(8, 'THU', '09:00', '12:00', 16, TRUE,  NULL),
-- 의사 공유진 (산부인과)
(9, 'MON', '09:00', '12:00', 15, TRUE,  NULL),
(9, 'WED', '09:00', '12:00', 15, TRUE,  NULL),
(9, 'FRI', '09:00', '12:00', 15, TRUE,  NULL),
-- 의사 김태희 (호흡기내과)
(10, 'MON', '09:00', '12:00', 18, TRUE, NULL),
(10, 'TUE', '09:00', '12:00', 18, TRUE, NULL),
(10, 'WED', '09:00', '12:00', 18, TRUE, NULL);

-- ==============================================================================
-- 5. 환자 (Patient) — 기존 + 추가 데이터
-- ==============================================================================
INSERT INTO patient (id, name, phone, email, address, note, created_at) VALUES
-- 기존 환자
(1,   '김명준', '010-1111-2222', 'minjun@test.com',    '서울시 강남구 역삼동',      '정기 건강검진',              NOW()),
(2,   '이서연', '010-3333-4444', 'seoyeon@test.com',    '서울시 서초구 반포동',      '두통 및 어지럼증',           NOW()),
(3,   '박지호', '010-5555-6666', 'jiho@test.com',       '서울시 송파구 잠실동',      '무릎 통증',                  NOW()),
-- 추가 환자 (이영희 의사 전용)
(201, '강호동', '010-1111-1111', 'hodong@test.com',     '서울시 마포구 합정동',      '소화불량',                   NOW()),
(202, '유재석', '010-2222-2222', 'jaeseok@test.com',    '서울시 강남구 청담동',      '만성 기침',                  NOW()),
(203, '신동엽', '010-3333-3333', 'dongyeop@test.com',   '서울시 용산구 이태원동',    '허리 통증',                  NOW()),
(204, '김구라', '010-4444-4444', 'gura@test.com',       '서울시 종로구 삼청동',      '위장 장애',                  NOW()),
-- 대규모 테스트 환자
(301, '박노인', '010-3333-0001', 'park@test.com',       '서울시 노원구 상계동',      '만성 고혈압 정기 검진',      NOW()),
(302, '이지혜', '010-3333-0002', 'lee@test.com',        '서울시 강동구 천호동',      '심한 편두통 및 어지럼증',    NOW()),
(303, '최청년', '010-3333-0003', 'choi@test.com',       '서울시 관악구 신림동',      '축구 중 발목 염좌',          NOW()),
(304, '김학생', '010-3333-0004', 'kim@test.com',        '서울시 동작구 사당동',      '환절기 알레르기 비염',       NOW()),
(305, '정중년', '010-3333-0005', 'jung@test.com',       '서울시 성북구 정릉동',      '건강검진 결과 상담',         NOW()),
(306, '한미소', '010-3333-0006', 'han@test.com',        '서울시 광진구 자양동',      '갑작스러운 복통과 구토',     NOW()),
(307, '미래인', '010-3333-0007', 'future@test.com',     '서울시 중구 명동',          '내일 예정된 수술 상담',      NOW()),
-- 추가 환자 (다양한 진료과 이용)
(401, '윤도현', '010-4001-0001', 'yoon@test.com',       '경기도 수원시 영통구',      '피부 발진',                  NOW()),
(402, '장윤정', '010-4001-0002', 'jang@test.com',       '경기도 성남시 분당구',      '시력 저하',                  NOW()),
(403, '김종국', '010-4001-0003', 'jongkook@test.com',   '경기도 용인시 수지구',      '어깨 통증',                  NOW()),
(404, '하하',   '010-4001-0004', 'haha@test.com',       '서울시 영등포구 여의도동',  '잦은 감기',                  NOW()),
(405, '송지효', '010-4001-0005', 'jihyo@test.com',      '서울시 강서구 등촌동',      '임신 정기 검진',             NOW()),
(406, '지석진', '010-4001-0006', 'sukjin@test.com',     '서울시 마포구 상수동',      '당뇨 정기 관리',             NOW()),
(407, '양세찬', '010-4001-0007', 'sechan@test.com',     '서울시 구로구 개봉동',      '귀 통증',                    NOW()),
(408, '전소민', '010-4001-0008', 'somin@test.com',      '서울시 은평구 녹번동',      '편도선 비대',                NOW()),
(409, '이광수', '010-4001-0009', 'kwangsoo@test.com',   '서울시 서대문구 연희동',    '무릎 인대 통증',             NOW()),
(410, '김희선', '010-4001-0010', 'heesun@test.com',     '서울시 강남구 압구정동',    '아토피 피부염',              NOW()),
(411, '조세호', '010-4001-0011', 'seho@test.com',       '서울시 동대문구 회기동',    '안검하수',                   NOW()),
(412, '박나래', '010-4001-0012', 'narae@test.com',      '서울시 성동구 성수동',      '소아 예방접종 (자녀)',       NOW()),
(413, '이수근', '010-4001-0013', 'sugeun@test.com',     '경기도 고양시 일산서구',    '만성 위염',                  NOW()),
(414, '김숙',   '010-4001-0014', 'sook@test.com',       '서울시 종로구 평창동',      '골다공증 검사',              NOW()),
(415, '신봉선', '010-4001-0015', 'bongseon@test.com',   '서울시 강북구 미아동',      '두드러기',                   NOW());

-- ==============================================================================
-- 6. 예약 (Reservation) — 기본 샘플
-- ==============================================================================
INSERT INTO reservation (reservation_number, patient_id, doctor_id, department_id, reservation_date, time_slot, status, source, created_at, updated_at) VALUES
-- [기본 예약 샘플]
('RES-BASE-001', 1,   1, 1, CURRENT_DATE,                    '14:00', 'RESERVED',     'ONLINE', NOW(), NOW()),
('RES-BASE-002', 2,   2, 2, CURRENT_DATE,                    '10:00', 'RECEIVED',     'PHONE',  NOW(), NOW()),
('RES-BASE-003', 3,   4, 3, CURRENT_DATE,                    '14:30', 'CANCELLED',    'ONLINE', NOW(), NOW()),
('RES-BASE-004', 1,   5, 4, CURRENT_DATE,                    '11:00', 'RESERVED',     'WALKIN', NOW(), NOW()),
('RES-BASE-005', 1,   1, 1, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '09:00', 'RESERVED', 'ONLINE', NOW(), NOW()),

-- [오늘 완료 데이터] 의사 이영희
('RES-DONE-001', 1,   1, 1, CURRENT_DATE, '18:00', 'COMPLETED', 'ONLINE', NOW(), NOW()),
('RES-DONE-002', 2,   1, 1, CURRENT_DATE, '18:30', 'COMPLETED', 'PHONE',  NOW(), NOW()),
('RES-DONE-003', 3,   1, 1, CURRENT_DATE, '19:00', 'COMPLETED', 'WALKIN', NOW(), NOW()),

-- [과거 완료 데이터] 김명준 환자 히스토리
('RES-PAST-001', 1,   1, 1, '2026-03-10', '10:30', 'COMPLETED', 'PHONE',  NOW(), NOW()),
('RES-PAST-002', 1,   5, 4, '2026-03-12', '15:00', 'COMPLETED', 'ONLINE', NOW(), NOW()),
('RES-PAST-003', 1,   2, 2, '2026-03-05', '11:00', 'COMPLETED', 'WALKIN', NOW(), NOW()),
('RES-PAST-004', 1,   1, 1, '2026-03-08', '09:30', 'COMPLETED', 'PHONE',  NOW(), NOW()),
('RES-PAST-005', 1,   1, 1, '2026-01-15', '10:00', 'COMPLETED', 'PHONE',  NOW(), NOW()),
('RES-PAST-006', 1,   2, 2, '2026-02-20', '14:00', 'CANCELLED', 'ONLINE', NOW(), NOW()),
('RES-PAST-007', 1,   5, 4, '2026-03-01', '16:30', 'CANCELLED', 'WALKIN', NOW(), NOW()),

-- [이영희 의사 전용 오늘 오전 진료 데이터]
('RES-LEE-004',  204, 1, 1, CURRENT_DATE, '10:30', 'RESERVED',     'ONLINE', NOW(), NOW()),

-- [이영희 의사 전용 대규모 테스트]
-- 과거 데이터 (그저께 완료)
('RES-DUMMY-001', 301, 1, 1, DATE_ADD(CURRENT_DATE, INTERVAL -2 DAY), '09:00', 'COMPLETED', 'PHONE',  NOW(), NOW()),
('RES-DUMMY-002', 302, 1, 1, DATE_ADD(CURRENT_DATE, INTERVAL -2 DAY), '10:00', 'COMPLETED', 'ONLINE', NOW(), NOW()),
-- 과거 데이터 (어제 완료)
('RES-DUMMY-003', 303, 1, 1, DATE_ADD(CURRENT_DATE, INTERVAL -1 DAY), '14:00', 'COMPLETED', 'WALKIN', NOW(), NOW()),
('RES-DUMMY-004', 304, 1, 1, DATE_ADD(CURRENT_DATE, INTERVAL -1 DAY), '15:30', 'COMPLETED', 'PHONE',  NOW(), NOW()),
-- 오늘 데이터 (진료 중 및 대기)
('RES-NOW-001',   305, 1, 1, CURRENT_DATE, '11:00', 'IN_TREATMENT', 'ONLINE', NOW(), NOW()),
('RES-NOW-002',   306, 1, 1, CURRENT_DATE, '11:30', 'RECEIVED',     'WALKIN', NOW(), NOW()),
-- 미래 데이터 (내일 예약)
('RES-FUTURE-001', 307, 1, 1, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '09:30', 'RESERVED', 'ONLINE', NOW(), NOW()),

-- ==============================================================================
-- [추가] 다양한 진료과 예약 데이터
-- ==============================================================================
-- 정형외과 (의사 한지민)
('RES-ORTHO-001', 403, 6, 5, DATE_ADD(CURRENT_DATE, INTERVAL -5 DAY), '10:00', 'COMPLETED', 'ONLINE', NOW(), NOW()),
('RES-ORTHO-002', 409, 6, 5, DATE_ADD(CURRENT_DATE, INTERVAL -3 DAY), '10:30', 'COMPLETED', 'PHONE',  NOW(), NOW()),
('RES-ORTHO-003', 414, 6, 5, CURRENT_DATE,                             '11:00', 'RESERVED',  'ONLINE', NOW(), NOW()),

-- 피부과 (의사 정우성)
('RES-DERM-001', 401, 7, 6, DATE_ADD(CURRENT_DATE, INTERVAL -7 DAY), '09:00', 'COMPLETED', 'WALKIN', NOW(), NOW()),
('RES-DERM-002', 410, 7, 6, DATE_ADD(CURRENT_DATE, INTERVAL -4 DAY), '09:30', 'COMPLETED', 'ONLINE', NOW(), NOW()),
('RES-DERM-003', 415, 7, 6, CURRENT_DATE,                             '10:00', 'RECEIVED',  'PHONE',  NOW(), NOW()),
('RES-DERM-004', 401, 7, 6, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY),  '09:00', 'RESERVED',  'ONLINE', NOW(), NOW()),

-- 안과 (의사 송혜교)
('RES-EYE-001', 402, 8, 7, DATE_ADD(CURRENT_DATE, INTERVAL -6 DAY), '09:00', 'COMPLETED', 'ONLINE', NOW(), NOW()),
('RES-EYE-002', 411, 8, 7, DATE_ADD(CURRENT_DATE, INTERVAL -2 DAY), '10:00', 'COMPLETED', 'PHONE',  NOW(), NOW()),
('RES-EYE-003', 402, 8, 7, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY),  '09:30', 'RESERVED',  'ONLINE', NOW(), NOW()),

-- 산부인과 (의사 공유진)
('RES-OB-001', 405, 9, 8, DATE_ADD(CURRENT_DATE, INTERVAL -10 DAY), '09:00', 'COMPLETED', 'ONLINE', NOW(), NOW()),
('RES-OB-002', 405, 9, 8, DATE_ADD(CURRENT_DATE, INTERVAL -3 DAY),  '09:30', 'COMPLETED', 'ONLINE', NOW(), NOW()),
('RES-OB-003', 405, 9, 8, DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY),   '09:00', 'RESERVED',  'ONLINE', NOW(), NOW()),

-- 이비인후과 (의사 이준혁) — 추가
('RES-ENT-001', 407, 5, 4, DATE_ADD(CURRENT_DATE, INTERVAL -8 DAY), '09:00', 'COMPLETED', 'PHONE',  NOW(), NOW()),
('RES-ENT-002', 408, 5, 4, DATE_ADD(CURRENT_DATE, INTERVAL -5 DAY), '09:30', 'COMPLETED', 'ONLINE', NOW(), NOW()),
('RES-ENT-003', 404, 5, 4, CURRENT_DATE,                             '10:00', 'RESERVED',  'WALKIN', NOW(), NOW()),

-- 소아과 (의사 최지우) — 추가
('RES-PED-001', 412, 4, 3, DATE_ADD(CURRENT_DATE, INTERVAL -4 DAY), '09:00', 'COMPLETED', 'PHONE',  NOW(), NOW()),
('RES-PED-002', 412, 4, 3, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY),  '10:00', 'RESERVED',  'ONLINE', NOW(), NOW()),

-- 내과 추가 (의사 김태희 — 호흡기내과)
('RES-RESP-001', 404, 10, 1, DATE_ADD(CURRENT_DATE, INTERVAL -6 DAY), '09:00', 'COMPLETED', 'ONLINE', NOW(), NOW()),
('RES-RESP-002', 413, 10, 1, DATE_ADD(CURRENT_DATE, INTERVAL -2 DAY), '10:00', 'COMPLETED', 'PHONE',  NOW(), NOW()),
('RES-RESP-003', 406, 10, 1, CURRENT_DATE,                             '09:30', 'RESERVED',  'WALKIN', NOW(), NOW()),

-- 외과 추가 (의사 박서연 — 복강경외과)
('RES-LAP-001', 3,   3, 2, DATE_ADD(CURRENT_DATE, INTERVAL -9 DAY), '09:00', 'COMPLETED', 'ONLINE', NOW(), NOW()),
('RES-LAP-002', 203, 3, 2, DATE_ADD(CURRENT_DATE, INTERVAL -4 DAY), '10:00', 'COMPLETED', 'WALKIN', NOW(), NOW()),

-- [미래 예약 모음] (다음 주 예약들)
('RES-FUT-001', 2,   1,  1, DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), '09:00', 'RESERVED', 'ONLINE', NOW(), NOW()),
('RES-FUT-002', 3,   2,  2, DATE_ADD(CURRENT_DATE, INTERVAL 4 DAY), '10:00', 'RESERVED', 'PHONE',  NOW(), NOW()),
('RES-FUT-003', 401, 7,  6, DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), '09:00', 'RESERVED', 'ONLINE', NOW(), NOW()),
('RES-FUT-004', 403, 6,  5, DATE_ADD(CURRENT_DATE, INTERVAL 5 DAY), '11:00', 'RESERVED', 'PHONE',  NOW(), NOW()),
('RES-FUT-005', 406, 1,  1, DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), '14:00', 'RESERVED', 'ONLINE', NOW(), NOW()),
('RES-FUT-006', 413, 10, 1, DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), '09:30', 'RESERVED', 'PHONE',  NOW(), NOW());

-- 취소 사유 업데이트
UPDATE reservation SET cancellation_reason = '개인 사정' WHERE reservation_number = 'RES-PAST-006';
UPDATE reservation SET cancellation_reason = '단순 변심' WHERE reservation_number = 'RES-PAST-007';
UPDATE reservation SET cancellation_reason = '일정 변경으로 인한 취소' WHERE reservation_number = 'RES-BASE-003';

-- ==============================================================================
-- 7. 진료 기록 (Treatment Record)
-- ==============================================================================

-- 오늘 완료 (이영희 의사)
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '급성 위염', '큐란 1정, 가스모틴 1정', '안정 필요 / 바이탈 정상, 수액 투여 완료', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-DONE-001';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '결막염', '레보플록사신 점안액', '렌즈 금지 / 안압 측정 완료 (정상 범위)', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-DONE-002';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '피부염', '베포리진 1정', '긁지 말 것', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-DONE-003';

-- 과거 완료 (김명준 환자)
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '만성 위염', '알마겔 1포', '식후 복용', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-PAST-001';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 5, '비염', '씨잘정 1정', '환기 자주', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-PAST-002';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 2, '장염', '스멕타 1포', '수분 섭취', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-PAST-003';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '몸살', '타이레놀 ER', '휴식', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-PAST-004';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '식도염', '넥시움정', '금식', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-PAST-005';

-- 이영희 의사 대규모 테스트 진료기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '본태성 고혈압', '암로디핀 5mg 1일 1회', '혈압 안정적임, 지속 관찰 요망 / 혈압 130/85, 맥박 72', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-DUMMY-001';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '긴장성 두통', '타이레놀 ER 650mg', '스트레스 관리 및 충분한 휴식 권고 / 체온 36.5, 혈압 정상', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-DUMMY-002';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '발목 외측 인대 염좌', '소염진통제 및 물리치료', '반깁스 1주일 유지 권장 / 부종 확인, 냉찜질 처치', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-DUMMY-003';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 1, '알레르기성 비염', '항히스타민제 제제', '외출 시 마스크 착용 당부', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-DUMMY-004';

-- 정형외과 진료기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 6, '회전근개 부분 파열', '소염진통제 처방, 물리치료 3주', '무거운 물건 들기 금지 / ROM 검사 완료', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-ORTHO-001';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 6, '전방십자인대 부분 손상', '보조기 착용 및 재활운동', 'MRI 추가 촬영 예정 / 부종 및 압통 확인', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-ORTHO-002';

-- 피부과 진료기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 7, '접촉성 피부염', '스테로이드 연고 1일 2회', '알레르기 유발 물질 접촉 주의', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-DERM-001';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 7, '아토피 피부염 (중등도)', '프로토픽 연고, 항히스타민 복용', '보습제 하루 3회 이상 도포 / 피부 상태 사진 촬영', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-DERM-002';

-- 안과 진료기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 8, '근시 진행 (양안)', '안경 도수 조정 처방', '6개월 후 재검 권고 / 시력 검사: 좌 0.4 / 우 0.5', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-EYE-001';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 8, '안검하수 (경미)', '경과 관찰', '수술 적응증 미달, 6개월 후 재검 / 안검거근 기능 측정 완료', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-EYE-002';

-- 산부인과 진료기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 9, '정상 임신 8주', '엽산 400mcg 1일 1회', '입덧 심할 시 소량씩 자주 섭취 / 초음파 확인: 심박 확인', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-OB-001';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 9, '정상 임신 12주', '철분제 추가 처방', '안정기 진입, 가벼운 운동 권장 / 초음파 정상, NT 검사 정상', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-OB-002';

-- 이비인후과 진료기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 5, '급성 중이염', '항생제 5일분, 진통제', '수영 금지, 귀에 물 들어가지 않게 주의', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-ENT-001';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 5, '편도선 비대 (Grade 2)', '소염제 처방, 경과 관찰', '재발 시 수술 고려 / 인후 사진 촬영', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-ENT-002';

-- 소아과 진료기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 4, '수두 예방접종 후 정상', '해열제 (필요시)', '접종 부위 발적 시 냉찜질 / 체온 36.8, 접종 부위 이상 없음', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-PED-001';

-- 호흡기내과 진료기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 10, '급성 상기도 감염', '세파클러 3일분, 슈다페드', '충분한 수분 섭취와 휴식 / 체온 37.8, 인후 발적', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-RESP-001';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 10, '만성 위염 (H.pylori 양성)', '제균 3제 요법 14일', '2주 후 재검, 음주 금지 / 위내시경 결과 첨부', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-RESP-002';

-- 복강경외과 진료기록
INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 3, '서혜부 탈장', '수술 전 검사 결과 이상 없음', '복강경 수술 일정 확정 / 혈액 검사 정상, 심전도 정상', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-LAP-001';

INSERT INTO treatment_record (reservation_id, doctor_id, diagnosis, prescription, remark, created_at)
SELECT r.id, 3, '요추 디스크 (경미)', '소염진통제 및 물리치료', '자세 교정 필요, 무거운 물건 금지', NOW()
FROM reservation r WHERE r.reservation_number = 'RES-LAP-002';

-- ==============================================================================
-- 8. 물품 (Item)
-- ==============================================================================
INSERT INTO item (name, category, quantity, min_quantity, created_at, updated_at) VALUES
-- 의료 소모품
('주사기 (5ml)',            'MEDICAL_SUPPLIES',  8,   50,  NOW(), NOW()),
('알코올솜',                'MEDICAL_SUPPLIES',  20,  100, NOW(), NOW()),
('일회용 장갑 (M)',         'MEDICAL_SUPPLIES',  150, 100, NOW(), NOW()),
('거즈 패드',               'MEDICAL_SUPPLIES',  200, 80,  NOW(), NOW()),
('마스크 (KF94)',           'MEDICAL_SUPPLIES',  3,   50,  NOW(), NOW()),
('수액 세트 (일반)',        'MEDICAL_SUPPLIES',  45,  30,  NOW(), NOW()),
('일회용 주사기 (3ml)',     'MEDICAL_SUPPLIES',  120, 50,  NOW(), NOW()),
('소독용 에탄올',           'MEDICAL_SUPPLIES',  25,  20,  NOW(), NOW()),
('반창고',                  'MEDICAL_SUPPLIES',  300, 100, NOW(), NOW()),
('면봉 (멸균)',             'MEDICAL_SUPPLIES',  500, 200, NOW(), NOW()),
('일회용 장갑 (L)',         'MEDICAL_SUPPLIES',  80,  100, NOW(), NOW()),
('일회용 장갑 (S)',         'MEDICAL_SUPPLIES',  90,  100, NOW(), NOW()),
('봉합사 (나일론 4-0)',     'MEDICAL_SUPPLIES',  15,  10,  NOW(), NOW()),
('드레싱 세트',             'MEDICAL_SUPPLIES',  30,  20,  NOW(), NOW()),
('카테터 (16G)',            'MEDICAL_SUPPLIES',  20,  15,  NOW(), NOW()),
-- 의료 장비
('혈압계',                  'MEDICAL_EQUIPMENT', 2,   5,   NOW(), NOW()),
('체온계',                  'MEDICAL_EQUIPMENT', 10,  5,   NOW(), NOW()),
('청진기',                  'MEDICAL_EQUIPMENT', 8,   3,   NOW(), NOW()),
('산소포화도 측정기',       'MEDICAL_EQUIPMENT', 5,   3,   NOW(), NOW()),
('심전도 모니터',           'MEDICAL_EQUIPMENT', 2,   2,   NOW(), NOW()),
('수액 펌프',               'MEDICAL_EQUIPMENT', 6,   4,   NOW(), NOW()),
('혈당 측정기',             'MEDICAL_EQUIPMENT', 12,  5,   NOW(), NOW()),
('네블라이저',              'MEDICAL_EQUIPMENT', 4,   3,   NOW(), NOW()),
('이경 (이비인후과용)',     'MEDICAL_EQUIPMENT', 3,   2,   NOW(), NOW()),
('안압계',                  'MEDICAL_EQUIPMENT', 2,   1,   NOW(), NOW()),
-- 일반 비품
('A4 용지 (박스)',          'GENERAL_SUPPLIES',  5,   2,   NOW(), NOW()),
('볼펜',                    'GENERAL_SUPPLIES',  30,  10,  NOW(), NOW()),
('프린터 토너',             'GENERAL_SUPPLIES',  3,   2,   NOW(), NOW()),
('처방전 용지',             'GENERAL_SUPPLIES',  500, 100, NOW(), NOW()),
('진료 기록 카드',          'GENERAL_SUPPLIES',  200, 50,  NOW(), NOW()),
('라벨 용지',               'GENERAL_SUPPLIES',  100, 30,  NOW(), NOW()),
('손 소독제 (500ml)',       'GENERAL_SUPPLIES',  15,  10,  NOW(), NOW()),
('휴지 (박스)',             'GENERAL_SUPPLIES',  8,   5,   NOW(), NOW());

-- ==============================================================================
-- 9. 물품 재고 로그 (Item Stock Log)
-- ==============================================================================
INSERT INTO item_stock_log (item_id, item_name, type, amount, performed_by, created_at) VALUES
-- 주사기 (5ml) - id 추정: 조회 후 매핑 필요하므로 하드코딩
(1,  '주사기 (5ml)',       'IN',  100, '물품담당최지훈', DATE_ADD(NOW(), INTERVAL -30 DAY)),
(1,  '주사기 (5ml)',       'OUT', 50,  '간호사박민수',   DATE_ADD(NOW(), INTERVAL -20 DAY)),
(1,  '주사기 (5ml)',       'OUT', 42,  '간호사김소연',   DATE_ADD(NOW(), INTERVAL -5 DAY)),
-- 알코올솜
(2,  '알코올솜',           'IN',  200, '물품담당최지훈', DATE_ADD(NOW(), INTERVAL -25 DAY)),
(2,  '알코올솜',           'OUT', 100, '간호사이하늘',   DATE_ADD(NOW(), INTERVAL -15 DAY)),
(2,  '알코올솜',           'OUT', 80,  '간호사박민수',   DATE_ADD(NOW(), INTERVAL -3 DAY)),
-- 일회용 장갑 (M)
(3,  '일회용 장갑 (M)',    'IN',  300, '물품담당양서윤', DATE_ADD(NOW(), INTERVAL -20 DAY)),
(3,  '일회용 장갑 (M)',    'OUT', 150, '간호사최예진',   DATE_ADD(NOW(), INTERVAL -7 DAY)),
-- 거즈 패드
(4,  '거즈 패드',          'IN',  500, '물품담당최지훈', DATE_ADD(NOW(), INTERVAL -15 DAY)),
(4,  '거즈 패드',          'OUT', 300, '간호사정다은',   DATE_ADD(NOW(), INTERVAL -2 DAY)),
-- 마스크 (KF94)
(5,  '마스크 (KF94)',      'IN',  200, '물품담당양서윤', DATE_ADD(NOW(), INTERVAL -28 DAY)),
(5,  '마스크 (KF94)',      'OUT', 197, '접수김철수',     DATE_ADD(NOW(), INTERVAL -1 DAY)),
-- 수액 세트
(6,  '수액 세트 (일반)',   'IN',  80,  '물품담당최지훈', DATE_ADD(NOW(), INTERVAL -10 DAY)),
(6,  '수액 세트 (일반)',   'OUT', 35,  '간호사박민수',   DATE_ADD(NOW(), INTERVAL -2 DAY)),
-- 혈압계
(16, '혈압계',             'IN',  5,   '물품담당최지훈', DATE_ADD(NOW(), INTERVAL -60 DAY)),
(16, '혈압계',             'OUT', 3,   '간호사박민수',   DATE_ADD(NOW(), INTERVAL -30 DAY)),
-- 체온계
(17, '체온계',             'IN',  15,  '물품담당양서윤', DATE_ADD(NOW(), INTERVAL -45 DAY)),
(17, '체온계',             'OUT', 5,   '간호사김소연',   DATE_ADD(NOW(), INTERVAL -20 DAY));

-- ==============================================================================
-- 10. 물품 사용 로그 (Item Usage Log)
-- ==============================================================================
INSERT INTO item_usage_log (reservation_id, item_id, item_name, amount, used_by, used_at) VALUES
-- RES-DONE-001 진료 시 사용
((SELECT id FROM reservation WHERE reservation_number = 'RES-DONE-001'), 1,  '주사기 (5ml)',       2,  '간호사박민수', NOW()),
((SELECT id FROM reservation WHERE reservation_number = 'RES-DONE-001'), 2,  '알코올솜',           3,  '간호사박민수', NOW()),
((SELECT id FROM reservation WHERE reservation_number = 'RES-DONE-001'), 3,  '일회용 장갑 (M)',    2,  '간호사박민수', NOW()),
((SELECT id FROM reservation WHERE reservation_number = 'RES-DONE-001'), 6,  '수액 세트 (일반)',   1,  '간호사박민수', NOW()),
-- RES-DONE-002 진료 시 사용
((SELECT id FROM reservation WHERE reservation_number = 'RES-DONE-002'), 3,  '일회용 장갑 (M)',    2,  '간호사박민수', NOW()),
((SELECT id FROM reservation WHERE reservation_number = 'RES-DONE-002'), 10, '면봉 (멸균)',        4,  '간호사박민수', NOW()),
-- RES-DONE-003 진료 시 사용
((SELECT id FROM reservation WHERE reservation_number = 'RES-DONE-003'), 3,  '일회용 장갑 (M)',    2,  '간호사박민수', NOW()),
((SELECT id FROM reservation WHERE reservation_number = 'RES-DONE-003'), 14, '드레싱 세트',        1,  '간호사박민수', NOW()),
-- 정형외과 진료 시 사용
((SELECT id FROM reservation WHERE reservation_number = 'RES-ORTHO-001'), 3,  '일회용 장갑 (M)',   2,  '간호사최예진', DATE_ADD(NOW(), INTERVAL -5 DAY)),
((SELECT id FROM reservation WHERE reservation_number = 'RES-ORTHO-001'), 4,  '거즈 패드',         3,  '간호사최예진', DATE_ADD(NOW(), INTERVAL -5 DAY)),
-- 피부과 진료 시 사용
((SELECT id FROM reservation WHERE reservation_number = 'RES-DERM-001'), 3,  '일회용 장갑 (M)',    2,  '간호사김소연', DATE_ADD(NOW(), INTERVAL -7 DAY)),
((SELECT id FROM reservation WHERE reservation_number = 'RES-DERM-002'), 3,  '일회용 장갑 (M)',    2,  '간호사김소연', DATE_ADD(NOW(), INTERVAL -4 DAY));

-- ==============================================================================
-- 11. 병원 규칙 (Hospital Rule) — 카테고리별 60건, 총 300건
-- ==============================================================================

-- 재귀 CTE로 시퀀스 생성 (MySQL 8.0+)
INSERT INTO hospital_rule (title, content, category, is_active, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS x
    UNION ALL
    SELECT x + 1 FROM seq WHERE x < 60
)
SELECT
    CONCAT('응급 대응 프로토콜 ', x),
    CONCAT('응급 호출 접수 후 1분 이내 담당 의사와 간호사에게 전파하고, 환자 상태 분류, 응급 카트 준비, 처치 기록을 순서대로 수행한다. 점검 코드 ER-', x),
    'EMERGENCY',
    CASE WHEN MOD(x, 10) = 0 THEN FALSE ELSE TRUE END,
    DATE_ADD(NOW(), INTERVAL -x MINUTE),
    DATE_ADD(NOW(), INTERVAL -x MINUTE)
FROM seq;

INSERT INTO hospital_rule (title, content, category, is_active, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS x
    UNION ALL
    SELECT x + 1 FROM seq WHERE x < 60
)
SELECT
    CONCAT('물품 재고 점검 규칙 ', x),
    CONCAT('일일 물품 라운드 시 재고 수량, 유효기간, 보관 위치, 멸균 상태를 함께 확인하고 부족 물품은 교대 종료 전까지 발주 요청한다. 점검 코드 SP-', x),
    'SUPPLY',
    CASE WHEN MOD(x, 8) = 0 THEN FALSE ELSE TRUE END,
    DATE_ADD(NOW(), INTERVAL -(60 + x) MINUTE),
    DATE_ADD(NOW(), INTERVAL -(60 + x) MINUTE)
FROM seq;

INSERT INTO hospital_rule (title, content, category, is_active, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS x
    UNION ALL
    SELECT x + 1 FROM seq WHERE x < 60
)
SELECT
    CONCAT('근무 인수인계 기준 ', x),
    CONCAT('교대 시작 전 환자 상태, 우선 처치 대상, 미완료 업무, 특이사항을 인수인계 체크리스트에 기록하고 담당자 서명을 남긴다. 점검 코드 DU-', x),
    'DUTY',
    CASE WHEN MOD(x, 12) = 0 THEN FALSE ELSE TRUE END,
    DATE_ADD(NOW(), INTERVAL -(120 + x) MINUTE),
    DATE_ADD(NOW(), INTERVAL -(120 + x) MINUTE)
FROM seq;

INSERT INTO hospital_rule (title, content, category, is_active, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS x
    UNION ALL
    SELECT x + 1 FROM seq WHERE x < 60
)
SELECT
    CONCAT('위생 관리 체크 규칙 ', x),
    CONCAT('진료 전후 손 위생을 수행하고 표면 소독, 폐기물 분리배출, 보호구 교체 여부를 체크리스트에 남긴다. 점검 코드 HY-', x),
    'HYGIENE',
    CASE WHEN MOD(x, 15) = 0 THEN FALSE ELSE TRUE END,
    DATE_ADD(NOW(), INTERVAL -(180 + x) MINUTE),
    DATE_ADD(NOW(), INTERVAL -(180 + x) MINUTE)
FROM seq;

INSERT INTO hospital_rule (title, content, category, is_active, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS x
    UNION ALL
    SELECT x + 1 FROM seq WHERE x < 60
)
SELECT
    CONCAT('병원 운영 공통 안내 ', x),
    CONCAT('내부 공지 확인, 문서 보관, 환자 응대, 보안 점검, 시설 이상 보고 절차를 숙지하고 근무 중 변경 사항은 즉시 공유한다. 점검 코드 OT-', x),
    'OTHER',
    CASE WHEN MOD(x, 20) = 0 THEN FALSE ELSE TRUE END,
    DATE_ADD(NOW(), INTERVAL -(240 + x) MINUTE),
    DATE_ADD(NOW(), INTERVAL -(240 + x) MINUTE)
FROM seq;

-- ==============================================================================
-- 12. LLM 추천 기록 (LLM Recommendation) — 추가 데이터
-- ==============================================================================
INSERT INTO llm_recommendation (symptom_text, recommended_dept, recommended_doctor, recommended_time, llm_response_raw, is_used, created_at) VALUES
('배가 아프고 속이 메스꺼워요',         '내과',       '의사이영희',   '14:00', '{"department":"내과","doctor":"의사이영희","reason":"소화기 증상으로 내과 진료 권장"}', TRUE,  DATE_ADD(NOW(), INTERVAL -2 DAY)),
('기침이 2주째 계속돼요',               '내과',       '의사김태희',   '09:30', '{"department":"내과","doctor":"의사김태희","reason":"만성 기침은 호흡기내과 전문의 진료 권장"}', TRUE,  DATE_ADD(NOW(), INTERVAL -1 DAY)),
('아이가 열이 나고 보채요',             '소아과',     '의사최지우',   '10:00', '{"department":"소아과","doctor":"의사최지우","reason":"소아 발열 증상"}', TRUE,  NOW()),
('귀에서 소리가 나고 아파요',           '이비인후과', '의사이준혁',   '09:00', '{"department":"이비인후과","doctor":"의사이준혁","reason":"이명 및 이통 증상"}', FALSE, NOW()),
('눈이 자주 충혈되고 뻑뻑해요',         '안과',       '의사송혜교',   '10:00', '{"department":"안과","doctor":"의사송혜교","reason":"안구건조증 의심"}', FALSE, NOW()),
('무릎이 걸을 때마다 아파요',           '정형외과',   '의사한지민',   '10:30', '{"department":"정형외과","doctor":"의사한지민","reason":"관절 통증 전문의 진료 권장"}', TRUE,  DATE_ADD(NOW(), INTERVAL -3 DAY)),
('피부에 붉은 반점이 생겼어요',         '피부과',     '의사정우성',   '09:00', '{"department":"피부과","doctor":"의사정우성","reason":"피부 발진 증상"}', TRUE,  DATE_ADD(NOW(), INTERVAL -7 DAY)),
('임신 가능성이 있는데 확인하고 싶어요', '산부인과',   '의사공유진',   '09:00', '{"department":"산부인과","doctor":"의사공유진","reason":"임신 확인 검사"}', TRUE,  DATE_ADD(NOW(), INTERVAL -10 DAY));

-- ==============================================================================
-- FK 제약 복원
-- ==============================================================================
SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================================
-- 테스트 로그인 정보 요약
-- ==============================================================================
-- | 역할          | 아이디     | 비밀번호      | 비고                    |
-- |---------------|-----------|--------------|------------------------|
-- | 관리자         | admin01   | password123  | 전체 시스템 관리         |
-- | 관리자         | admin02   | password123  | 전체 시스템 관리 (보조)   |
-- | 접수 직원      | staff01   | password123  | 내과 소속               |
-- | 접수 직원      | staff02   | password123  | 외과 소속               |
-- | 접수 직원      | staff03   | password123  | 소아과 소속             |
-- | 의사 (내과)    | doctor01  | password123  | 소화기내과 전문          |
-- | 의사 (외과)    | doctor02  | password123  | 외상외과 전문            |
-- | 의사 (외과)    | doctor03  | password123  | 복강경외과 전문          |
-- | 의사 (소아과)  | doctor04  | password123  | 소아일반                 |
-- | 의사 (이비인후)| doctor05  | password123  | 이비인후일반             |
-- | 의사 (정형외과)| doctor06  | password123  | 관절외과 전문            |
-- | 의사 (피부과)  | doctor07  | password123  | 일반피부과               |
-- | 의사 (안과)    | doctor08  | password123  | 안과일반                 |
-- | 의사 (산부인과)| doctor09  | password123  | 산부인과일반             |
-- | 의사 (내과)    | doctor10  | password123  | 호흡기내과 전문          |
-- | 간호사         | nurse01   | password123  | 내과 소속               |
-- | 간호사         | nurse02   | password123  | 외과 소속               |
-- | 간호사         | nurse03   | password123  | 소아과 소속             |
-- | 간호사         | nurse04   | password123  | 이비인후과 소속          |
-- | 간호사         | nurse05   | password123  | 정형외과 소속            |
-- | 물품 관리자    | item01    | password123  | 물품 총괄                |
-- | 물품 관리자    | item02    | password123  | 물품 관리 (보조)         |
-- ==============================================================================
