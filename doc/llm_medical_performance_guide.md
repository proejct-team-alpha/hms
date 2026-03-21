# LLM 의료 진단 시스템 — 성능 최적화 완전 가이드

> 스택: Spring Boot (SSR Mustache) / FastAPI / MySQL / ChromaDB / Redis
> 목표: 순차 I/O로 인한 **~9초 응답시간 → ~2초 이하** 단축

---

## 목차

1. [문제 분석](#1-문제-분석)
2. [개선 전략 개요](#2-개선-전략-개요)
3. [아키텍처 설계](#3-아키텍처-설계)
4. [Spring Boot — HTMX 스켈레톤 + @Async 병렬처리](#4-spring-boot--htmx-스켈레톤--async-병렬처리)
5. [MySQL — N+1 제거 + 인덱스 최적화](#5-mysql--n1-제거--인덱스-최적화)
6. [Redis — 3계층 캐시 전략](#6-redis--3계층-캐시-전략)
7. [FastAPI — asyncio 완전 병렬화](#7-fastapi--asyncio-완전-병렬화)
8. [ChromaDB 튜닝](#8-chromadb-튜닝)
9. [Anthropic Prompt Caching 측정](#9-anthropic-prompt-caching-측정)
10. [최종 성능 요약](#10-최종-성능-요약)

---

## 1. 문제 분석

### 현재 구조 (순차 실행)

```
증상 입력
    ↓
LLM + Vector DB RAG 추론   ~3s
    ↓
RDB 쿼리 ① 진료과 조회     ~2s
    ↓
RDB 쿼리 ② 의사 목록 조회  ~2s
    ↓
RDB 쿼리 ③ 의사 일정 조회  ~2s
    ↓
화면 표시                  총 ~9s
```

### 병목 원인

| 원인        | 구체적 문제                                       |
| ----------- | ------------------------------------------------- |
| 순차 I/O    | 진료과 확정 후 의사·일정을 직렬 조회              |
| N+1 쿼리    | 의사 10명이면 일정 조회가 10번 발생               |
| 캐시 없음   | 동일 증상·진료과·의사 정보를 매번 DB에서 로드     |
| 동기 블로킹 | FastAPI에서 ChromaDB 동기 호출이 이벤트 루프 차단 |
| SSR 대기    | 전체 완료 전까지 사용자는 빈 화면                 |

---

## 2. 개선 전략 개요

### 적용 우선순위

| 우선순위 | 전략                       | 기대 효과                |
| -------- | -------------------------- | ------------------------ |
| ①        | HTMX 스켈레톤 즉시 반환    | 체감 첫 응답 ~50ms       |
| ②        | MySQL JOIN + 인덱스        | 실측 DB 시간 ~80% 감소   |
| ③        | Redis 다층 캐시            | 반복 요청 90%+ 절감      |
| ④        | @Async 병렬 처리           | 직렬 합산 → 최장 시간만  |
| ⑤        | FastAPI asyncio + pipeline | 이벤트 루프 최적화       |
| ⑥        | ChromaDB HNSW 튜닝         | 검색 속도 3~5배          |
| ⑦        | Prompt Caching             | LLM 비용 87% + 속도 4.8x |

---

## 3. 아키텍처 설계

```
Browser (Mustache + HTMX)
    │  POST /diagnosis/start → 스켈레톤 즉시 반환
    │  GET  /diagnosis/poll/{jobId} → 800ms polling
    ↓
Spring Boot (SSR 오케스트레이터)
    │  @Async / CompletableFuture / Spring Cache
    ├─────────────────┬────────────────────┐
    ↓                 ↓                    ↓
FastAPI           Redis               MySQL
async LLM       다층 캐시 레이어     JOIN 쿼리 + 인덱스
+ ChromaDB      dept / doctors       IN 절 단일 쿼리
                / schedules
    │
    ├── ChromaDB  (HNSW ef=50, asyncio.to_thread)
    └── LLM API   (claude-haiku + Prompt Caching)
```

### FastAPI ↔ Spring Boot 통신 (WebClient)

```java
// FastApiClient.java
@Component
public class FastApiClient {

    private final WebClient webClient;

    public FastApiClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
            .responseTimeout(Duration.ofSeconds(5))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(5))
                .addHandlerLast(new WriteTimeoutHandler(2)));

        this.webClient = builder
            .baseUrl("http://fastapi:8000")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public Department getDepartment(String symptoms) {
        return webClient.post()
            .uri("/diagnosis/department")
            .bodyValue(Map.of("symptoms", symptoms))
            .retrieve()
            .bodyToMono(Department.class)
            .timeout(Duration.ofSeconds(4))
            .block();
    }
}
```

---

## 4. Spring Boot — HTMX 스켈레톤 + @Async 병렬처리

### 4-1. HTMX polling 패턴 (SSR 체감 속도 개선)

SSR Mustache에서 스트리밍은 불가능하지만,
**"스켈레톤 즉시 반환 → HTMX polling → 결과 교체"** 패턴으로 체감 속도를 크게 개선할 수 있다.

```html
<!-- symptom_form.mustache -->
<form hx-post="/diagnosis/start" hx-target="#result-area" hx-swap="innerHTML">
  <input name="symptoms" placeholder="증상을 입력하세요" />
  <button type="submit">진단 요청</button>
</form>

<div id="result-area"></div>
```

```html
<!-- diagnosis_loading.mustache (즉시 반환 ~50ms) -->
<div
  id="result-area"
  hx-get="/diagnosis/poll/{{jobId}}"
  hx-trigger="every 800ms"
  hx-target="#result-area"
  hx-swap="outerHTML"
>
  <!-- 스켈레톤 UI -->
  <div class="skeleton skeleton-dept"></div>
  <div class="skeleton skeleton-doctor"></div>
  <div class="skeleton skeleton-schedule"></div>
</div>
```

### 4-2. Controller

```java
// DiagnosisController.java
@Controller
public class DiagnosisController {

    @PostMapping("/diagnosis/start")
    public String startDiagnosis(@RequestParam String symptoms, Model model) {
        // 즉시 jobId 발급 후 비동기 처리 시작
        String jobId = diagnosisService.submitAsync(symptoms);
        model.addAttribute("jobId", jobId);
        return "diagnosis_loading"; // 스켈레톤 템플릿 즉시 반환
    }

    @GetMapping("/diagnosis/poll/{jobId}")
    public String pollResult(@PathVariable String jobId, Model model) {
        DiagnosisResult result = diagnosisService.getResult(jobId);

        if (result == null) {
            model.addAttribute("jobId", jobId);
            return "diagnosis_loading"; // 처리 중
        }

        model.addAttribute("result", result);
        return "diagnosis_result"; // 완료 시 결과 반환
    }
}
```

### 4-3. @Async 병렬 처리

```java
// DiagnosisService.java
@Service
public class DiagnosisService {

    private final Map<String, DiagnosisResult> resultStore = new ConcurrentHashMap<>();

    @Async("diagnosisExecutor")
    public void processAsync(String jobId, String symptoms) {
        try {
            // ① FastAPI 호출 (RAG → 진료과 추출)
            Department dept = fastApiClient.getDepartment(symptoms);

            // ② 진료과 확정 후 의사 + Redis 캐시 병렬 조회
            CompletableFuture<List<Doctor>> doctorsFuture =
                CompletableFuture.supplyAsync(() -> doctorQueryService.findByDept(dept.getId()));

            CompletableFuture<List<Doctor>> cachedFuture =
                CompletableFuture.supplyAsync(() -> cacheService.getCachedDoctors(dept.getId()));

            // 캐시 우선, 없으면 DB 결과 사용
            List<Doctor> doctors = cachedFuture.get() != null
                ? cachedFuture.get()
                : doctorsFuture.get();

            // ③ 의사 목록으로 일정 조회 (IN 절 단일 쿼리)
            List<Schedule> schedules = scheduleRepository.findByDoctorIds(
                doctors.stream().map(Doctor::getId).collect(toList())
            );

            resultStore.put(jobId, new DiagnosisResult(dept, doctors, schedules));
        } catch (Exception e) {
            resultStore.put(jobId, DiagnosisResult.error(e.getMessage()));
        }
    }
}
```

### 4-4. ThreadPool 설정

```java
// AsyncConfig.java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "diagnosisExecutor")
    public ThreadPoolTaskExecutor diagnosisExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(10);
        exec.setMaxPoolSize(30);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("diagnosis-");
        exec.initialize();
        return exec;
    }
}
```

---

## 5. MySQL — N+1 제거 + 인덱스 최적화

### 5-1. N+1 쿼리 제거

```java
// ScheduleRepository.java
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // 의사 수만큼 반복 쿼리 → 단일 IN 쿼리로 교체
    @Query("""
        SELECT d, s FROM Doctor d
        LEFT JOIN FETCH s ON s.doctor = d
        WHERE d.id IN :doctorIds
          AND s.date BETWEEN :startDate AND :endDate
          AND s.available = true
        ORDER BY d.id, s.date
    """)
    List<Object[]> findDoctorsWithSchedules(
        @Param("doctorIds") List<Long> doctorIds,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
```

### 5-2. 인덱스 추가

```sql
-- 진료과별 의사 조회
CREATE INDEX idx_doctor_dept ON doctor(department_id);

-- 의사별 일정 조회 (복합 인덱스 순서 중요)
CREATE INDEX idx_schedule_doctor_date ON schedule(doctor_id, date, available);

-- 날짜 범위 검색용
CREATE INDEX idx_schedule_available ON schedule(available, date);
```

### 5-3. HikariCP 커넥션 풀 설정

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20 # 기본 10 → 상향
      minimum-idle: 5
      connection-timeout: 2000 # 2초 초과 시 빠른 실패
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## 6. Redis — 3계층 캐시 전략

### 6-1. 캐시 TTL 설계

| 캐시 대상     | 키 패턴                       | TTL    | 이유                  |
| ------------- | ----------------------------- | ------ | --------------------- |
| 진료과 목록   | `dept:{id}`                   | 24시간 | 거의 변경 없음        |
| 의사 목록     | `dept:{id}:doctors`           | 1시간  | 인사 변경 반영        |
| 의사 일정     | `doctor:{id}:schedule:{week}` | 5분    | 실시간 예약 반영      |
| RAG 결과      | `rag:{symptom_hash}`          | 30분   | 동일 증상 재처리 방지 |
| ChromaDB 검색 | `chroma:{symptom_hash}`       | 24시간 | 임베딩 결과 재사용    |

### 6-2. Spring Cache 설정

```java
// CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> configs = Map.of(
            "doctors",    ttl(Duration.ofHours(1)),
            "schedules",  ttl(Duration.ofMinutes(5)),
            "rag-result", ttl(Duration.ofMinutes(30))
        );
        return RedisCacheManager.builder(factory)
            .withInitialCacheConfigurations(configs)
            .build();
    }

    private RedisCacheConfiguration ttl(Duration d) {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(d)
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())
            );
    }
}
```

```java
// DoctorQueryService.java
@Service
public class DoctorQueryService {

    @Cacheable(value = "doctors", key = "#deptId", unless = "#result.isEmpty()")
    public List<Doctor> findByDept(Long deptId) {
        return doctorRepository.findByDepartmentIdWithSchedules(deptId);
    }

    @Cacheable(value = "schedules", key = "#doctorId + ':' + #week")
    public List<Schedule> findSchedules(Long doctorId, String week) {
        return scheduleRepository.findByDoctorIdAndWeek(doctorId, week);
    }
}
```

---

## 7. FastAPI — asyncio 완전 병렬화

### 7-1. 프로젝트 구조

```
fastapi_app/
├── main.py
├── core/
│   ├── redis_client.py      # aioredis 싱글턴 커넥션 풀
│   ├── db.py                # aiomysql 커넥션 풀
│   └── chroma_client.py     # ChromaDB 클라이언트
├── services/
│   ├── cache_service.py     # Redis CRUD (3계층)
│   ├── chroma_service.py    # 벡터 검색 + 캐시
│   ├── llm_service.py       # Anthropic API + Prompt Caching
│   └── diagnosis_service.py # asyncio.gather 오케스트레이션
└── routers/
    └── diagnosis.py
```

### 7-2. 의존성

```toml
# pyproject.toml
[tool.poetry.dependencies]
fastapi    = "^0.115"
uvicorn    = {extras = ["standard"], version = "^0.30"}
aioredis   = "^2.0"     # 비동기 Redis
aiomysql   = "^0.2"     # 비동기 MySQL
httpx      = "^0.27"    # 비동기 HTTP
anthropic  = "^0.34"
chromadb   = "^0.5"
orjson     = "^3.10"    # 빠른 JSON 직렬화
```

### 7-3. Redis 클라이언트 — 싱글턴 커넥션 풀

```python
# core/redis_client.py
import aioredis
from typing import Optional
import orjson

redis_pool: Optional[aioredis.Redis] = None

async def get_redis() -> aioredis.Redis:
    global redis_pool
    if redis_pool is None:
        redis_pool = await aioredis.from_url(
            "redis://localhost:6379",
            encoding="utf-8",
            decode_responses=True,
            max_connections=20,
            socket_connect_timeout=1,
            socket_timeout=2,
        )
    return redis_pool

async def cache_get(redis: aioredis.Redis, key: str) -> Optional[dict]:
    raw = await redis.get(key)
    return orjson.loads(raw) if raw else None

async def cache_set(redis: aioredis.Redis, key: str, value: dict, ttl: int):
    await redis.setex(key, ttl, orjson.dumps(value).decode())
```

### 7-4. Cache Service — Redis Pipeline 활용

```python
# services/cache_service.py
from core.redis_client import cache_get, cache_set, get_redis
import asyncio, orjson

TTL = {
    "department": 86400,   # 24시간
    "doctors":    3600,    # 1시간
    "schedule":   300,     # 5분
    "rag":        1800,    # 30분
}

class CacheService:

    async def get_doctors(self, dept_id: int) -> Optional[list]:
        r = await get_redis()
        return await cache_get(r, f"dept:{dept_id}:doctors")

    async def set_doctors(self, dept_id: int, doctors: list):
        r = await get_redis()
        await cache_set(r, f"dept:{dept_id}:doctors", doctors, TTL["doctors"])

    # ── Pipeline으로 N번 왕복 → 1번 왕복 ──────────────
    async def get_schedules_bulk(self, doctor_ids: list[int], week: str) -> dict:
        r = await get_redis()
        keys = [f"doctor:{did}:schedule:{week}" for did in doctor_ids]

        async with r.pipeline(transaction=False) as pipe:
            for key in keys:
                pipe.get(key)
            results = await pipe.execute()

        return {
            did: orjson.loads(raw) if raw else None
            for did, raw in zip(doctor_ids, results)
        }

    async def set_schedules_bulk(self, schedules_map: dict, week: str):
        r = await get_redis()
        async with r.pipeline(transaction=False) as pipe:
            for doctor_id, schedules in schedules_map.items():
                key = f"doctor:{doctor_id}:schedule:{week}"
                pipe.setex(key, TTL["schedule"], orjson.dumps(schedules).decode())
            await pipe.execute()
```

### 7-5. Diagnosis Service — asyncio 오케스트레이션

```python
# services/diagnosis_service.py
import asyncio, hashlib
from datetime import date, timedelta

class DiagnosisService:

    def __init__(self, cache, chroma, llm, db):
        self.cache = cache
        self.chroma = chroma
        self.llm = llm
        self.db = db

    async def diagnose(self, symptoms: str) -> dict:
        # ① RAG → 진료과 (내부 캐시 포함)
        department = await self._get_department_from_rag(symptoms)

        # ② 의사 목록 조회
        doctors = await self._get_doctors(department["id"])
        week    = self._current_week()

        # ③ 전체 의사 일정 bulk 조회 (pipeline)
        schedules_map = await self._get_schedules_bulk(
            [d["id"] for d in doctors], week
        )

        return {
            "department": department,
            "doctors":    doctors,
            "schedules":  schedules_map,
        }

    # ── RAG 파이프라인 ────────────────────────────────
    async def _get_department_from_rag(self, symptoms: str) -> dict:
        cache_key = hashlib.md5(symptoms.encode()).hexdigest()
        r = await get_redis()

        cached = await cache_get(r, f"rag:{cache_key}")
        if cached:
            return cached  # ~5ms

        # ChromaDB 검색 (동기 라이브러리 → to_thread로 블로킹 방지)
        search_result = await asyncio.to_thread(
            self.chroma.search_sync, symptoms, n_results=3
        )

        # LLM 분류
        dept, stats = await self.llm.classify_with_caching(symptoms, search_result)

        # fire and forget — 응답 반환을 캐시 저장이 막지 않음
        asyncio.create_task(
            cache_set(r, f"rag:{cache_key}", dept, TTL["rag"])
        )

        return dept

    # ── 의사 목록 ────────────────────────────────────
    async def _get_doctors(self, dept_id: int) -> list:
        cached = await self.cache.get_doctors(dept_id)
        if cached:
            return cached

        doctors = await self.db.fetch_doctors_by_dept(dept_id)

        # fire and forget
        asyncio.create_task(self.cache.set_doctors(dept_id, doctors))
        return doctors

    # ── 일정 Partial Cache Hit ────────────────────────
    async def _get_schedules_bulk(self, doctor_ids: list[int], week: str) -> dict:
        # 1. Pipeline으로 전체 확인 (1번 왕복)
        cached_map = await self.cache.get_schedules_bulk(doctor_ids, week)

        # 2. 캐시 미스된 의사만 추출
        miss_ids = [did for did, v in cached_map.items() if v is None]

        if miss_ids:
            # 3. 미스된 의사들만 DB 조회
            db_results = await self.db.fetch_schedules_by_doctor_ids(miss_ids, week)

            # 4. 캐시 저장 (fire and forget)
            asyncio.create_task(
                self.cache.set_schedules_bulk(db_results, week)
            )

            cached_map.update(db_results)

        return cached_map

    def _current_week(self) -> str:
        today  = date.today()
        monday = today - timedelta(days=today.weekday())
        return monday.isoformat()
```

### 7-6. aiomysql — 비동기 DB 풀

```python
# core/db.py
import aiomysql
from typing import Optional

_pool: Optional[aiomysql.Pool] = None

async def get_pool() -> aiomysql.Pool:
    global _pool
    if _pool is None:
        _pool = await aiomysql.create_pool(
            host="localhost", port=3306,
            user="app", password="secret", db="hospital",
            minsize=5, maxsize=20,
            autocommit=True, charset="utf8mb4",
        )
    return _pool

# services/db_service.py
class DBService:

    async def fetch_doctors_by_dept(self, dept_id: int) -> list:
        pool = await get_pool()
        async with pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cur:
                await cur.execute("""
                    SELECT d.id, d.name, d.specialty, d.profile_image
                    FROM doctor d
                    WHERE d.department_id = %s AND d.active = 1
                    ORDER BY d.name
                """, (dept_id,))
                return await cur.fetchall()

    async def fetch_schedules_by_doctor_ids(
        self, doctor_ids: list[int], week: str
    ) -> dict:
        if not doctor_ids:
            return {}

        pool = await get_pool()
        placeholders = ",".join(["%s"] * len(doctor_ids))

        async with pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cur:
                # IN 절 단일 쿼리 — N+1 완전 제거
                await cur.execute(f"""
                    SELECT s.doctor_id, s.date, s.time_slot, s.available
                    FROM schedule s
                    WHERE s.doctor_id IN ({placeholders})
                      AND s.date >= %s
                      AND s.date < DATE_ADD(%s, INTERVAL 7 DAY)
                      AND s.available = 1
                    ORDER BY s.doctor_id, s.date, s.time_slot
                """, (*doctor_ids, week, week))
                rows = await cur.fetchall()

        # doctor_id 기준 그룹핑
        result = {}
        for row in rows:
            did = row["doctor_id"]
            result.setdefault(did, []).append({
                "date":      str(row["date"]),
                "time_slot": str(row["time_slot"]),
            })
        return result
```

### 7-7. 앱 진입점 — lifespan 워밍업

```python
# main.py
from contextlib import asynccontextmanager
from fastapi import FastAPI

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 시작 시 커넥션 풀 워밍업 (첫 요청 지연 방지)
    await get_redis()
    await get_pool()
    yield
    # 종료 시 정리
    if _pool:
        _pool.close()
        await _pool.wait_closed()

app = FastAPI(lifespan=lifespan)
app.include_router(diagnosis_router)
```

### 7-8. asyncio 핵심 패턴 요약

| 패턴                    | 코드                          | 효과                           |
| ----------------------- | ----------------------------- | ------------------------------ |
| `asyncio.gather()`      | 독립 작업 동시 실행           | 직렬 합산 시간 → 최장 시간만   |
| `asyncio.create_task()` | 백그라운드 실행               | 캐시 저장을 응답에서 분리      |
| `asyncio.to_thread()`   | 동기 라이브러리 논블로킹 처리 | ChromaDB 이벤트 루프 차단 방지 |
| Redis `pipeline()`      | N번 왕복 → 1번 왕복           | 의사 10명 일정: 10ms → 1ms     |
| Partial Cache Hit       | 미스된 ID만 DB 조회           | 캐시 워밍 후 DB 부하 최소화    |

---

## 8. ChromaDB 튜닝

### 8-1. HNSW 파라미터 이해

| 파라미터           | 역할                 | 권장값 | 트레이드오프              |
| ------------------ | -------------------- | ------ | ------------------------- |
| `M`                | 노드당 연결 수       | 16     | 높을수록 정확도↑ 메모리↑  |
| `ef_construction`  | 인덱스 빌드 품질     | 200    | 빌드 시 1회만 → 높게 설정 |
| `hnsw:search_ef`   | 검색 시 탐색 후보 수 | **50** | 핵심 튜닝 파라미터        |
| `hnsw:space`       | 거리 함수            | cosine | 텍스트 유사도에 적합      |
| `hnsw:num_threads` | CPU 병렬 스레드      | 4      | CPU 코어 수에 맞게 조정   |

### 8-2. 컬렉션 초기화

```python
# core/chroma_client.py
import chromadb
from chromadb.config import Settings
from chromadb.utils import embedding_functions

chroma_client = chromadb.HttpClient(
    host="localhost",
    port=8001,
    settings=Settings(anonymized_telemetry=False),
)

# 한국어 의료 특화 임베딩 모델
embedding_fn = embedding_functions.SentenceTransformerEmbeddingFunction(
    model_name="intfloat/multilingual-e5-large",
    device="cpu",               # GPU 있으면 "cuda"
    normalize_embeddings=True,
)

def get_collection():
    return chroma_client.get_or_create_collection(
        name="medical_symptoms",
        embedding_function=embedding_fn,
        metadata={
            "hnsw:space":            "cosine",
            "hnsw:M":                16,
            "hnsw:construction_ef":  200,
            "hnsw:search_ef":        50,   # 검색 속도 핵심
            "hnsw:num_threads":      4,
        }
    )

collection = get_collection()
```

### 8-3. 데이터 적재 — 배치 upsert

```python
# scripts/load_medical_data.py
async def load_symptoms_bulk(symptoms_data: list[dict]):
    BATCH_SIZE = 100   # ChromaDB 권장 배치 크기

    for i in range(0, len(symptoms_data), BATCH_SIZE):
        batch = symptoms_data[i:i + BATCH_SIZE]

        collection.upsert(
            ids       = [str(item["id"]) for item in batch],
            documents = [item["text"] for item in batch],
            metadatas = [{
                "department_id":   item["dept_id"],
                "department_name": item["dept_name"],
                "symptom_tags":    ",".join(item["tags"]),
                "severity":        item["severity"],   # mild|moderate|severe
            } for item in batch],
        )
```

### 8-4. 검색 — 유사도 임계값 + where 필터

```python
# services/chroma_service.py
import asyncio, time
from typing import Optional

class ChromaService:

    def __init__(self, collection):
        self.collection = collection

    def _search_sync(
        self,
        query_text: str,
        n_results: int = 3,
        where: Optional[dict] = None,
    ) -> list[dict]:
        result = self.collection.query(
            query_texts = [query_text],
            n_results   = n_results,
            where       = where,
            include     = ["documents", "metadatas", "distances"],
        )
        return self._parse(result)

    async def search(
        self,
        query: str,
        n_results: int = 3,
        severity_filter: Optional[str] = None,
    ) -> list[dict]:
        where = {"severity": {"$eq": severity_filter}} if severity_filter else None

        t0 = time.perf_counter()
        results = await asyncio.to_thread(
            self._search_sync, query, n_results, where
        )
        print(f"[ChromaDB] {(time.perf_counter()-t0)*1000:.1f}ms, hits={len(results)}")

        return results

    def _parse(self, raw: dict) -> list[dict]:
        return [
            {
                "document":        doc,
                "department_id":   meta["department_id"],
                "department_name": meta["department_name"],
                "similarity":      round(1 - dist, 4),  # cosine → similarity
            }
            for doc, meta, dist in zip(
                raw["documents"][0],
                raw["metadatas"][0],
                raw["distances"][0],
            )
            if (1 - dist) >= 0.65   # 유사도 임계값 필터
        ]

    # ── 임베딩 결과 Redis 캐시 ────────────────────────
    async def search_with_cache(
        self, query: str, redis, n_results: int = 3
    ) -> list[dict]:
        import hashlib, orjson

        cache_key = f"chroma:{hashlib.md5(query.encode()).hexdigest()}"
        cached = await redis.get(cache_key)
        if cached:
            return orjson.loads(cached)

        results = await self.search(query, n_results)

        # 동일 증상 텍스트 → 동일 결과 → 24시간 캐시
        await redis.setex(cache_key, 86400, orjson.dumps(results).decode())
        return results
```

### 8-5. ef_search 벤치마크

```python
# scripts/benchmark_chroma.py
import time, statistics

TEST_QUERIES = [
    "두통이 심하고 메스껍고 빛이 싫어요",
    "가슴이 두근거리고 숨이 차요",
    "무릎이 붓고 계단 내려갈 때 아파요",
    "소화가 안 되고 명치가 콕콕 찔려요",
    "눈이 충혈되고 분비물이 나와요",
]
GROUND_TRUTH = [3, 1, 9, 2, 10]  # 정답 진료과 ID

def run_benchmark(ef_search: int, n_runs: int = 20):
    collection.modify(metadata={"hnsw:search_ef": ef_search})

    latencies, correct = [], 0

    for query, truth in zip(TEST_QUERIES, GROUND_TRUTH):
        times, result_dept = [], None
        for _ in range(n_runs):
            t0 = time.perf_counter()
            r  = collection.query(query_texts=[query], n_results=1,
                                  include=["metadatas"])
            times.append((time.perf_counter() - t0) * 1000)
            result_dept = r["metadatas"][0][0]["department_id"]
        latencies.extend(times)
        if result_dept == truth:
            correct += 1

    return {
        "ef_search": ef_search,
        "p50_ms":    round(statistics.median(latencies), 1),
        "p95_ms":    round(statistics.quantiles(latencies, n=20)[18], 1),
        "accuracy":  f"{correct}/{len(TEST_QUERIES)}",
    }

if __name__ == "__main__":
    print(f"{'ef':>6} | {'p50(ms)':>8} | {'p95(ms)':>8} | 정확도")
    print("-" * 42)
    for ef in [10, 20, 50, 100, 200]:
        r = run_benchmark(ef)
        print(f"{r['ef_search']:>6} | {r['p50_ms']:>8} | {r['p95_ms']:>8} | {r['accuracy']}")
```

**벤치마크 예상 결과:**

```
    ef |  p50(ms) |  p95(ms) | 정확도
------------------------------------------
    10 |      8.2 |     14.1 | 3/5   ← 빠르지만 부정확
    20 |     12.4 |     19.3 | 4/5
    50 |     22.1 |     31.8 | 5/5   ← 권장 (균형)
   100 |     38.7 |     55.2 | 5/5
   200 |     72.4 |    101.3 | 5/5   ← 정확하지만 느림
```

---

## 9. Anthropic Prompt Caching 측정

### 9-1. 동작 원리

- **조건**: 시스템 프롬프트가 **1024 토큰 이상**이어야 캐시 활성화
- **TTL**: **5분** (5분 이내 동일 프롬프트 재호출 시 캐시 적중)
- **cache miss**: 기본 비용 × **1.25** (25% 추가)
- **cache hit**: 기본 비용 × **0.10** (90% 절감)

```
첫 번째 요청 (cache miss)
  [system: 2000 토큰 ████████████] [user: 변수 토큰]
       → 전체 처리 후 캐시 저장
       → cache_creation_input_tokens: 2000
       → 비용: 기본 × 1.25 / 속도: ~1,500ms

이후 요청 (cache hit)
  [system: ✓캐시 HIT, 스킵] [user: 변수 토큰]
       → user 토큰만 처리
       → cache_read_input_tokens: 2000
       → 비용: 기본 × 0.10 / 속도: ~300ms
```

### 9-2. LLM Service — Prompt Caching 적용

```python
# services/llm_service.py
from anthropic import AsyncAnthropic
import time, orjson

client = AsyncAnthropic()

# 1024 토큰 이상 확보 — 의료 지식을 충분히 담을 것
SYSTEM_PROMPT = """
당신은 15년 경력의 의료 AI 어시스턴트입니다.
환자가 입력한 증상을 분석하여 가장 적합한 진료과를 추천합니다.

[진료과 분류 기준]
내과: 발열, 기침, 감기, 소화불량, 복통, 설사, 당뇨, 고혈압, 피로감, 체중감소, 빈혈
외과: 복부 통증(맹장 의심), 탈장, 상처, 종양, 외상, 수술 후 관리
신경과: 두통, 편두통, 어지럼증, 기억력 저하, 손발 저림, 뇌졸중 의심
정형외과: 관절통, 허리통증, 골절, 인대 손상, 근육통, 척추측만증
심장내과: 가슴 통증, 두근거림, 호흡곤란, 부정맥, 심부전
피부과: 발진, 두드러기, 여드름, 탈모, 건선, 아토피
안과: 시력저하, 충혈, 눈곱, 안구건조, 날파리증
이비인후과: 귀 통증, 난청, 코막힘, 비염, 편도염
정신건강의학과: 우울, 불안, 불면, 공황장애, ADHD
비뇨의학과: 배뇨 이상, 혈뇨, 요로감염, 전립선 문제

[응답 규칙]
1. 반드시 JSON 형식으로만 응답
2. 추측이 어려우면 "내과"를 기본값으로
3. 응급 증상(흉통+호흡곤란)은 emergency: true 포함

[응답 형식]
{"department_id": <int>, "department_name": "<str>",
 "confidence": <0.0~1.0>, "reason": "<50자 이내>", "emergency": <bool>}
"""

class LLMService:

    async def classify_with_caching(
        self, symptoms: str, rag_docs: list
    ) -> tuple[dict, dict]:
        t0 = time.perf_counter()

        response = await client.messages.create(
            model      = "claude-haiku-4-5-20251001",
            max_tokens = 256,
            system     = [
                {
                    "type": "text",
                    "text": SYSTEM_PROMPT,
                    "cache_control": {"type": "ephemeral"},  # 캐시 경계선
                }
            ],
            messages   = [{
                "role":    "user",
                "content": f"증상: {symptoms}\n\n참고 문서:\n{self._format_rag(rag_docs)}",
            }],
        )

        elapsed_ms = (time.perf_counter() - t0) * 1000
        usage      = response.usage

        stats = {
            "elapsed_ms":                    round(elapsed_ms, 1),
            "cache_creation_input_tokens":   getattr(usage, "cache_creation_input_tokens", 0),
            "cache_read_input_tokens":       getattr(usage, "cache_read_input_tokens", 0),
            "input_tokens":                  usage.input_tokens,
            "output_tokens":                 usage.output_tokens,
            "cache_hit":                     getattr(usage, "cache_read_input_tokens", 0) > 0,
        }

        result = orjson.loads(response.content[0].text)
        return result, stats

    def _format_rag(self, docs: list) -> str:
        return "\n".join(
            f"- {d['document']} (유사도: {d['similarity']:.2f})"
            for d in docs
        )
```

### 9-3. 실시간 메트릭 수집

```python
# middleware/llm_metrics.py
import asyncio, statistics
from collections import deque
from dataclasses import dataclass
from datetime import datetime

@dataclass
class CallRecord:
    timestamp:              datetime
    elapsed_ms:             float
    cache_hit:              bool
    cache_creation_tokens:  int
    cache_read_tokens:      int
    input_tokens:           int
    output_tokens:          int

    @property
    def cost_usd(self) -> float:
        # claude-haiku-4-5 기준
        input_cost  = self.input_tokens  * 0.80 / 1_000_000
        output_cost = self.output_tokens * 4.00 / 1_000_000
        create_cost = self.cache_creation_tokens * 1.00 / 1_000_000
        read_cost   = self.cache_read_tokens     * 0.08 / 1_000_000
        return input_cost + output_cost + create_cost + read_cost


class LLMMetrics:

    def __init__(self, window: int = 200):
        self._records: deque[CallRecord] = deque(maxlen=window)
        self._lock = asyncio.Lock()

    async def record(self, stats: dict):
        async with self._lock:
            self._records.append(CallRecord(
                timestamp             = datetime.now(),
                elapsed_ms            = stats["elapsed_ms"],
                cache_hit             = stats["cache_hit"],
                cache_creation_tokens = stats["cache_creation_input_tokens"],
                cache_read_tokens     = stats["cache_read_input_tokens"],
                input_tokens          = stats["input_tokens"],
                output_tokens         = stats["output_tokens"],
            ))

    async def report(self) -> dict:
        async with self._lock:
            records = list(self._records)

        if not records:
            return {"message": "데이터 없음"}

        hits   = [r for r in records if r.cache_hit]
        misses = [r for r in records if not r.cache_hit]
        hit_rate = len(hits) / len(records) * 100

        total_cost  = sum(r.cost_usd for r in records)
        nocache_cost = sum(
            (r.input_tokens + r.cache_read_tokens + r.cache_creation_tokens)
            * 0.80 / 1_000_000 + r.output_tokens * 4.00 / 1_000_000
            for r in records
        )

        return {
            "총 호출":          len(records),
            "캐시 히트율":       f"{hit_rate:.1f}%",
            "히트 p50 (ms)":    round(statistics.median([r.elapsed_ms for r in hits]), 1) if hits else "-",
            "미스 p50 (ms)":    round(statistics.median([r.elapsed_ms for r in misses]), 1) if misses else "-",
            "속도 개선":         (
                f"{statistics.median([r.elapsed_ms for r in misses]) / statistics.median([r.elapsed_ms for r in hits]):.1f}x"
                if hits and misses else "-"
            ),
            "실제 비용 ($)":     round(total_cost, 5),
            "캐시 없을 때 ($)":  round(nocache_cost, 5),
            "비용 절감율":        f"{(1 - total_cost/nocache_cost)*100:.1f}%" if nocache_cost else "-",
        }

metrics = LLMMetrics(window=200)
```

### 9-4. 측정 엔드포인트

```python
# routers/diagnosis.py
@router.post("/analyze")
async def analyze(symptoms: str):
    rag_docs      = await chroma_svc.search_with_cache(symptoms, redis_client)
    result, stats = await llm_svc.classify_with_caching(symptoms, rag_docs)

    # 메트릭 기록 — fire and forget
    asyncio.create_task(metrics.record(stats))

    return {"result": result, "debug": stats}

@router.get("/metrics")
async def get_metrics():
    return await metrics.report()
```

### 9-5. 측정 명령어 및 예상 결과

```bash
# 연속 요청으로 캐시 히트율 측정
for i in {1..20}; do
  curl -s -X POST "http://localhost:8000/diagnosis/analyze" \
    -d '{"symptoms": "두통이 심하고 메스껍습니다"}'
  sleep 0.5
done

curl http://localhost:8000/diagnosis/metrics
```

```json
{
  "총 호출": 20,
  "캐시 히트율": "90.0%",
  "히트 p50 (ms)": 312.4,
  "미스 p50 (ms)": 1487.2,
  "속도 개선": "4.8x",
  "실제 비용 ($)": 0.00041,
  "캐시 없을 때 ($)": 0.00318,
  "비용 절감율": "87.1%"
}
```

---

## 10. 최종 성능 요약

### 구간별 개선 효과

| 구간                | 개선 전 | 캐시 미스 | 캐시 히트 | 적용 기술                    |
| ------------------- | ------- | --------- | --------- | ---------------------------- |
| 체감 첫 응답        | ~9s     | **~50ms** | **~50ms** | HTMX 스켈레톤                |
| ChromaDB 검색       | 포함    | ~25ms     | ~2ms      | HNSW ef=50 + Redis           |
| LLM 분류            | ~3s     | ~1,500ms  | ~300ms    | Prompt Caching + Haiku       |
| DB 조회 (의사+일정) | ~4s     | ~800ms    | ~5ms      | 병렬 @Async + Redis Pipeline |
| FastAPI 전체        | 포함    | ~1,600ms  | ~310ms    | asyncio.gather               |
| **전체 완료**       | **~9s** | **~2.5s** | **~0.4s** | 복합 적용                    |

### 캐시 웜업 후 기대 히트율

| 캐시 종류             | 기대 히트율 | 이유                         |
| --------------------- | ----------- | ---------------------------- |
| Prompt Caching        | 90%+        | 시스템 프롬프트 항상 동일    |
| RAG 결과 (Redis)      | 70%+        | 자주 입력되는 증상 패턴 집중 |
| ChromaDB 검색         | 70%+        | 동일 증상 텍스트 재사용      |
| 의사 목록 (Redis)     | 95%+        | 1시간 TTL, 변경 드묾         |
| 일정 (Redis Pipeline) | 60%+        | 5분 TTL, 예약 시 갱신        |

### LLM 비용 절감 요약

| 항목           | 절감율   | 방법                     |
| -------------- | -------- | ------------------------ |
| 입력 토큰 비용 | **90%**  | Prompt Caching cache hit |
| LLM 호출 횟수  | **70%+** | RAG 결과 Redis 캐시      |
| 임베딩 처리    | **70%+** | ChromaDB 결과 Redis 캐시 |

---

> 이 가이드의 최적화를 모두 적용하면, 체감 응답은 **즉시(50ms)** 로 개선되며
> 실제 데이터 완료까지 **캐시 히트 시 ~0.4초, 미스 시 ~2.5초**로 단축됩니다.
