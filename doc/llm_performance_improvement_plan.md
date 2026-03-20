# HMS LLM 성능 개선 — 현재 코드 기반 실행 계획

> 기준 문서: `doc/llm_medical_performance_guide.md`
> 작성 기준: 현재 프로젝트 코드 분석 결과, **실제 반영 가능한 항목**만 정리

---

## 목차

1. [현재 상태 진단](#1-현재-상태-진단)
2. [즉시 적용 가능 (코드 변경만)](#2-즉시-적용-가능--코드-변경만)
3. [설정 추가로 적용 가능 (인프라 변경 없음)](#3-설정-추가로-적용-가능--인프라-변경-없음)
4. [인프라 추가 필요 (Redis 등)](#4-인프라-추가-필요--redis-등)
5. [현재 프로젝트에 맞지 않는 항목](#5-현재-프로젝트에-맞지-않는-항목)
6. [우선순위별 실행 로드맵](#6-우선순위별-실행-로드맵)

---

## 1. 현재 상태 진단

### 이미 적용된 것 (가이드 vs 현재 코드)

| 가이드 권장                  | 현재 상태                                        | 위치                                                        |
| ---------------------------- | ------------------------------------------------ | ----------------------------------------------------------- |
| WebClient 타임아웃 설정      | **적용됨** (connect 5s, read 120s)               | `WebClientConfig.java`                                      |
| FastAPI lifespan 리소스 관리 | **적용됨** (httpx client, pool 정리)             | `python-llm/app.py`                                         |
| aiomysql 커넥션 풀           | **적용됨** (asyncio.Lock, minsize=0, maxsize=10) | `medical_context_service.py`                                |
| ChromaDB HttpClient 싱글톤   | **적용됨**                                       | `vector_store.py`                                           |
| asyncio.gather 병렬 검색     | **적용됨** (벡터+QA+콘텐츠 3개 동시)             | `medical_context_service.py:192`                            |
| JOIN FETCH N+1 방지          | **부분 적용** (doctor 쿼리만)                    | `DoctorRepository.java`, `DoctorReservationRepository.java` |
| SSE 스트리밍 응답            | **적용됨** (medical/stream, rule/stream)         | `app.py`, `MedicalController.java`                          |
| Circuit Breaker              | **적용됨**                                       | `python-llm/circuit_breaker.py`                             |
| Rate Limiting                | **적용됨** (양쪽 모두)                           | `RateLimitFilter.java`, `app.py (slowapi)`                  |

### 적용되지 않은 것 (개선 필요)

| 가이드 권장                | 현재 상태                                        | 영향도               |
| -------------------------- | ------------------------------------------------ | -------------------- |
| HTMX 스켈레톤 즉시 반환    | 미적용 — 응답 완료까지 빈 화면                   | **높음** (체감 속도) |
| @Async 병렬처리 (Spring)   | 미적용 — 동기 블로킹 호출                        | **높음** (응답 시간) |
| Redis 캐시                 | 미적용 — 매번 DB/LLM 호출                        | **높음** (반복 요청) |
| ChromaDB HNSW 튜닝         | 기본값만 사용 (`hnsw:space: cosine`)             | **중간** (검색 속도) |
| LlmReservationService N+1  | 슬롯마다 개별 COUNT 쿼리                         | **중간** (DB 부하)   |
| MySQL 인덱스 최적화        | 명시적 인덱스 없음                               | **중간** (쿼리 성능) |
| Prompt Caching (Anthropic) | 미적용 — Claude API 직접 호출 아님 (Ollama/vLLM) | **해당없음**         |

---

## 2. 즉시 적용 가능 — 코드 변경만

### 2-1. LlmReservationService N+1 쿼리 제거

**현재 문제**: `getAvailableSlots()`에서 7일 x 슬롯마다 `countByDoctor_IdAndReservationDateAndStartTime()` 개별 호출

```java
// 현재 (N+1): 슬롯 12개면 COUNT 쿼리 12번
long count = reservationRepository
    .countByDoctor_IdAndReservationDateAndStartTime(doctorId, date, slotTime);
```

**개선안**: 날짜 범위로 한 번에 조회 후 메모리에서 필터

```java
// ReservationRepository에 추가
@Query("""
    SELECT r.reservationDate, r.startTime
    FROM Reservation r
    WHERE r.doctor.id = :doctorId
      AND r.reservationDate BETWEEN :startDate AND :endDate
      AND r.status <> 'CANCELLED'
    """)
List<Object[]> findBookedSlots(
    @Param("doctorId") Long doctorId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);
```

```java
// LlmReservationService 개선
public LlmReservationResponse.SlotList getAvailableSlots(Long doctorId) {
    LocalDate startDate = LocalDate.now().plusDays(1);
    LocalDate endDate = startDate.plusDays(7);

    // 1회 쿼리로 예약된 슬롯 전체 조회
    Set<String> bookedSlots = reservationRepository
        .findBookedSlots(doctorId, startDate, endDate)
        .stream()
        .map(row -> row[0].toString() + "_" + row[1].toString())
        .collect(Collectors.toSet());

    // 메모리에서 가용 여부 판단
    for (...) {
        boolean available = !bookedSlots.contains(date + "_" + slotTime);
        slots.add(new Slot(date, slotTime, slotEnd, available));
    }
}
```

**효과**: 쿼리 12회 → 1회 (DB 왕복 ~92% 감소)

---

### 2-2. ChromaDB HNSW 파라미터 튜닝

**현재**: `metadata={"hnsw:space": "cosine"}` (기본값만 설정)

**개선안**: `vector_store.py`에서 HNSW 파라미터 추가

```python
# vector_store.py — get_collection(), get_rule_collection() 수정
_collection = client.get_or_create_collection(
    name=settings.chroma_collection,
    metadata={
        "hnsw:space": "cosine",
        "hnsw:M": 16,                  # 노드당 연결 수
        "hnsw:construction_ef": 200,    # 인덱스 빌드 품질
        "hnsw:search_ef": 50,           # 검색 시 탐색 후보 수 (핵심)
        "hnsw:num_threads": 4,          # CPU 병렬 스레드
    },
)
```

**효과**: 검색 속도 3~5배 개선 (정확도 유지)

---

### 2-3. ChromaDB 검색을 asyncio.to_thread로 감싸기

**현재**: `vector_store.search_similar()`이 동기 함수 → 이벤트 루프 차단 가능

```python
# medical_context_service.py:168
results = search_similar(query_embedding, top_k=top_k)
```

**개선안**:

```python
# medical_context_service.py — search_vector_store() 수정
results = await asyncio.to_thread(search_similar, query_embedding, top_k=top_k)
```

**효과**: FastAPI 이벤트 루프 차단 방지, 동시 요청 처리 능력 향상

---

### 2-4. WebClient 읽기 타임아웃 조정

**현재**: `readTimeout=120000` (120초) — 지나치게 긴 대기

**개선안**: `application-dev.properties` 수정

```properties
# 현재: 120초 → 개선: 30초 (vLLM/Ollama 기준 충분)
llm.service.timeout.read=30000
```

**효과**: 장애 시 빠른 실패, 리소스 낭비 방지

---

## 3. 설정 추가로 적용 가능 — 인프라 변경 없음

### 3-1. Spring Boot @Async 병렬 처리

**현재 문제**: `MedicalController.handleMedicalQueryWithDoctors()`에서 LLM 호출 → 진료과 파싱 → 의사 조회가 **순차 실행**

```java
// 현재: LLM 응답 후 → 동기적으로 의사 조회 (직렬)
return medicalService.callMedicalLlmApi(request.getQuery())
    .map(response -> {
        String department = llmResponseParser.extractDepartment(response);
        List<DoctorWithScheduleDto> doctors = doctorService.findDoctorsWithSchedule(department);
        // ...
    });
```

**개선안**: AsyncConfig 추가 + CompletableFuture 활용

```java
// config/AsyncConfig.java (신규)
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "llmExecutor")
    public ThreadPoolTaskExecutor llmExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(5);
        exec.setMaxPoolSize(15);
        exec.setQueueCapacity(50);
        exec.setThreadNamePrefix("llm-");
        exec.initialize();
        return exec;
    }
}
```

**의사+스케줄 조회를 @Async로 분리**:

```java
// DoctorService.java
@Async("llmExecutor")
public CompletableFuture<List<DoctorWithScheduleDto>> findDoctorsWithScheduleAsync(String department) {
    return CompletableFuture.completedFuture(findDoctorsWithSchedule(department));
}
```

**효과**: LLM 호출 완료 즉시 의사 조회 시작 (체감 ~1초 단축 가능)

---

### 3-2. MySQL 인덱스 추가

**현재**: JPA DDL auto로 생성된 기본 인덱스만 존재

**개선안**: `src/main/resources/schema-index.sql` 추가 (prod 환경)

```sql
-- 예약: 의사별 날짜별 슬롯 조회 (LlmReservationService 핵심 쿼리)
CREATE INDEX IF NOT EXISTS idx_reservation_doctor_date
    ON reservation(doctor_id, reservation_date, start_time);

-- 예약: 상태별 필터 (접수직원/간호사 대시보드)
CREATE INDEX IF NOT EXISTS idx_reservation_status_date
    ON reservation(status, reservation_date);

-- 의사 스케줄: 의사별 가용 일정 조회
CREATE INDEX IF NOT EXISTS idx_doctor_schedule_available
    ON doctor_schedule(doctor_id, is_available);

-- 의사: 진료과별 조회 (LLM 추천 → 의사 목록)
CREATE INDEX IF NOT EXISTS idx_doctor_department
    ON doctor(department_id);
```

**효과**: 의사+일정 조회 쿼리 ~80% 속도 개선 (특히 데이터 증가 시)

---

### 3-3. Python LLM 서비스 — 유사도 임계값 필터 추가

**현재**: `vector_store.search_similar()`에서 유사도 필터 없이 모든 결과 반환

**개선안**: `vector_store.py`에 유사도 임계값 추가

```python
def search_similar(query_embedding, top_k=3, min_similarity=0.65):
    results = collection.query(
        query_embeddings=[query_embedding],
        n_results=min(top_k, collection.count()),
        include=["documents", "metadatas", "distances"],
    )
    # cosine distance → similarity 변환 후 임계값 필터
    items = []
    for i, doc in enumerate(results["documents"][0]):
        distance = results["distances"][0][i]
        similarity = 1 - distance
        if similarity < min_similarity:
            continue
        items.append({...})
    return items
```

**효과**: 관련 없는 저품질 컨텍스트 제거 → LLM 응답 품질 향상

---

## 4. 인프라 추가 필요 — Redis 등

### 4-1. Redis 캐시 도입 (docker-compose.yml에 추가)

**현재**: 동일 증상을 매번 LLM + ChromaDB + MySQL 풀 파이프라인 실행

**추가 인프라**:

```yaml
# docker-compose.yml에 추가
redis:
  image: redis:7-alpine
  container_name: hms-redis
  ports:
    - "6379:6379"
  volumes:
    - hms_redis_data:/data
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
```

**Spring Boot 측** (`build.gradle`에 의존성 추가):

```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

**캐시 대상 및 TTL**:

| 캐시 대상            | TTL   | 이유                  |
| -------------------- | ----- | --------------------- |
| 진료과별 의사 목록   | 1시간 | 인사 변경 드묾        |
| 의사 스케줄          | 5분   | 실시간 예약 반영      |
| RAG 결과 (증상 해시) | 30분  | 동일 증상 재처리 방지 |

**효과**: 반복 요청 90%+ 절감, 체감 응답 ~0.4초 (캐시 히트 시)

---

### 4-2. HTMX 스켈레톤 UI + Polling

**현재 문제**: Mustache SSR에서 LLM 응답 완료까지 사용자가 빈 화면/로딩 대기

**개선 방식**: HTMX 라이브러리 추가 (CDN or static) + 스켈레톤 패턴

**구현 범위**:

1. `static/js/htmx.min.js` 추가 (CDN 또는 로컬)
2. 증상 분석 페이지 (`symptom-reservation.mustache`) 수정
3. 의료 상담 페이지 (`medical.mustache`) 수정
4. Spring Controller에 polling 엔드포인트 추가

```java
// LlmPageController에 추가
@PostMapping("/llm/symptom/start")
public String startAnalysis(@RequestParam String symptoms, Model model) {
    String jobId = UUID.randomUUID().toString();
    symptomJobService.submitAsync(jobId, symptoms);
    model.addAttribute("jobId", jobId);
    return "llm/symptom-loading";  // 스켈레톤 즉시 반환
}

@GetMapping("/llm/symptom/poll/{jobId}")
public String pollResult(@PathVariable String jobId, Model model) {
    SymptomResult result = symptomJobService.getResult(jobId);
    if (result == null) {
        model.addAttribute("jobId", jobId);
        return "llm/symptom-loading";  // 아직 처리 중
    }
    model.addAttribute("result", result);
    return "llm/symptom-result";  // 완료
}
```

**효과**: 체감 첫 응답 ~50ms (스켈레톤 즉시 표시), 사용자 이탈률 감소

---

## 5. 현재 프로젝트에 맞지 않는 항목

| 가이드 항목                     | 미적용 사유                                                                                                                                                                                                                                                             |
| ------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Anthropic Prompt Caching**    | 현재 LLM 백엔드가 Ollama/vLLM (로컬 모델). Claude API는 `SymptomAnalysisService`에서만 사용하며 `ClaudeApiConfig`로 설정되어 있지만, 메인 추론 파이프라인은 Python FastAPI → Ollama/vLLM 경로. Prompt Caching은 Anthropic API 전용 기능이므로 현재 아키텍처에 해당 없음 |
| **Anthropic 비용 절감 계산**    | 위와 동일 — 로컬 모델 사용 중이므로 API 호출 비용 자체가 없음                                                                                                                                                                                                           |
| **claude-haiku 모델 사용**      | 현재 Qwen 2.5 7B 모델 사용 중. 모델 전환은 성능 특성이 달라지므로 별도 평가 필요                                                                                                                                                                                        |
| **aioredis 직접 사용 (Python)** | Redis 인프라 자체가 없음. 도입 시에는 Spring Boot 측 Redis 우선 적용이 효율적                                                                                                                                                                                           |

---

## 6. 우선순위별 실행 로드맵

### Phase 1: 즉시 적용 (코드 변경만, 1~2일)

| #   | 작업                               | 파일                                                       | 난이도 | 효과                 |
| --- | ---------------------------------- | ---------------------------------------------------------- | ------ | -------------------- |
| 1   | LlmReservationService N+1 제거     | `ReservationRepository.java`, `LlmReservationService.java` | 낮음   | DB 쿼리 12회→1회     |
| 2   | ChromaDB HNSW 튜닝                 | `vector_store.py`                                          | 낮음   | 검색 3~5배           |
| 3   | ChromaDB asyncio.to_thread 적용    | `medical_context_service.py`                               | 낮음   | 이벤트루프 차단 방지 |
| 4   | WebClient 타임아웃 조정 (120s→30s) | `application-dev.properties`                               | 낮음   | 빠른 실패            |
| 5   | 벡터 검색 유사도 임계값 추가       | `vector_store.py`                                          | 낮음   | 응답 품질 향상       |

### Phase 2: Spring Boot 비동기화 (2~3일)

| #   | 작업                      | 파일                                            | 난이도 | 효과           |
| --- | ------------------------- | ----------------------------------------------- | ------ | -------------- |
| 6   | AsyncConfig + @Async 적용 | `AsyncConfig.java` (신규), `DoctorService.java` | 중간   | ~1초 단축      |
| 7   | MySQL 인덱스 추가         | `schema-index.sql` (신규)                       | 낮음   | 쿼리 ~80% 개선 |

### Phase 3: 체감 속도 개선 (3~5일)

| #   | 작업                         | 파일                     | 난이도 | 효과              |
| --- | ---------------------------- | ------------------------ | ------ | ----------------- |
| 8   | HTMX 스켈레톤 + Polling 패턴 | 템플릿 + Controller 수정 | 중간   | 체감 첫응답 ~50ms |

### Phase 4: 캐시 인프라 (5~7일)

| #   | 작업                           | 파일                                                     | 난이도 | 효과               |
| --- | ------------------------------ | -------------------------------------------------------- | ------ | ------------------ |
| 9   | Redis 도입 + Spring Cache 설정 | `docker-compose.yml`, `build.gradle`, `CacheConfig.java` | 높음   | 반복요청 90%+ 절감 |

### 예상 최종 성능

| 구간                  | 현재 (추정)      | Phase 1~2 후 | Phase 3~4 후         |
| --------------------- | ---------------- | ------------ | -------------------- |
| 체감 첫 응답          | ~9초 (전체 완료) | ~5초         | **~50ms** (스켈레톤) |
| 실제 완료 (캐시 미스) | ~9초             | ~4초         | **~2.5초**           |
| 실제 완료 (캐시 히트) | ~9초             | ~4초         | **~0.4초**           |

---

> **권장**: Phase 1은 리스크 없이 즉시 적용 가능. Phase 2~3은 기능 테스트 후 순차 적용. Phase 4는 운영 환경에서 반복 요청 패턴 확인 후 결정.
