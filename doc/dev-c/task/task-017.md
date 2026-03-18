# task-017

## 목적

- [ ] `workflow-017`의 진료과 목록(25) 보완 범위를 구현/검증 단위로 나누어 순차 진행한다.
- [ ] 관리자 진료과 목록을 SSR 기준으로 정상 동작하게 정리한다.
- [ ] 전체 조회 구조를 페이징 목록 구조로 변경한다.
- [ ] `admin.department` 모듈 책임에 맞게 컨트롤러/서비스/리포지토리를 정리한다.
- [ ] 깨진 템플릿 문구와 마크업을 복구한다.
- [ ] 테스트를 추가해 목록 기능을 보장한다.

## Task 17-1. 현재 목록 화면 복구

- [x] `department-list.mustache` 한글 깨짐 복구
- [x] 깨진 HTML 태그 수정
- [x] `<title>` / 화면 제목 / 설명 문구 정리
- [x] 빈 목록 메시지 정리
- [x] `{{pageTitle}}` 사용 구조 적용
- [x] 기본 렌더링 확인

## Task 17-2. 목록 조회 책임 정리

- [ ] `AdminDepartmentService`에서 `reservation.reservation.DepartmentRepository` 의존 제거
- [ ] `AdminDepartmentRepository` 기준으로 조회 책임 통일
- [ ] 불필요한 타 모듈 의존 제거 확인
- [ ] 진료과 활성 상태/정렬 기준 점검

## Task 17-3. 진료과 목록 페이징 구현

- [ ] `AdminDepartmentController`에 `page`, `size` 요청 파라미터 추가
- [ ] 기본값 `page=1`, `size=10` 적용
- [ ] `Pageable` 기반 목록 조회 구현
- [ ] `totalCount` 계산 추가
- [ ] `currentPage` 계산 추가
- [ ] `totalPages` 계산 추가
- [ ] `hasPrevious`, `hasNext` 계산 추가
- [ ] page link DTO/응답 모델 추가
- [ ] `model + pageTitle` 패턴 적용

## Task 17-4. 템플릿 페이지네이션 렌더링 반영

- [ ] 목록을 `{{model.xxx}}` 기준으로 렌더링하도록 정리
- [ ] 총 건수 표시 추가
- [ ] 현재 페이지 정보 표시 추가
- [ ] 이전 링크 추가
- [ ] 다음 링크 추가
- [ ] 페이지 번호 링크 추가
- [ ] 빈 목록 처리 유지 확인

## Task 17-5. 등록 폼 계약 정리

- [ ] 등록 모달 필드와 서버 입력 계약 비교
- [ ] `active` 체크박스 사용 여부 결정
- [ ] 화면에서 보내는 값과 서버에서 받는 값 일치화
- [ ] 필요 시 DTO 도입 여부 정리
- [ ] 등록 후 목록 복귀 흐름 확인

## Task 17-6. 테스트 추가

- [ ] `AdminDepartmentControllerTest` 생성
- [ ] `AdminDepartmentServiceTest` 생성
- [ ] 기본 페이징 검증 추가
- [ ] 목록 렌더링 검증 추가
- [ ] 빈 목록 처리 검증 추가
- [ ] 페이지 링크 계산 검증 추가
- [ ] `admin.department` 범위 테스트 실행

## 완료 기준

- [ ] `GET /admin/department/list`에서 페이징 목록이 조회된다
- [ ] 기본 페이징 파라미터가 적용된다
- [ ] 진료과 목록 화면에 페이지네이션 UI가 렌더링된다
- [ ] `AdminDepartmentRepository` 기준으로 조회 책임이 정리된다
- [ ] 깨진 한글/마크업이 복구된다
- [ ] 관련 테스트가 통과한다

## 추천 진행 순서

- [ ] Task 17-1 화면/문구 복구
- [ ] Task 17-2 조회 책임 정리
- [ ] Task 17-3 페이징 서비스/컨트롤러 구현
- [ ] Task 17-4 페이지네이션 UI 반영
- [ ] Task 17-5 등록 폼 계약 정리
- [ ] Task 17-6 테스트 추가

## 메모

- [ ] 현재 진료과 목록은 기능이 일부 있는 초안 상태로 보고 진행
- [ ] 화면 복구와 페이징 보완을 분리해서 진행
- [ ] `workflow-017`은 보완 명세, `task-017`은 실제 구현 단위 분할 문서로 사용
