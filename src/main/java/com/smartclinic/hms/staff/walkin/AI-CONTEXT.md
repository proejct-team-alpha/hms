<!-- Parent: ../AI-CONTEXT.md -->

# staff/walkin

## 목적

현장 방문 환자의 즉석 접수(Walk-in) 기능을 담당한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| WalkinController.java | 현장 접수 등록 페이지 및 처리 컨트롤러 |
| WalkinService.java | 현장 접수 시 환자 검색/등록 및 예약 생성 로직 |

## AI 작업 지침

- 현장 접수는 예약 없이 바로 내원한 환자를 대상으로 하므로, `ReservationSource.WALKIN` 등의 구분값을 사용한다.
