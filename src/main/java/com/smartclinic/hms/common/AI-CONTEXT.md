<!-- Parent: ../AI-CONTEXT.md -->

# common — 공통 레이어

## 목적

전 모듈이 공유하는 예외처리, 인터셉터, 유틸리티, 공통 서비스를 제공한다.
**책임개발자 소유 (공유 레이어).** SlotService / ReservationValidationService는 수정 금지.

## 하위 패키지 및 파일

### exception/
| 파일 | 설명 |
|------|------|
| CustomException.java | 애플리케이션 전용 예외. 팩토리 메서드로 생성 (`notFound()`, `forbidden()`, `conflict()` 등) |
| GlobalExceptionHandler.java | `@RestControllerAdvice`. CustomException → ErrorResponse record 변환 후 JSON 응답 |

### interceptor/
| 파일 | 설명 |
|------|------|
| LayoutModelInterceptor.java | 모든 요청에 공통 모델 변수 자동 주입 |

LayoutModelInterceptor 주입 변수:
- `pageTitle`, `currentPath`, `loginName`
- `isAdmin`, `isDoctor`, `isNurse`, `isStaff`
- `showChatbot`, `dashboardUrl`

### service/
| 파일 | 설명 |
|------|------|
| SlotService.java | 30분 단위 슬롯 생성, 중복 예약 방지 (`getAvailableSlots()`, `validateAndLock()`) |
| ReservationValidationService.java | 예약 유효성 검증 (의사 진료 요일, 시간 범위 등) |

### util/
| 파일 | 설명 |
|------|------|
| ReservationNumberGenerator.java | 예약번호 생성 (`RES-YYYYMMDD-NNN` 형식) |
| Resp.java | AJAX 공통 응답 래퍼 (`Resp.ok(data)`, `Resp.error(message)`) |

## AI 작업 지침

- `CustomException` 직접 `new` 금지 → 반드시 팩토리 메서드 사용
- `IllegalArgumentException` 직접 throw 금지 → `CustomException` 사용
- Controller에서 AJAX 응답 시 `Resp.ok()` 사용
- `SlotService`, `ReservationValidationService` 수정 금지 — 인터페이스 호출만

## 의존성

- 내부: `domain/`
- 외부: Spring Web, Spring Security
