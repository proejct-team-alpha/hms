## 추가 작업 주제
관리자 진료과 목록(25) 기능의 현재 구현 상태를 점검하고, 명세 기준에 맞게 보완한다.

## 현재 상태 점검 결과
1. `AdminDepartmentController`
- `GET /admin/department/list`는 존재한다.
- 하지만 `page`, `size` 등 페이징 파라미터를 받지 않는다.
- 현재는 전체 목록 조회만 수행한다.

2. `AdminDepartmentService`
- `getDepartmentList()`가 전체 조회(`findAll`) 기반이다.
- 페이징 처리 없음
- `totalCount`, `currentPage`, `totalPages`, `pageLinks` 계산 없음

3. Repository 구조
- `admin.department.AdminDepartmentRepository`가 이미 존재한다.
- 그런데 실제 서비스는 `reservation.reservation.DepartmentRepository`를 사용 중이다.
- 모듈 책임 기준이 어긋나 있어 정리가 필요하다.

4. `department-list.mustache`
- 진료과 목록 화면은 존재한다.
- 하지만 페이징 UI가 없다.
- 한글 깨짐과 일부 마크업 손상이 있다.
- `pageTitle` + `model` 패턴 기준 정리도 필요하다.

5. 테스트
- `AdminDepartmentControllerTest` 없음
- `AdminDepartmentServiceTest` 없음
- 현재 구현을 보장하는 테스트가 없다.

## 판단
현재 구현은 진료과 목록 화면 초안 수준이며,  
명세의 핵심인 **진료과 목록 페이징 기능은 아직 미완성**이다.

## 보완 구현 범위
1. `AdminDepartmentController`
- `page`, `size` 요청 파라미터 수신
- 목록 화면에 `model`, `pageTitle` 전달

2. `AdminDepartmentService`
- `Pageable` 기반 진료과 목록 조회
- `totalCount`, `currentPage`, `totalPages`, `hasPrevious`, `hasNext` 계산
- page link 생성

3. `AdminDepartmentRepository`
- `admin.department` 소유 Repository 기준으로 조회 책임 정리
- 필요 시 활성 상태/이름 정렬/페이징 쿼리 추가

4. `department-list.mustache`
- 깨진 한글 문구 복구
- 깨진 마크업 복구
- `{{pageTitle}}` 적용
- `{{model.xxx}}` 기반 목록/페이지네이션 렌더링 추가

5. 테스트
- Controller 테스트 추가
- Service 테스트 추가
- 기본 페이징과 목록 렌더링 검증

## 수용 기준
- [ ] `GET /admin/department/list`에서 페이징 목록이 조회된다
- [ ] 기본 페이징 파라미터가 적용된다
- [ ] 진료과 목록 화면에 페이지네이션 UI가 렌더링된다
- [ ] `AdminDepartmentRepository` 기준으로 조회 책임이 정리된다
- [ ] 깨진 한글/마크업이 복구된다
- [ ] 관련 테스트가 통과한다

## 우선순위
1. 템플릿 깨짐 복구
2. 서비스/리포지토리 책임 정리
3. 페이징 목록 구현
4. 테스트 추가