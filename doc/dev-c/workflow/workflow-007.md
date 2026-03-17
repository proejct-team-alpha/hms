# Admin 예약 취소 액션 구현 명세서 (workflow-007)

## 문제 정의
관리자 예약 목록 페이지에서 상태 운영을 위해 `액션` 컬럼을 추가하고, `취소` 버튼으로 예약 상태를 `CANCELLED`로 변경할 수 있어야 한다.
취소는 확인창을 거쳐 실행되며, SSR + PRG 규칙을 따른다.

## 대상 사용자
- 관리자(ROLE_ADMIN): 예약 취소 처리 운영

## 핵심 요구사항 (우선순위순)
1. [P0] 예약 목록 테이블에 `액션` 컬럼 추가
2. [P0] 각 행에 `취소` 버튼 제공
3. [P0] 취소 버튼 클릭 시 확인창 표시
4. [P0] 확인창 `예` 선택 시 취소 처리 실행
5. [P0] 확인창 `아니오` 선택 시 현재 페이지 유지
6. [P0] 취소 허용 상태: `RESERVED`, `RECEIVED`
7. [P0] 취소 불가 상태: `COMPLETED`, `CANCELLED`
8. [P0] 처리 후 `redirect:/admin/reservation/list` + flash 메시지
   - `successMessage = "예약이 취소되었습니다."`
9. [P1] 취소 후 목록 복귀 시 기존 `status/page/size` 유지

## 제약 조건 & 전제
- URL prefix `/admin/**` 유지
- SSR 규칙 준수: 상태 변경은 `POST` + PRG
- 컨트롤러는 Service 위임, 상태 전이 검증은 도메인/서비스에서 처리
- 기존 레이아웃/템플릿 구조 유지

## 엣지 케이스 & 에러 시나리오
- `COMPLETED` 취소 시도 시 거부 처리
- 이미 `CANCELLED`인 예약 재취소 시도 시 거부 처리
- 잘못된 `reservationId` 전달 시 not found 처리
- 취소 처리 중 예외 발생 시 목록으로 복귀 + 실패 메시지 표시

## 범위 밖 (명시적 제외)
- 다시 접수(상태 복구) 기능
- 모달 UI 도입/대규모 디자인 개편
- JSON API 전환

## 수용 기준
- [ ] 목록에 `액션` 컬럼과 `취소` 버튼이 보인다.
- [ ] `RESERVED`, `RECEIVED`에서만 취소 버튼이 활성 동작한다.
- [ ] 확인창 `예` 선택 시 상태가 `CANCELLED`로 변경된다.
- [ ] 확인창 `아니오` 선택 시 상태 변경 없이 현재 페이지에 남는다.
- [ ] 성공 시 flash `예약이 취소되었습니다.`가 표시된다.
- [ ] 취소 후 `status/page/size`가 유지된다.

## 구현 순서
1. `reservation-list.mustache`에 `액션` 컬럼 및 취소 버튼 배치
2. 버튼 클릭 시 확인창(`confirm`) 동작 연결
3. `POST /admin/reservation/cancel` 컨트롤러 엔드포인트 추가
4. Service에 취소 메서드 추가 및 상태 전이 검증 적용
5. 성공 시 `successMessage` flash 설정 + PRG redirect
6. redirect URL에 `status/page/size` 포함해 상태 유지
7. 테스트(Controller/Service)로 정상/예외 케이스 검증
