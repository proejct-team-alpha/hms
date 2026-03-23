# HMS 인프라 구성 및 실행 가이드

## 포트 목록

| 서비스                            | 포트  | 설명                             |
| --------------------------------- | ----- | -------------------------------- |
| **hms-db** (MySQL 8.0)            | 3306  | 메인 데이터베이스                |
| **hms-redis** (Redis 7)           | 6379  | 캐시/세션                        |
| **hms-chromadb** (ChromaDB 1.5.4) | 8100  | 벡터 DB (컨테이너 내부 8000)     |
| **hms-python** (FastAPI)          | 8000  | LLM Python 서비스                |
| **hms-spring** (Spring Boot)      | 8080  | 백엔드 API 서버                  |
| **vLLM**                          | 9000  | LLM 추론 서버 (호스트 직접 실행) |
| **Ollama**                        | 11434 | 임베딩 서버 (호스트 직접 실행)   |

---

## 1. Docker (docker-compose)

### DB 접속 정보

| 항목          | 값                                           |
| ------------- | -------------------------------------------- |
| Host          | localhost (호스트에서) / mysql (컨테이너 간) |
| Port          | 3306                                         |
| Database      | hms_db                                       |
| User          | hms_admin                                    |
| Password      | hms_password                                 |
| Root Password | rootpassword                                 |

### 실행

```bash

# 전체 서비스 기동
docker compose up -d

# 특정 서비스만 기동 (DB, Redis, ChromaDB만)
docker compose up -d mysql redis chromadb

# 상태 확인
docker ps

# 로그 확인
docker compose logs -f [서비스명]

# 전체 중지
docker compose down
```

### DDL 실행 (의학 데이터 테이블 생성)

```bash
docker exec -i hms-db mysql -uhms_admin -phms_password --default-character-set=utf8mb4 hms_db \
  < /mnt/c/workspace/hms/python-llm/sql/medical-tables.sql
```

---

## 2. Ollama (임베딩 서버)

### 설정

| 항목           | 값                                      |
| -------------- | --------------------------------------- |
| 바인드 주소    | 127.0.0.1:11434                         |
| 임베딩 모델    | nomic-embed-text                        |
| API 엔드포인트 | `POST http://localhost:11434/api/embed` |
| 버전           | 0.18.2                                  |

### 설치

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

### 모델 다운로드

```bash
ollama pull nomic-embed-text
```

### 실행

```bash
# 서비스 시작 (설치 시 자동 실행됨)
ollama serve

# 상태 확인
curl http://localhost:11434/api/version

# 모델 목록
ollama list

# 임베딩 테스트
curl http://localhost:11434/api/embed -d '{"model":"nomic-embed-text","input":"hello"}'
```

---

## 3. vLLM (LLM 추론 서버)

### 설정

| 항목                    | 값                                   |
| ----------------------- | ------------------------------------ |
| 포트                    | 9000                                 |
| 모델                    | Qwen/Qwen2.5-3B-Instruct             |
| served-model-name       | qwen2.5-3b                           |
| dtype                   | bfloat16                             |
| max-model-len           | 3072                                 |
| gpu-memory-utilization  | 0.85 (RTX 3060 Ti 8GB + WSL 환경)    |
| API -memory-utilization | 0.85 (RTX 3060 Ti 8GB + WSL 환경)    |
| API                     | OpenAI 호환 (`/v1/chat/completions`) |

### 가상환경

```bash
# vLLM 전용 가상환경 활성화
source /mnt/c/Users/G/vllm-env/bin/activate
```

### 실행

```bash
python -m vllm.entrypoints.openai.api_server \
  --model Qwen/Qwen2.5-3B-Instruct \
  --host 0.0.0.0 \
  --port 9000 \
  --dtype bfloat16 \
  --max-model-len 3072 \
  --served-model-name qwen2.5-3b \
  --gpu-memory-utilization 0.85
```

### 상태 확인

```bash
# 모델 목록
curl http://localhost:9000/v1/models

# 추론 테스트
curl http://localhost:9000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen2.5-7b","messages":[{"role":"user","content":"hello"}],"max_tokens":50}'
```

### GPU 메모리 관련 (RTX 3060 Ti 8GB + WSL)

- WSL의 Xwayland가 ~1GB를 점유하여 실제 가용 메모리는 약 6.94GB
- **기동 순서 중요:** Ollama 종료 → vLLM 기동 → Ollama 재시작
- 7B 모델은 8GB에서 실행 불가, 3B 모델 + `max-model-len 3072` 권장
- `--gpu-memory-utilization`은 0.85 이하로 설정 (0.9는 Xwayland 점유분 때문에 실패)

---

## 4. 데이터 적재 스크립트

### 의학 데이터 적재

```bash
cd /mnt/c/workspace/hms/python-llm
source .venv/bin/activate

# 의학 콘텐츠 + Q&A 적재
python import_medical_data.py

# 병원 규칙 적재 + ChromaDB 인덱싱 (Ollama 필요)
python index_rule_data.py
```

---

## 5. 서비스 기동 순서 (권장)

```
1. Docker (DB, Redis, ChromaDB)  →  docker compose up -d mysql redis chromadb
2. Ollama                         →  ollama serve (자동 실행 확인)
3. vLLM                           →  위 실행 명령어 참조
4. DDL 실행                       →  medical-tables.sql 실행
5. 데이터 적재                    →  import_medical_data.py, index_rule_data.py
6. hms-python (Docker)            →  docker compose up -d python-llm
7. hms-spring (Docker)            →  docker compose up -d spring-app
```

> **주의:** vLLM과 hms-python(Docker)은 기본 포트가 8000으로 동일합니다.
> vLLM은 반드시 `--port 9000`으로 기동하고, docker-compose의 `VLLM_BASE_URL`을 확인하세요.
