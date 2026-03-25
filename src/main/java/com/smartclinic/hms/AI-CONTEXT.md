<!-- Parent: ../../../../../../../../../AI-CONTEXT.md -->

# com.smartclinic.hms — Java 소스 루트

## 목적

HMS 전체 Spring Boot 애플리케이션 소스. 패키지별로 기능이 수직 분리되어 있으며,
각 패키지는 Controller → Service → Repository → DTO 스택을 독립적으로 소유한다.

## 주요 파일

| 파일 | 설명 |
|------|------|
| HmsApplication.java | Spring Boot 진입점 (`@SpringBootApplication`) |

## 하위 패키지

| 패키지 | 담당 | 설명 |
|--------|------|------|
| `config/` | 책임개발자 | SecurityConfig, WebMvcConfig, ClaudeApiConfig, RateLimitFilter |
| `domain/` | 책임개발자 | JPA 엔티티 15개 (Doctor, Patient, Reservation 등) |
| `common/` | 책임개발자 | GlobalExceptionHandler, LayoutModelInterceptor, Resp, ReservationNumberGenerator |
| `auth/` | 책임개발자 | AuthController, CustomUserDetailsService, StaffRepository |
| `home/` | 개발자 A | HomeController (GET /) |
| `reservation/` | 개발자 A | 비회원 예약 흐름 (S00~S04) |
| `staff/` | 개발자 B | 스태프 접수·방문·전화예약 |
| `doctor/` | 개발자 B | 의사 대시보드·진료완료 처리 |
| `nurse/` | 개발자 B | 간호사 대시보드·환자정보·일정 |
| `admin/` | 개발자 C | 관리자 대시보드·CRUD 관리 |
| `llm/` | 책임개발자(Service) + B(UI) | Claude API 증상분석·챗봇 |
| `_sample/` | 참고용 | 패턴 샘플 코드 (배포 제외) |

## 패키지별 AI-CONTEXT

- [config/](config/AI-CONTEXT.md)
- [domain/](domain/AI-CONTEXT.md)
- [common/](common/AI-CONTEXT.md)
- [auth/](auth/AI-CONTEXT.md)
- [home/](home/AI-CONTEXT.md)
- [reservation/](reservation/AI-CONTEXT.md)
- [staff/](staff/AI-CONTEXT.md)
- [doctor/](doctor/AI-CONTEXT.md)
- [nurse/](nurse/AI-CONTEXT.md)
- [admin/](admin/AI-CONTEXT.md)
- [llm/](llm/AI-CONTEXT.md)

## AI 작업 지침

- 패키지 간 의존 방향: `reservation → common/service`, `reservation → domain`
- 타 모듈 패키지에 파일 생성 금지 (수직 소유 원칙)
- `config/`, `domain/`, `common/service/SlotService`, `llm/LlmService` 수정 금지
- 새 Repository는 반드시 해당 모듈 패키지 안에 배치 (dashboard Repository 금지)
