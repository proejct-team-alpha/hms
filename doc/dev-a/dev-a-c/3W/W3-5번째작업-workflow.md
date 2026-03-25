# W3-5번째작업 Workflow — 의사 진료실 물품 사용(출고) 기능

> **작성일**: 3W
> **목표**: 의사 진료 기록 작성 화면 좌측에 물품 사용 섹션 추가 (카테고리 필터 + 초성 검색 + AJAX 출고)

---

## 전체 흐름

```
ItemUsageLog 엔티티 신규 생성 → ItemManagerService 출고 메서드 추가
  → DoctorTreatmentController 수정 (items·usageLogs 모델 추가, AJAX 엔드포인트)
  → treatment-detail.mustache 물품 사용 섹션 추가
  → DoctorTreatmentService 쿼리 변경 (CANCELLED 제외 → RECEIVED만)
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 의사 진료 상세 화면에 물품 사용 기능 추가 |
| 표시 조건 | `canComplete=true` (RECEIVED 상태 환자)일 때만 물품 사용 섹션 표시 |
| 카테고리 필터 | 전체 / 의료소모품 / 의료기기 / 사무비품 (JS 클라이언트 필터) |
| 검색 | 한글 초성 + 영문 대소문자 구분 없이 검색 |
| 출고 이력 | `ItemUsageLog` 엔티티 신규 생성, `reservationId`로 예약 연결 |
| 진료 완료 뷰 | 완료 환자는 사용 물품 이력을 읽기 전용으로 표시 |

---

## 실행 흐름

```
GET /doctor/treatment-detail/{id}
  → DoctorTreatmentController: items, usageLogs 모델 추가
  → treatment-detail.mustache 렌더링
    - {{#detail.canComplete}} 물품 사용 섹션 표시
    - {{^detail.canComplete}} 완료 뷰 (사용 이력 읽기 전용)

사용 버튼 클릭 (AJAX)
  → fetch POST /doctor/item/use (id, amount, reservationId)
  → ItemManagerService.useItem() → 재고 차감 + ItemUsageLog 저장
  → 성공: {"quantity": N} → 카드 재고 즉시 갱신
  → 재고 부족: 400 + {"error": "재고가 N개 부족합니다."} → alert
```

---

## UI Mockup

```
┌──────────────────────────────────────────────────────┐
│ [환자 정보 카드] 홍길동 / 09:00 / 내과               │
├──────────────────────────────────────────────────────┤
│ 물품 사용 ({{#detail.canComplete}}만 표시)            │
│ [전체] [의료소모품] [의료기기] [사무비품]              │
│ [검색: 물품명 입력...]                                │
│ ┌──────┬────────┬──────┬─────────┐                   │
│ │ 붕대  │의료소모품│ 25개  │ [2] [사용] │                   │
│ │ 주사기│의료소모품│  3개  │ [1] [사용] │                   │
│ └──────┴────────┴──────┴─────────┘                   │
└──────────────────────────────────────────────────────┘
```

---

## 작업 목록

1. `item/log/ItemUsageLog.java` — 물품 사용 이력 엔티티 신규 생성
2. `item/log/ItemUsageLogRepository.java` — JPA 리포지토리 신규 생성
3. `item/log/ItemUsageLogDto.java` — 뷰용 DTO 신규 생성
4. `ItemManagerService` — `useItem()`, `getUsageLogs()` 메서드 추가
5. `item/dto/ItemListDto` — `category` 필드 추가
6. `DoctorTreatmentController` — items·usageLogs 모델 추가, AJAX 엔드포인트 추가
7. `DoctorTreatmentService` — `getTreatmentPage()` 쿼리 변경
8. `treatment-detail.mustache` — 물품 사용 섹션 추가, 완료 뷰 개선
9. `completed-list.mustache` — 링크 `/completed-detail`로 변경
10. `sql_test.sql` — 오늘 날짜 테스트 데이터 추가

---

## 작업 진행내용

- [x] ItemUsageLog 엔티티·Repository·Dto 신규 생성
- [x] ItemManagerService `useItem()`, `getUsageLogs()` 추가
- [x] ItemListDto `category` 필드 추가
- [x] DoctorTreatmentController 수정 (items·usageLogs 모델, AJAX 엔드포인트)
- [x] DoctorTreatmentService 쿼리 변경 (RECEIVED만)
- [x] treatment-detail.mustache 물품 사용 섹션 + 완료 뷰 개선
- [x] completed-list.mustache 링크 변경
- [x] sql_test.sql 테스트 데이터 추가

---

## 실행 흐름에 대한 코드

### 1. ItemUsageLog — 엔티티

```java
// 물품 사용 이력 엔티티
@Entity
@Table(name = "item_usage_log")
public class ItemUsageLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long reservationId;   // null 허용 (예약 없는 출고)
    private Long itemId;
    private String itemName;
    private int amount;
    private LocalDateTime usedAt;

    // 팩토리 메서드
    public static ItemUsageLog of(Long reservationId, Long itemId, String itemName, int amount) {
        ItemUsageLog log = new ItemUsageLog();
        log.reservationId = reservationId;
        log.itemId = itemId;
        log.itemName = itemName;
        log.amount = amount;
        log.usedAt = LocalDateTime.now();
        return log;
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 물품 사용 이력을 DB에 저장하는 엔티티(테이블 매핑 클래스)를 만듭니다. "언제 어떤 진료에서 어떤 물품을 몇 개 사용했는지" 기록합니다.
> - **왜 이렇게 썼는지**: 팩토리 메서드 `of()`로 객체 생성 로직을 캡슐화하면, 외부에서 필드 순서를 외울 필요 없이 의미 있는 이름의 메서드로 생성할 수 있습니다.
> - **쉽게 말하면**: 물품 사용 기록 카드를 DB에 남기는 설계도입니다.

### 2. ItemManagerService — useItem()

```java
// 물품 사용(출고) + 이력 저장
@Transactional
public int useItem(Long id, int amount, Long reservationId) {
    Item item = itemRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("물품 없음"));

    int newQuantity = item.getQuantity() - amount;
    if (newQuantity < 0) {
        throw new IllegalStateException("재고가 " + (-newQuantity) + "개 부족합니다.");
    }

    item.updateQuantity(newQuantity);
    itemUsageLogRepository.save(ItemUsageLog.of(reservationId, id, item.getName(), amount));
    return newQuantity;
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 물품 사용(출고) 서비스 메서드입니다. 재고가 부족하면 예외를 발생시키고, 충분하면 재고를 차감하고 이력을 저장한 뒤 새 재고 수량을 반환합니다.
> - **왜 이렇게 썼는지**: 서비스 계층에서 재고 부족을 검사해 예외를 던지면, 컨트롤러에서 해당 예외를 잡아서 400 응답으로 변환할 수 있습니다.
> - **쉽게 말하면**: 재고가 충분하면 줄이고, 부족하면 "재고가 N개 부족합니다"라고 알려주는 기능입니다.

### 3. treatment-detail.mustache — 카테고리 JS 필터 + AJAX

```javascript
// 카테고리 탭 버튼 필터
document.querySelectorAll('.category-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        const cat = this.dataset.category;
        document.querySelectorAll('[data-category]').forEach(card => {
            card.style.display = (cat === 'ALL' || card.dataset.category === cat) ? '' : 'none';
        });
    });
});

// 물품 사용 AJAX
document.querySelectorAll('form[action$="/doctor/item/use"]').forEach(form => {
    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        const res = await fetch('/doctor/item/use', { method: 'POST', body: new URLSearchParams(new FormData(form)) });
        if (res.ok) {
            const data = await res.json();
            form.closest('[data-item-card]').querySelector('.qty-span').textContent = data.quantity + '개';
        } else {
            const err = await res.json();
            alert(err.error);
        }
    });
});
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 카테고리 버튼 클릭 시 해당 카테고리 물품만 필터링해 보여주고, 사용 버튼 클릭 시 AJAX로 재고를 차감합니다.
> - **왜 이렇게 썼는지**: `data-category` 속성을 이용한 JS 필터링은 서버 요청 없이 즉시 동작해 빠릅니다. AJAX 출고는 페이지 이동 없이 재고 수량만 갱신합니다.
> - **쉽게 말하면**: 탭 버튼으로 카테고리를 골라 원하는 물품을 찾고, 사용 버튼으로 재고를 차감하는 기능입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 진료 중 환자 | `canComplete=true` | 물품 사용 섹션 표시 |
| 진료 완료 환자 | `canComplete=false` | 사용 이력 읽기 전용 표시 |
| 카테고리 필터 | 탭 버튼 클릭 | 해당 카테고리 물품만 표시 |
| 물품 사용 성공 | 유효한 수량 입력 | 재고 즉시 갱신 |
| 재고 부족 | 재고 초과 수량 입력 | alert "재고가 N개 부족합니다." |
| 오늘의 진료 목록 | 페이지 접속 | RECEIVED 상태 환자만 표시 |

---

## 완료 기준

- [x] 진료 중(`canComplete=true`): 물품 사용 섹션 표시, 카테고리 필터 + 초성 검색 + AJAX 출고 정상 작동
- [x] 진료 완료(`canComplete=false`): 완료 뷰에서 사용 물품 내역 조회 가능
- [x] 오늘의 진료 목록: 진료 대기(RECEIVED) 환자만 표시
- [x] `config/`, `domain/` 수정 없음 (`updateQuantity()` 기존 메서드 재사용)
