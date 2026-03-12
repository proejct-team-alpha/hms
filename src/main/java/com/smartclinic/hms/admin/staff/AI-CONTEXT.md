<!-- Parent: ../AI-CONTEXT.md -->

# admin/staff

## 목적

병원 직원(Staff) 정보를 관리한다. 직원 등록, 정보 수정, 활성화/비활성화 상태 관리를 담당한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| AdminStaffController.java | 직원 목록, 등록, 수정 컨트롤러 (`/admin/staff`) |
| AdminStaffRepository.java | 직원 조회 및 활성 직원 수 집계 담당 |

## AI 작업 지침

- 직원 역할(Role)은 `StaffRole` 이넘을 사용하며, 접근 권한과 연동된다.
- 직원 비활성화 시 `active` 필드를 사용한다.

## 의존성

- 내부: `domain/Staff`, `domain/StaffRole`
