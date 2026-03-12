<!-- Parent: ../AI-CONTEXT.md -->

# common/util

## 목적

전역적으로 사용되는 유틸리티 클래스와 공통 응답 규격을 정의한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| ReservationNumberGenerator.java | 고유한 예약 번호를 생성하는 유틸리티 |
| Resp.java | API 응답 공통 규격 (Success, Fail, Data 포함) |

## AI 작업 지침

- AJAX/REST API 응답 시 항상 `Resp.ok()` 또는 `Resp.fail()`을 사용하여 규격을 일관되게 유지한다.
