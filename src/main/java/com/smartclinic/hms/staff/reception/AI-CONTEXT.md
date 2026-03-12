<!-- Parent: ../AI-CONTEXT.md -->

# staff/reception

## 목적

스태프용 내원 환자 접수 및 상태 관리 기능을 제공한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| ReceptionController.java | 접수 목록 조회 및 상태 변경 컨트롤러 |
| ReceptionService.java | 접수 상태(대기, 진료 중 등) 변경 로직 처리 |

## AI 작업 지침

- 접수 상태 변경 시 관련 도메인 엔티티(`Reservation` 등)의 상태 값 전이를 확인한다.
