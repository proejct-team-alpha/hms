# HMS Repository 규칙

> **기준**: Spring Boot 4.0.x + Spring Data JPA

본 문서는 Repository 계층의 배치/네이밍/쿼리 작성 기준을 정의한다.
핵심은 **소유 모듈 배치**와 **Spring Data JPA 인터페이스 우선**이다.

---

## 1. 목적

- Repository를 기능 소유 모듈 기준으로 일관되게 배치한다.
- 화면/통계 이름 기반의 임의 Repository 생성을 방지한다.
- 파생 메서드 중심의 단순하고 예측 가능한 데이터 접근 규칙을 유지한다.

---

## 2. 핵심 원칙

### 2.1 소유 모듈 배치

- Repository는 화면 패키지가 아니라 **해당 기능의 소유 도메인 모듈**에 둔다.
- 예시:
  - `com.smartclinic.hms.admin.reservation`
  - `com.smartclinic.hms.admin.staff`
  - `com.smartclinic.hms.admin.item`
- `dashboard`는 조합/집계 서비스 계층으로만 사용하고 Repository를 두지 않는다.

### 2.2 Spring Data JPA 인터페이스 우선

- Repository는 인터페이스로 작성하고 `JpaRepository<Entity, PK>`를 상속한다.
- 수동 구현 클래스/`EntityManager` 직접 구현은 예외 상황에서만 허용한다.

### 2.3 네이밍 규칙

- 클래스명은 화면명이 아니라 도메인/기능 기준으로 작성한다.
- 허용 예시:
  - `AdminReservationRepository`
  - `AdminStaffRepository`
  - `ItemRepository`
- 금지 예시:
  - `AdminDashboardStatsRepository`
  - `DashboardRepository`
  - `StatsRepository`

---

## 3. 구현 규칙

1. 쿼리 작성 우선순위
- 1순위: 파생 메서드
- 2순위: Projection
- 3순위: `@Query`

2. 집계 처리 규칙
- 단일 도메인 집계는 해당 도메인 Repository에서 처리한다.
- 다중 도메인 집계는 전용 통계 Repository를 만들지 않고 Service에서 조합한다.
- 집계 결과 가공(비율/임계치 판정/상태 분류)은 Service 책임으로 둔다.

3. EntityManager 사용 예외 규칙
- 원칙: `EntityManager` 직접 사용 금지
- 예외 허용 조건(모두 충족):
  - 파생 메서드/Projection/`@Query`로 구현 불가
  - 성능 또는 기능상 필요 근거를 코드 주석 또는 설계 문서로 명시
  - 팀 코드리뷰에서 예외 사용 승인

4. 패키지 구조 예시
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

---

## 4. 체크리스트

- [ ] Repository가 소유 기능 모듈 패키지에 위치하는가
- [ ] `JpaRepository` 인터페이스 기반으로 작성했는가
- [ ] 쿼리 우선순위(파생 메서드 -> Projection -> `@Query`)를 지켰는가
- [ ] 금지 네이밍(`AdminDashboardStatsRepository`, `DashboardRepository`, `StatsRepository`)을 사용하지 않았는가
- [ ] 다중 도메인 집계를 Service 조합으로 처리했는가
- [ ] `EntityManager` 사용 시 예외 조건 3가지를 모두 충족했는가

---

## 5. 요약

- Repository는 **소유 도메인 모듈**에 배치한다.
- 구현은 **Spring Data JPA 인터페이스 우선**으로 유지한다.
- 쿼리는 **파생 메서드 -> Projection -> `@Query`** 순으로 선택한다.
- 다중 도메인 집계는 **Service 조합**으로 처리한다.
- `EntityManager`는 문서화 + 리뷰 승인된 예외에서만 사용한다.
