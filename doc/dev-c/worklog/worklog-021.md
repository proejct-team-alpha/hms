# task-021 작업 로그

## 작업 전 준수 항목 체크리스트

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-021.md` 범위 확인
- [x] `doc/dev-c/workflow/workflow-021.md` 방향 확인
- [x] `doc/dev-c/.person/reports/task-021/` 보고서 확인

## 작업 목표

- 별도 접수 전용 화면을 만들지 않고 기존 `/admin/reservation/list` 안에서 `접수` 상태 필터를 자연스럽게 제공한다.
- 내부 상태값 `RECEIVED`는 유지하면서 사용자 노출 라벨을 `접수`로 통일한다.
- 예약 취소 후에도 `page`, `size`, `status`가 안전하게 유지되도록 정리한다.
- `admin.reservation` 범위와 전체 테스트를 통해 예약/접수 통합 흐름 회귀를 방어한다.

## 보고서 소스

- `report-20260319-1305-task-21-2.md`
- `report-20260319-1404-task-21-3.md`
- `report-20260319-1412-task-21-4.md`
- `report-20260319-1415-task-21-5.md`
- `report-20260319-1419-task-21-6.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationController.java`
- `src/main/resources/templates/admin/reservation-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationServiceTest.java`
- `doc/dev-c/task/task-021.md`
- `doc/dev-c/workflow/workflow-021.md`

## 구현 내용

### 1. 접수 라벨 계약 고정

- 서비스는 기존 `STATUS_LABELS` 매핑을 유지하면서 `RECEIVED -> 접수` 계약을 테스트로 고정했다.
- `AdminReservationServiceTest`에 상태 옵션 라벨과 목록 row의 `statusLabel`이 모두 `접수`인지 확인하는 테스트를 추가했다.
- 내부 상태값은 바꾸지 않고 UI 표현만 `접수`로 맞추는 방향을 명확히 닫았다.

### 2. 예약 목록 화면 문구와 필터 UI 정리

- `reservation-list.mustache` 설명 문구를 `예약과 접수 현황을 상태별로 확인하고 관리합니다.` 기준으로 정리했다.
- 상태 버튼 묶음 앞에 `상태 필터` 라벨을 추가해 필터 의미가 더 명확하게 보이도록 정리했다.
- 빈 목록 메시지를 `조회된 내역이 없습니다.`로 바꿔 예약/접수 통합 흐름에서도 어색하지 않게 맞췄다.

### 3. 취소 후 복귀 흐름 안정화

- `AdminReservationController`에 `normalizeStatus()`를 추가해 허용된 상태값만 유지하고 그 외 값은 `ALL`로 보정하도록 정리했다.
- `status=received` 같은 입력은 `RECEIVED`로 정규화하고, `status=INVALID` 같은 값은 `ALL`로 안전하게 복귀시킨다.
- 예약 취소 후에도 `page`, `size`, `status`가 그대로 유지되도록 컨트롤러 테스트를 보강했다.

### 4. 화면 렌더링과 범위 테스트 마감

- 컨트롤러 테스트에 `RECEIVED` 상태 row가 실제 HTML에서 `접수` 배지로 렌더링되는지 확인하는 검증을 추가했다.
- 서비스 라벨, 필터 렌더링, 취소 후 복귀, 템플릿 문구를 묶어 `admin.reservation` 범위 테스트를 다시 통과시켰다.
- 마지막으로 `workflow-021.md`, `task-021.md`를 완료 상태로 갱신하고, 후속 분리 기준과 PR 리뷰 포인트를 문서에 정리했다.

## 검증 결과

- `./gradlew test --tests 'com.smartclinic.hms.admin.reservation.AdminReservationServiceTest'` : `BUILD SUCCESSFUL`
- `./gradlew test --tests 'com.smartclinic.hms.admin.reservation.AdminReservationControllerTest'` : `BUILD SUCCESSFUL`
- `./gradlew test --tests 'com.smartclinic.hms.admin.reservation.*'` : `BUILD SUCCESSFUL`
- `./gradlew test` : `BUILD SUCCESSFUL`

## 참고 문서

- 로컬
- `AGENTS.md`
- `.ai/memory.md`
- `doc/PROJECT_STRUCTURE.md`
- `doc/RULE.md`
- `doc/dev-c/task/task-021.md`
- `doc/dev-c/workflow/workflow-021.md`

## 남은 TODO / 리스크

- 현재 범위 기준 known issue는 없다.
- 별도 `/admin/reception/list` 분리는 이번 단계에서 하지 않는다.
- 이후 접수 전용 통계, 실시간 모니터링, 대기 순번 강조, 보드형 레이아웃 요구가 생기면 별도 화면 분리를 다시 검토한다.
