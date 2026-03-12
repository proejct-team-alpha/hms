# W3-4 추천 → 예약 폼 연결 구조

## 작업 목표
추천 결과의 time 파라미터를 direct-reservation으로 전달하고,
direct-reservation에서 시간 select를 자동 선택하도록 연결한다.

## 작업 목록
<!-- DONE 1. symptom-reservation: proceedToDirect()에 time 파라미터 추가 -->
<!-- DONE 2. direct-reservation: DEPT_ID_MAP으로 진료과 이름→ID 변환 후 자동 선택 -->
<!-- DONE 3. direct-reservation: 의사 fetch 후 이름 매칭으로 doctor select 자동 선택 -->
<!-- DONE 4. direct-reservation: time select 자동 선택 -->
<!-- DONE 5. symptom-reservation: SYMPTOM_MAP 의사명을 실제 DB 데이터로 수정 -->

## 수정 파일
- `src/main/resources/templates/reservation/symptom-reservation.mustache`
- `src/main/resources/templates/reservation/direct-reservation.mustache`
