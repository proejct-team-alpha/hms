<!-- Parent: ../AI-CONTEXT.md -->

# templates/doctor

## 목적

의사(DOCTOR) 업무 처리를 위한 Mustache 템플릿.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| dashboard.mustache | 의사 대시보드 (대기 환자 목록 등) |
| treatment-list.mustache | 진료 대기/진행 목록 |
| treatment-detail.mustache | 진료 상세 기록 및 처방 입력 페이지 |
| completed-list.mustache | 진료 완료 내역 |

## AI 작업 지침

- 진료 기록 저장 시 AJAX 또는 폼 전송 방식을 컨트롤러 규격에 맞춰 사용한다.
