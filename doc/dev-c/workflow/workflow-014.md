# 직원 비활성화 SSR 구현 명세서 (workflow-014)

## 문제 정의
관리자 직원 비활성화 기능을 구현한다. `POST /admin/staff/deactivate` 요청으로 직원을 실제 삭제하지 않고 `Staff.active = false`로 변경한다.

## 대상 사용자
- 관리자(`ROLE_ADMIN`)

## 전달 요구사항
1. `POST /admin/staff/deactivate` 비활성화 처리
2. 실제 삭제 없이 `Staff.active = false`로 변경
3. 성공 시 직원 목록으로 리다이렉트
4. 성공 메시지 표시(Flash Attribute)
5. 직원 목록 화면에서 비활성화 버튼 노출 규칙
   - 활성 직원에게만 표시
   - 본인 행에는 버튼 숨김
   - 이미 비활성화된 직원은 버튼 대신 비활성 상태 표시

## 공통 제약 조건
- 관리자 본인 계정은 비활성화할 수 없다
  - 메시지: `본인 계정은 비활성화할 수 없습니다.`
- 이미 비활성화된 직원은 다시 비활성화할 수 없다
  - 메시지: `이미 비활성화된 직원입니다.`
- 존재하지 않는 `staffId`는 차단한다
  - 메시지: `직원을 찾을 수 없습니다.`

## 역할별 정책
- 이번 범위에서는 역할별 추가 제약 없이 공통 처리
- `DOCTOR`, `NURSE`, `STAFF`, `ADMIN` 모두 동일하게 `active=false` 적용
- 단, 위 공통 제약은 동일하게 적용

## 엣지 케이스 & 에러 시나리오
- 없는 `staffId` 요청
- 본인 계정 비활성화 요청
- 이미 비활성화된 직원 재요청
- 비활성화 후 목록 재조회 시 상태 반영 확인

## 수용 기준
- [ ] `POST /admin/staff/deactivate` 요청 시 대상 직원이 `active=false`로 변경된다
- [ ] 성공 시 직원 목록으로 리다이렉트되고 성공 메시지가 표시된다
- [ ] 본인 계정 비활성화는 차단된다
- [ ] 이미 비활성화된 직원 재비활성화는 차단된다
- [ ] 존재하지 않는 직원은 예외 처리된다
- [ ] 직원 목록 화면에서 비활성화 버튼 노출 규칙이 반영된다

## 구현 순서
1. `Staff` 엔티티의 활성 상태 변경 방식과 `AdminStaffRepository` 조회 메서드 확인
2. 비활성화 요청 파라미터와 컨트롤러 PRG 흐름 설계
3. `AdminStaffService`에 직원 비활성화 로직 구현
4. `AdminStaffController`에 `POST /admin/staff/deactivate` 구현
5. `staff-list.mustache`에 비활성화 버튼/상태 표시 규칙 반영
6. Controller/Service 테스트 작성 및 검증
