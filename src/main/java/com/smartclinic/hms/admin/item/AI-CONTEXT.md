<!-- Parent: ../AI-CONTEXT.md -->

# admin/item

## 목적

병원의 소모품 및 장비(Item)를 관리한다. 물품의 카테고리, 수량, 재고 경고 상태를 CRUD 처리한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| AdminItemController.java | 물품 목록, 등록, 수정, 삭제 컨트롤러 (`/admin/item`) |
| ItemRepository.java | 물품 조회 및 카테고리별 집계, 재고 부족 물품 조회 담당 |

## AI 작업 지침

- 새로운 물품 카테고리 추가 시 `ItemCategory` 이넘(Enum)을 확인한다.
- 재고 부족 알림 로직은 `ItemRepository.countLowStockItems()`를 참고한다.

## 의존성

- 내부: `domain/Item`, `domain/ItemCategory`
