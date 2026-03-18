# 진료과 상세/수정/상태 관리 구현 명세서 (workflow-018)

## 문제 정의
관리자 진료과 관리에서 등록, 상세/수정, 활성/비활성 기능을 구현한다. 등록은 목록 화면 인라인 폼에서 처리하고, 상세와 수정은 하나의 통합 페이지에서 처리한다.

## 대상 사용자
- 관리자(`ROLE_ADMIN`)

## 확정 URL
1. 목록 화면: `GET /admin/department/list`
2. 등록 처리: `POST /admin/department/create`
3. 상세/수정 통합 화면: `GET /admin/department/detail?departmentId={id}`
4. 수정 처리: `POST /admin/department/update`
5. 비활성화 처리: `POST /admin/department/deactivate`
6. 활성화 처리: `POST /admin/department/activate`

## 전달 요구사항
1. 별도 등록 화면은 만들지 않는다.
2. 등록은 목록 화면(`/admin/department/list`)에서 인라인 폼으로 처리한다.
3. `[+ 진료과 등록]` 클릭 시 목록 상단에 입력 폼이 열린다.
4. 등록 시 `POST /admin/department/create`를 호출한다.
5. 상세와 수정은 `GET /admin/department/detail?departmentId={id}` 한 페이지에서 통합 처리한다.
6. 상세/수정 통합 페이지에는 아래 정보와 액션이 포함된다.
   - 진료과 기본 정보 표시
   - 진료과명 수정 폼
   - 현재 상태 배지
   - 상태 액션 버튼
7. 수정 가능 항목은 `진료과명`만 허용한다.
8. 상태값은 수정 폼에서 직접 입력받지 않는다.
9. 상태 변경은 별도 액션으로만 처리한다.
   - 운영 중이면 `비활성화`
   - 비운영이면 `활성화`
10. 수정 성공 후 상세 페이지로 리다이렉트한다.
11. 비활성화/활성화 성공 후에도 상세 페이지로 리다이렉트한다.

## 제약 조건
- 기존 목록 URL과 페이지네이션 구조를 유지한다.
- `GET /admin/department/form` 같은 별도 등록 화면은 만들지 않는다.
- 관리자 영역 URL prefix(`/admin/**`)를 유지한다.
- SSR 방식과 `pageTitle + model` 패턴을 유지한다.
- 활성/비활성은 상태 전이 액션으로만 다룬다.
- 수정 화면에서 상태를 직접 select/input으로 수정하지 않는다.

## 엣지 케이스
- 존재하지 않는 `departmentId` 요청
- 빈 이름 또는 중복 이름 수정 요청
- 이미 비활성인 진료과 재비활성화 요청
- 이미 활성인 진료과 재활성화 요청
- 비활성화 후 상세 페이지 복귀 시 상태 배지가 즉시 반영되는지 확인

## 수용 기준
- [ ] 목록 화면에서 인라인 폼으로 진료과 등록이 가능하다.
- [ ] `GET /admin/department/detail?departmentId={id}`에서 상세/수정 통합 화면이 렌더링된다.
- [ ] 진료과명 수정이 가능하다.
- [ ] 상태에 따라 활성/비활성 버튼이 다르게 표시된다.
- [ ] 비활성화/활성화 처리가 가능하다.
- [ ] 모든 액션 성공 후 상세 페이지로 리다이렉트된다.
- [ ] 관련 SSR 컨트롤러/서비스 테스트가 통과한다.

## 구현 순서
1. 현재 `admin.department` 목록/등록 구조와 엔티티 필드 점검
2. 상세/수정 통합 화면 렌더링용 응답 구조 정의
3. `GET /admin/department/detail` 컨트롤러/서비스 구현
4. `POST /admin/department/update` 이름 수정 구현
5. `POST /admin/department/deactivate`, `POST /admin/department/activate` 상태 전이 구현
6. 목록 화면 인라인 등록 폼 액션을 `POST /admin/department/create` 기준으로 정리
7. 컨트롤러/서비스 테스트 보강
