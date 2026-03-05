# Repository 작성 규칙 (HMS)

## 목적

- Repository를 기능 모듈 소유 원칙에 맞춰 일관되게 작성한다.
- 화면/통계 이름 기반의 임시 Repository 생성을 방지한다.

## 기본 원칙

- Repository는 **소유 기능 모듈(도메인) 패키지**에 둔다.
- 형태는 **Spring Data JPA 인터페이스**를 기본으로 한다.
- `JpaRepository<엔티티, PK>` 상속을 기본으로 한다.
- 조회는 **메서드 네이밍 쿼리**를 우선 사용한다.
- 서비스는 필요한 도메인 Repository를 조합해 비즈니스 로직/집계를 처리한다.

## 패키지 배치 규칙

- 고정 경로를 강제하지 않는다.
- Repository는 해당 기능을 소유한 모듈 아래에 둔다.
- 예시(참고):
  - `com.smartclinic.hms.admin.reservation`
  - `com.smartclinic.hms.admin.staff`
  - `com.smartclinic.hms.admin.item`
- 금지:
  - 화면/서비스 이름 패키지(`dashboard`, `stats` 등)에 Repository를 생성하는 방식

## 패키지 구조 예시

```text
com.smartclinic.hms
└─ admin
   ├─ dashboard
   │  └─ AdminDashboardStatsService
   ├─ reservation
   │  └─ AdminReservationRepository
   ├─ staff
   │  └─ AdminStaffRepository
   └─ item
      └─ ItemRepository
```

- `dashboard`는 서비스/조합 계층만 두고, Repository는 각 도메인 패키지에 둔다.

## 네이밍 규칙

- 클래스명은 화면명이 아니라 도메인/기능명 중심으로 작성한다.
- 허용 예시:
  - `AdminReservationRepository`
  - `AdminStaffRepository`
  - `ItemRepository`
- 금지 예시:
  - `AdminDashboardStatsRepository`
  - `XxxScreenRepository`, `XxxViewRepository`

## 쿼리 작성 규칙

- 1순위: 파생 메서드
  - `countByReservationDate(...)`
  - `countByActiveTrue()`
- 2순위: Projection 인터페이스로 필요한 컬럼만 조회
  - `findAllProjectedBy()` + Projection
- 3순위: `@Query`/JPQL (파생 메서드로 불가능한 경우)

## 집계 처리 규칙

- 단일 도메인 집계는 해당 도메인 Repository의 파생 메서드/Projection으로 처리한다.
  - 예:
    - `ReservationRepository.countByReservationDate(...)`
    - `StaffRepository.countByActiveTrue()`
    - `ItemRepository.countByQuantityLessThanMinQuantity()`
- 다중 도메인 집계는 전용 Repository를 만들지 않고 Service에서 조합한다.
  - 예: DashboardService에서 - ReservationRepository - StaffRepository - ItemRepository
    를 조합해 대시보드 통계를 계산한다.
- 집계 결과 가공(비율, 상태 분류, 임계치 판정 등)은 Service 책임으로 둔다.
- 집계용 화면(대시보드/통계)이라도 Repository 위치 기준은 도메인 소유 원칙을 따른다.

## EntityManager 사용 제한 (강화)

- 원칙적으로 `EntityManager` 직접 사용 금지.
- 아래 조건을 모두 만족할 때만 예외적으로 허용:
  - 파생 메서드 + Projection + `@Query`로 구현 불가
  - 성능/기능상 근거가 코드 주석 또는 설계 문서에 명시됨
  - 팀 리뷰에서 예외 사용 승인을 받음
- 허용 시에도 위치는 도메인 모듈 내부로 제한하고, 화면/통계 전용 패키지에는 두지 않는다.

## 서비스 계층 규칙

- `@Transactional(readOnly = true)`를 기본으로 한다.
- Repository는 DB 접근만 담당한다.
- 비즈니스 계산/집계 조합은 Service에서 처리한다.

## 체크리스트

- [ ] Repository가 소유 기능 모듈 패키지에 위치하는가
- [ ] `JpaRepository` 인터페이스 기반인가
- [ ] 메서드 네이밍 쿼리를 우선 적용했는가
- [ ] 화면/통계 전용 이름의 Repository를 만들지 않았는가
- [ ] 비즈니스 로직이 Repository에 들어가지 않았는가

## 참고 문서

- `AGENTS.md`
- `doc/PROJECT_STRUCTURE.md`
- `doc/RULE.md`
- `doc/rules/rule_spring.md`
