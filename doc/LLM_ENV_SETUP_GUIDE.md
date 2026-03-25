# HMS 프로젝트 — LLM 로컬 환경 구축 가이드

> Windows 11 환경에서 WSL2, Docker, Ollama, vLLM을 설치하고, HMS Python LLM 서버를 로컬에서 구동하는 전체 절차

---

## 목차

1. [사전 요구사항](#1-사전-요구사항)
2. [WSL2 설치 및 설정](#2-wsl2-설치-및-설정)
3. [Docker Desktop 설치](#3-docker-desktop-설치)
4. [Ollama 설치 (개발용 LLM 백엔드)](#4-ollama-설치-개발용-llm-백엔드)
5. [vLLM 설치 (운영용 LLM 백엔드)](#5-vllm-설치-운영용-llm-백엔드)
6. [ChromaDB 구동 (벡터 검색)](#6-chromadb-구동-벡터-검색)
7. [Python LLM 서버 로컬 실행](#7-python-llm-서버-로컬-실행)
8. [Docker Compose 풀스택 구동](#8-docker-compose-풀스택-구동)
9. [데이터 인덱싱](#9-데이터-인덱싱)
10. [검증 및 테스트](#10-검증-및-테스트)
11. [환경변수 레퍼런스](#11-환경변수-레퍼런스)
12. [트러블슈팅](#12-트러블슈팅)

---

## 1. 사전 요구사항

### 하드웨어 권장 사양

| 항목         | 최소            | 권장             |
| ------------ | --------------- | ---------------- |
| RAM          | 16GB            | 32GB             |
| 디스크 여유  | 30GB            | 50GB             |
| GPU (vLLM용) | NVIDIA 6GB VRAM | NVIDIA 8GB+ VRAM |
| CPU          | 4코어           | 8코어+           |

> GPU가 없으면 Ollama CPU 모드로 구동 가능 (추론 속도 저하)
> vLLM은 NVIDIA GPU + CUDA 필수

### 소프트웨어 버전

| 소프트웨어     | 버전                                           |
| -------------- | ---------------------------------------------- |
| Windows        | 11 (빌드 22000+) 또는 Windows 10 (빌드 19041+) |
| WSL2           | 최신 커널                                      |
| Docker Desktop | 4.25+                                          |
| Python         | 3.11                                           |
| Ollama         | 최신                                           |
| NVIDIA Driver  | 535+ (vLLM 사용 시)                            |
| CUDA Toolkit   | 12.1+ (vLLM 사용 시)                           |

---

## 2. WSL2 설치 및 설정

### 2.1 WSL2 활성화

PowerShell (관리자 권한)에서 실행:

```powershell
# WSL 설치 (Ubuntu가 기본 배포판으로 설치됨)
wsl --install

# 재부팅 후 WSL 버전 확인
wsl --version

# Ubuntu가 WSL2로 실행되는지 확인
wsl -l -v
```

출력 예시:

```
  NAME      STATE           VERSION
* Ubuntu    Running         2
```

VERSION이 `1`이면 변환:

```powershell
wsl --set-version Ubuntu 2
```

### 2.2 WSL2 리소스 설정

`C:\Users\<사용자명>\.wslconfig` 파일 생성:

```ini
[wsl2]
memory=8GB
processors=4
swap=4GB
localhostForwarding=true
```

> `memory`는 전체 RAM의 절반 이하 권장. Ollama + Docker가 동시에 구동되므로 최소 8GB 할당.

설정 적용:

```powershell
wsl --shutdown
wsl
```

### 2.3 WSL2 Ubuntu 기본 패키지

WSL Ubuntu 터미널에서:

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl wget git build-essential python3.11 python3.11-venv python3-pip
```

---

## 3. Docker Engine 설치 (WSL2 Ubuntu)

> Docker Desktop(Windows GUI) 대신 **WSL2 Ubuntu 내부에 Docker Engine을 직접 설치**합니다.
> 라이선스 제약이 없고, WSL2 내에서 네이티브 리눅스 Docker로 동작하여 성능과 호환성이 우수합니다.

### 3.1 기존 Docker 제거 (충돌 방지)

WSL Ubuntu 터미널에서:

```bash
# 기존 설치가 있으면 제거
sudo apt remove -y docker docker-engine docker.io containerd runc 2>/dev/null
```

> Windows에 Docker Desktop이 설치되어 있다면, WSL Integration을 **OFF**로 설정하거나 Docker Desktop을 제거하세요. 두 Docker가 동시에 동작하면 소켓 충돌이 발생합니다.

### 3.2 Docker 공식 저장소 추가 및 설치

```bash
# 필수 패키지 설치
sudo apt update
sudo apt install -y ca-certificates curl gnupg lsb-release

# Docker 공식 GPG 키 추가
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Docker 저장소 추가
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker Engine + Docker Compose 플러그인 설치
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

### 3.3 Docker 서비스 시작

WSL2에서는 systemd가 기본 활성화되어 있습니다 (Ubuntu 22.04+):

```bash
# Docker 서비스 시작
sudo service docker start

# 부팅 시 자동 시작 (systemd 환경)
sudo systemctl enable docker
```

> systemd가 비활성화된 WSL2라면 `sudo service docker start`를 WSL 시작 시마다 실행해야 합니다.
> `/etc/wsl.conf`에서 systemd를 활성화할 수 있습니다:
>
> ```ini
> [boot]
> systemd=true
> ```
>
> 설정 후 `wsl --shutdown` → WSL 재시작

### 3.4 현재 사용자를 docker 그룹에 추가 (sudo 없이 사용)

```bash
sudo usermod -aG docker $USER

# 그룹 변경 적용 (재로그인 또는)
newgrp docker
```

### 3.5 설치 확인

```bash
# 버전 확인
docker --version
docker compose version

# 테스트 실행
docker run --rm hello-world
```

정상 출력:

```
Hello from Docker!
This message shows that your installation appears to be working correctly.
```

### 3.6 Docker 데이터 경로 설정 (선택)

WSL2의 기본 디스크 공간이 부족하면 Docker 데이터 경로를 변경합니다:

```bash
# Docker 데이터 사용량 확인
docker system df

# 데이터 경로 변경 시 /etc/docker/daemon.json 생성
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<EOF
{
  "data-root": "/mnt/d/docker-data"
}
EOF

sudo service docker restart
```

---

## 4. Ollama 설치 (개발용 LLM 백엔드)

HMS 프로젝트에서 Ollama는 **개발 환경용 LLM 백엔드**로 사용됩니다.

### 4.1 WSL2 Ubuntu에 설치

WSL Ubuntu 터미널에서:

```bash
# Ollama 설치 (자동으로 GPU 감지)
curl -fsSL https://ollama.com/install.sh | sh

# 설치 확인
ollama --version
```

### 4.2 Ollama 서버 시작

```bash
# 서버 시작 (포그라운드)
ollama serve

# 또는 백그라운드 실행
nohup ollama serve > /tmp/ollama.log 2>&1 &

# systemd 서비스로 자동 시작 (설치 시 자동 등록되는 경우가 많음)
sudo systemctl enable ollama
sudo systemctl start ollama
sudo systemctl status ollama
```

> Ollama는 Docker 컨테이너가 아닌 **WSL2 호스트에서 직접 실행**합니다.
> Docker 컨테이너 내 python-llm에서 Ollama에 접근할 때는 `http://host.docker.internal:11434` 또는
> WSL2 내부 IP(`hostname -I`로 확인)를 사용합니다.

### 4.3 필수 모델 다운로드

```bash
# LLM 추론 모델 (택 1)
ollama pull qwen2.5:3b        # 경량 (2GB, 빠름) — 기본값
ollama pull qwen2.5:7b        # 고품질 (4.7GB, 느림)

# 임베딩 모델 (필수 — 벡터 검색용)
ollama pull nomic-embed-text   # 274MB
```

### 4.4 모델 확인

```bash
# 설치된 모델 목록
ollama list

# 모델 테스트
ollama run qwen2.5:3b "안녕하세요, 두통이 있어요"
```

### 4.5 Ollama 서버 상태 확인

```bash
# 서버 상태 (기본 포트: 11434)
curl http://localhost:11434/api/tags
```

정상 응답 예시:

```json
{"models":[{"name":"qwen2.5:3b","model":"qwen2.5:3b",...}]}
```

### 4.6 Ollama 환경변수 설정 (선택)

WSL Ubuntu에서:

```bash
# 다른 포트 사용 시
export OLLAMA_HOST="0.0.0.0:11434"

# GPU 메모리 제한 (VRAM 부족 시)
export OLLAMA_NUM_GPU=999    # 전체 GPU 레이어
export OLLAMA_MAX_LOADED_MODELS=1

# 영구 적용 시 ~/.bashrc에 추가
echo 'export OLLAMA_HOST="0.0.0.0:11434"' >> ~/.bashrc
```

---

## 5. vLLM 설치 (운영용 LLM 백엔드)

HMS 프로젝트에서 vLLM은 **운영 환경용 LLM 백엔드**로, Continuous Batching을 통해 높은 처리량을 제공합니다.

> vLLM은 **Linux + NVIDIA GPU + CUDA** 환경에서만 동작합니다.
> Windows에서는 WSL2 내부에 설치합니다.

### 5.1 NVIDIA 드라이버 확인 (Windows)

```powershell
nvidia-smi
```

출력에서 `Driver Version: 535+`, `CUDA Version: 12.x` 확인.
드라이버가 없으면 [NVIDIA 드라이버 다운로드](https://www.nvidia.com/Download/index.aspx)에서 설치.

### 5.2 WSL2에서 CUDA 확인

```bash
# WSL Ubuntu에서
nvidia-smi
```

> Windows에 NVIDIA 드라이버가 설치되어 있으면 WSL2에서 자동으로 GPU에 접근 가능.
> WSL 내부에 별도 NVIDIA 드라이버를 설치하지 마세요 (충돌 발생).

### 5.3 vLLM 설치 (WSL2 Ubuntu)

```bash
# Python 가상환경 생성
python3.11 -m venv ~/vllm-env
source ~/vllm-env/bin/activate

# vLLM 설치
pip install vllm
```

> 설치 시간: 약 10~20분 (CUDA 의존성 포함)

### 5.4 vLLM 서버 실행

```bash
source ~/vllm-env/bin/activate

# qwen2.5-3b 모델로 서버 시작 (포트 9000)
python -m vllm.entrypoints.openai.api_server \
    --model Qwen/Qwen2.5-3B-Instruct \
    --served-model-name qwen2.5-3b \
    --host 0.0.0.0 \
    --port 9000 \
    --max-model-len 4096 \
    --gpu-memory-utilization 0.8
```

| 파라미터                   | 설명                     | 기본값                   |
| -------------------------- | ------------------------ | ------------------------ |
| `--model`                  | Hugging Face 모델 ID     | Qwen/Qwen2.5-3B-Instruct |
| `--served-model-name`      | API에서 사용할 모델 이름 | qwen2.5-3b               |
| `--port`                   | 서버 포트                | 9000                     |
| `--max-model-len`          | 최대 컨텍스트 길이       | 4096                     |
| `--gpu-memory-utilization` | GPU 메모리 사용 비율     | 0.8                      |

### 5.5 vLLM 서버 확인

```bash
# 헬스 체크
curl http://localhost:9000/health

# 모델 목록
curl http://localhost:9000/v1/models

# 추론 테스트
curl http://localhost:9000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen2.5-3b",
    "messages": [{"role": "user", "content": "두통이 있어요"}],
    "max_tokens": 256,
    "temperature": 0.3
  }'
```

### 5.6 vLLM을 Docker로 실행 (대안)

GPU가 있는 리눅스 서버에서:

```bash
docker run --gpus all \
  -p 9000:9000 \
  vllm/vllm-openai:latest \
  --model Qwen/Qwen2.5-3B-Instruct \
  --served-model-name qwen2.5-3b \
  --host 0.0.0.0 \
  --port 9000
```

### 5.7 Ollama vs vLLM 비교

| 항목          | Ollama               | vLLM                       |
| ------------- | -------------------- | -------------------------- |
| 설치 난이도   | 쉬움 (원클릭)        | 보통 (CUDA 필요)           |
| GPU 필수      | 아니오 (CPU 가능)    | 예 (NVIDIA 필수)           |
| 처리량        | 낮음 (단일 요청)     | 높음 (Continuous Batching) |
| API 형식      | Ollama 전용          | OpenAI 호환                |
| 적합 시나리오 | 개발/테스트          | 운영/다중 사용자           |
| HMS 환경변수  | `LLM_BACKEND=ollama` | `LLM_BACKEND=vllm`         |

---

## 6. ChromaDB 구동 (벡터 검색)

HMS에서 ChromaDB는 의학 데이터/병원 규칙의 **벡터 검색 엔진**으로 사용됩니다.

### 6.1 Docker로 실행 (권장)

```bash
docker run -d \
  --name hms-chromadb \
  -p 8100:8000 \
  -v hms_chroma_data:/chroma/chroma \
  -e ANONYMIZED_TELEMETRY=FALSE \
  chromadb/chroma:1.5.4
```

### 6.2 상태 확인

```bash
# 헬스 체크
curl http://localhost:8100/api/v2/heartbeat

# 또는 Python으로
python -c "import chromadb; c = chromadb.HttpClient(host='localhost', port=8100); print(c.heartbeat())"
```

### 6.3 주의사항

- **Windows 네이티브에서 ChromaDB 직접 실행 금지**: SQLite 호환 문제로 segfault 발생. 반드시 Docker 컨테이너 사용
- 포트 매핑: 컨테이너 내부 `8000` → 호스트 `8100`
- HMS 설정에서 `CHROMA_HOST=localhost`, `CHROMA_PORT=8100`

---

## 7. Python LLM 서버 로컬 실행

### 7.1 가상환경 생성 및 의존성 설치

```bash
# WSL Ubuntu에서 프로젝트 디렉토리로 이동
# (Windows D:\workspace\hms는 WSL에서 /mnt/d/workspace/hms로 접근)
cd /mnt/d/workspace/hms/python-llm

# 가상환경 생성
python3.11 -m venv .venv

# 활성화
source .venv/bin/activate

# 의존성 설치
pip install -r requirements.txt
```

### 7.2 환경변수 설정

`.env` 파일 생성 (`python-llm/.env`):

```ini
# === LLM 백엔드 선택 ===
# ollama: 로컬 Ollama 사용 (개발 권장)
# vllm: vLLM 서버 사용 (운영/GPU 환경)
LLM_BACKEND=ollama

# === Ollama 설정 ===
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:3b
OLLAMA_EMBED_MODEL=nomic-embed-text

# === vLLM 설정 (vLLM 사용 시) ===
# VLLM_BASE_URL=http://localhost:9000
# VLLM_MODEL=qwen2.5-3b

# === MySQL (필수) ===
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=hms_admin
MYSQL_PASSWORD=hms_password
MYSQL_DB=hms_db

# === ChromaDB ===
CHROMA_HOST=localhost
CHROMA_PORT=8100
USE_VECTOR_SEARCH=true

# === 서버 ===
HOST=0.0.0.0
PORT=8000
```

### 7.3 서버 실행

```bash
# 방법 1: 스크립트 사용 (WSL Ubuntu)
chmod +x run.sh
./run.sh
./run.sh --reload   # 코드 변경 시 자동 재시작

# 방법 2: 직접 실행
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

### 7.4 서버 상태 확인

```bash
# 루트 엔드포인트
curl http://localhost:8000/

# 헬스 체크 (MySQL, ChromaDB, LLM 상태 통합)
curl http://localhost:8000/health

# 추론 테스트
curl -X POST http://localhost:8000/infer \
  -H "Content-Type: application/json" \
  -d '{"query": "두통이 심해요", "max_length": 256, "temperature": 0.3}'
```

---

## 8. Docker Compose 풀스택 구동

모든 서비스를 한 번에 실행하는 방법입니다.

### 8.1 사전 준비

```bash
# WSL Ubuntu에서 프로젝트 디렉토리로 이동
cd /mnt/d/workspace/hms

# python-llm 디렉토리 확인
ls python-llm/app.py

# .env 파일 생성 (프로젝트 루트)
# Claude API 키가 있으면 설정
echo "CLAUDE_API_KEY=your-key-here" > .env
```

### 8.2 전체 서비스 시작

```bash
# 빌드 + 시작
docker compose up -d --build

# 로그 확인
docker compose logs -f

# 특정 서비스 로그만
docker compose logs -f python-llm
docker compose logs -f spring-app
```

### 8.3 서비스 구성

```
docker compose up -d 실행 시 아래 순서로 시작됩니다:

1. mysql        (hms-db)        → 포트 3306
2. redis        (hms-redis)     → 포트 6379
3. chromadb     (hms-chromadb)  → 포트 8100
4. python-llm   (hms-python)    → 포트 8000  (mysql, chromadb 의존)
5. spring-app   (hms-spring)    → 포트 8080  (mysql, redis, python-llm 의존)
6. nginx        (hms-nginx)     → 포트 443/80 (spring-app 의존)
```

### 8.4 LLM 백엔드 전환

`docker-compose.yml`에서 python-llm 서비스의 환경변수를 변경:

```yaml
# Ollama 사용 (WSL2 호스트에서 Ollama가 실행 중이어야 함)
environment:
  - LLM_BACKEND=ollama
  - OLLAMA_BASE_URL=http://host.docker.internal:11434

# vLLM 사용 (WSL2에서 vLLM 서버가 실행 중이어야 함)
environment:
  - LLM_BACKEND=vllm
  - VLLM_BASE_URL=http://host.docker.internal:9000
  - VLLM_MODEL=qwen2.5-3b
```

> **`host.docker.internal`**: Docker 컨테이너에서 WSL2 호스트(= Ollama/vLLM이 실행 중인 환경)에 접근하는 특수 DNS.
> `docker-compose.yml`의 `extra_hosts: ["host.docker.internal:host-gateway"]` 설정으로 활성화됩니다.
>
> 만약 `host.docker.internal`이 동작하지 않으면, WSL2 내부 IP를 직접 지정할 수 있습니다:
>
> ```bash
> # WSL2 호스트 IP 확인
> hostname -I   # 예: 172.25.160.1
> ```
>
> → `OLLAMA_BASE_URL=http://172.25.160.1:11434`

변경 후 재시작:

```bash
docker compose up -d python-llm
```

### 8.5 서비스 중지 및 정리

```bash
# 서비스 중지
docker compose down

# 서비스 중지 + 볼륨 삭제 (DB 데이터 포함 초기화)
docker compose down -v

# 특정 서비스만 재시작
docker compose restart python-llm
```

---

## 9. 데이터 인덱싱

ChromaDB에 의학 데이터/병원 규칙을 벡터 인덱싱합니다.

### 9.1 사전 조건

- MySQL에 `hms_db` 데이터베이스가 생성되어 있어야 함
- Ollama에 `nomic-embed-text` 임베딩 모델이 설치되어 있어야 함
- ChromaDB가 실행 중이어야 함

### 9.2 병원 규칙 인덱싱

```bash
cd python-llm
source .venv/bin/activate  # 또는 .venv\Scripts\activate.bat

# 병원 규칙 JSON → MySQL + ChromaDB
python index_rule_data.py
```

데이터 소스: `llm_data/medical_rules.json`

### 9.3 의학 데이터 인덱싱

```bash
# 전체 인덱싱 (최초)
python index_medical_data.py --full

# 증분 인덱싱 (변경분만)
python index_medical_data.py
```

### 9.4 인덱싱 확인

```bash
# ChromaDB 문서 수 확인
python -c "
from vector_store import get_document_count, get_rule_collection
print(f'Medical docs: {get_document_count()}')
print(f'Rule docs: {get_rule_collection().count()}')
"

# 헬스 체크로 전체 확인
curl http://localhost:8000/health
```

---

## 10. 검증 및 테스트

### 10.1 단계별 검증 체크리스트

```
[ ] 1. Ollama 서버 응답     → curl http://localhost:11434/api/tags
[ ] 2. Ollama 모델 존재     → ollama list (qwen2.5:3b, nomic-embed-text)
[ ] 3. ChromaDB 응답        → curl http://localhost:8100/api/v2/heartbeat
[ ] 4. MySQL 접속           → mysql -h 127.0.0.1 -P 3306 -u hms_admin -p
[ ] 5. Python 서버 시작     → curl http://localhost:8000/
[ ] 6. 헬스 체크 통합       → curl http://localhost:8000/health
[ ] 7. 추론 테스트          → curl -X POST http://localhost:8000/infer ...
[ ] 8. 의학 추론 테스트     → curl -X POST http://localhost:8000/infer/medical ...
[ ] 9. 스트리밍 테스트      → curl -N http://localhost:8000/infer/medical/stream ...
[ ] 10. Spring 연동 테스트  → 브라우저에서 http://localhost:8080 접속
```

### 10.2 LLM 추론 테스트

```bash
# 일반 추론
curl -X POST http://localhost:8000/infer \
  -H "Content-Type: application/json" \
  -d '{
    "query": "두통이 심하고 열이 나요",
    "max_length": 512,
    "temperature": 0.3
  }'

# 의학 추론 (RAG 컨텍스트 포함)
curl -X POST http://localhost:8000/infer/medical \
  -H "Content-Type: application/json" \
  -d '{
    "query": "두통이 심하고 열이 나요. 어떤 진료과에 가야 하나요?",
    "max_length": 512,
    "temperature": 0.3
  }'

# 병원 규칙 Q&A
curl -X POST http://localhost:8000/infer/rule \
  -H "Content-Type: application/json" \
  -d '{
    "query": "야간 당직 규정이 어떻게 되나요?",
    "max_length": 512,
    "temperature": 0.3
  }'

# SSE 스트리밍 (토큰 단위 실시간 출력)
curl -N -X POST http://localhost:8000/infer/medical/stream \
  -H "Content-Type: application/json" \
  -d '{
    "query": "허리가 아프고 다리가 저려요",
    "max_length": 512,
    "temperature": 0.3
  }'
```

### 10.3 Python 단위 테스트

```bash
cd python-llm
source .venv/bin/activate
pip install -r requirements-dev.txt

pytest tests/ -v
```

### 10.4 메트릭 확인

```bash
curl http://localhost:8000/metrics
```

응답 예시:

```json
{
  "total_requests": 15,
  "success_count": 14,
  "error_count": 1,
  "avg_latency_ms": 2340.5,
  "vector_hit_rate": 0.85
}
```

---

## 11. 환경변수 레퍼런스

### Python LLM 서버 (`python-llm/.env`)

| 변수                    | 기본값                      | 설명                                         |
| ----------------------- | --------------------------- | -------------------------------------------- |
| `LLM_BACKEND`           | `vllm`                      | LLM 백엔드 (`ollama`, `vllm`, `huggingface`) |
| `OLLAMA_BASE_URL`       | `http://localhost:11434`    | Ollama 서버 URL                              |
| `OLLAMA_MODEL`          | `qwen2.5:3b`                | Ollama 추론 모델명                           |
| `OLLAMA_EMBED_MODEL`    | `nomic-embed-text`          | Ollama 임베딩 모델명                         |
| `VLLM_BASE_URL`         | `http://localhost:9000`     | vLLM 서버 URL                                |
| `VLLM_MODEL`            | `qwen2.5-3b`                | vLLM 모델명                                  |
| `MYSQL_HOST`            | `localhost`                 | MySQL 호스트                                 |
| `MYSQL_PORT`            | `3306`                      | MySQL 포트                                   |
| `MYSQL_USER`            | (필수)                      | MySQL 사용자                                 |
| `MYSQL_PASSWORD`        | (필수)                      | MySQL 비밀번호                               |
| `MYSQL_DB`              | `hms_db`                    | MySQL 데이터베이스명                         |
| `CHROMA_HOST`           | `localhost`                 | ChromaDB 호스트                              |
| `CHROMA_PORT`           | `8100`                      | ChromaDB 포트                                |
| `USE_VECTOR_SEARCH`     | `true`                      | 벡터 검색 사용 여부                          |
| `HOST`                  | `0.0.0.0`                   | 서버 바인드 주소                             |
| `PORT`                  | `8000`                      | 서버 포트                                    |
| `LLM_INFER_TIMEOUT_SEC` | `60`                        | 추론 타임아웃 (초)                           |
| `LLM_INPUT_MAX_LENGTH`  | `2048`                      | 입력 최대 문자 수                            |
| `CORS_ORIGINS`          | `http://localhost:8080,...` | CORS 허용 origin                             |

### Docker Compose 환경변수 (docker-compose.yml)

| 서비스     | 변수                  | 값                                  |
| ---------- | --------------------- | ----------------------------------- |
| mysql      | `MYSQL_ROOT_PASSWORD` | `rootpassword`                      |
| mysql      | `MYSQL_DATABASE`      | `hms_db`                            |
| mysql      | `MYSQL_USER`          | `hms_admin`                         |
| mysql      | `MYSQL_PASSWORD`      | `hms_password`                      |
| python-llm | `LLM_BACKEND`         | `vllm` 또는 `ollama`                |
| python-llm | `OLLAMA_BASE_URL`     | `http://host.docker.internal:11434` |
| python-llm | `VLLM_BASE_URL`       | `http://<vllm-서버-IP>:9000`        |
| spring-app | `LLM_SERVICE_URL`     | `http://python-llm:8000`            |

---

## 12. 트러블슈팅

### 12.1 Ollama 관련

#### Ollama 서버 연결 실패

```
ConnectionError: Ollama 서버에 연결할 수 없습니다
```

**원인**: Ollama 서버가 실행되지 않음
**해결**:

```bash
# WSL Ubuntu에서 Ollama 서버 시작
ollama serve &

# systemd로 관리 중이라면
sudo systemctl start ollama
sudo systemctl status ollama
```

#### 모델 다운로드 실패

```bash
# 재시도
ollama pull qwen2.5:3b

# 특정 레지스트리 사용
OLLAMA_HOST=0.0.0.0:11434 ollama pull qwen2.5:3b
```

#### GPU 메모리 부족 (Ollama)

```
Error: model requires more system memory than is available
```

**해결**: 더 작은 모델 사용

```bash
ollama pull qwen2.5:1.5b   # 1.5B 파라미터 (1GB)
# python-llm/.env에서
OLLAMA_MODEL=qwen2.5:1.5b
```

### 12.2 vLLM 관련

#### CUDA 미검출

```
RuntimeError: No CUDA GPUs are available
```

**해결**:

```bash
# WSL에서 GPU 확인
nvidia-smi

# 안 되면 Windows NVIDIA 드라이버 업데이트 후 WSL 재시작
wsl --shutdown
wsl
```

#### vLLM OOM (GPU 메모리 부족)

```
torch.cuda.OutOfMemoryError
```

**해결**: GPU 메모리 사용률 낮추기

```bash
python -m vllm.entrypoints.openai.api_server \
    --model Qwen/Qwen2.5-3B-Instruct \
    --gpu-memory-utilization 0.5 \
    --max-model-len 2048
```

### 12.3 ChromaDB 관련

#### Windows에서 segfault

```
Segmentation fault (core dumped)
```

**원인**: Windows 네이티브 SQLite 호환 문제
**해결**: Docker 컨테이너로만 사용 (6.1절 참고)

#### ChromaDB healthcheck 실패

```bash
# Docker 로그 확인
docker logs hms-chromadb

# 컨테이너 재시작
docker restart hms-chromadb
```

### 12.4 MySQL 관련

#### localhost가 IPv6(::1)로 해석

```
Can't connect to MySQL server on 'localhost'
```

**해결**: `localhost` 대신 `127.0.0.1`로 명시

```ini
# .env
MYSQL_HOST=127.0.0.1
```

#### Docker MySQL 시작 타임아웃

**원인**: healthcheck가 DB 준비 전에 실패
**해결**: `docker-compose.yml`에서 healthcheck 설정 확인 (이미 적용됨):

```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
  interval: 10s
  timeout: 10s
  retries: 15
  start_period: 90s
```

### 12.5 Python LLM 서버 관련

#### aiomysql 연결 풀 hang

**원인**: `minsize > 0`일 때 MySQL 미응답 시 서버 시작 멈춤
**해결**: HMS 프로젝트에서는 이미 lazy connection (minsize=0)으로 구성됨. `.env`에서 MySQL 접속 정보 확인.

#### torch 로딩 실패

```
OSError: libcudnn.so: cannot open shared object file
```

**해결**: WSL2에서 CUDA 관련 라이브러리가 누락된 경우

```bash
# CUDA 런타임 확인
nvidia-smi
# 실제 LLM 추론은 Ollama/vLLM으로 대체하고, torch 직접 사용은 권장하지 않음
# .env에서 mock 모드 활성화 (torch 미사용)
LLM_FALLBACK_MOCK=true
```

### 12.6 Docker 네트워크 관련

#### 컨테이너에서 호스트 Ollama/vLLM 접근 불가

**원인**: Docker 컨테이너에서 WSL2 호스트의 Ollama(11434)/vLLM(9000)에 접근 불가
**해결**: `docker-compose.yml`에 `extra_hosts` 설정 (이미 적용됨):

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

```ini
# .env 또는 docker-compose.yml에서
OLLAMA_BASE_URL=http://host.docker.internal:11434
VLLM_BASE_URL=http://host.docker.internal:9000
```

`host.docker.internal`이 동작하지 않는 경우:

### 12.6 Docker 네트워크 관련

#### 컨테이너에서 호스트 Ollama/vLLM 접근 불가

**원인**: Docker 컨테이너에서 WSL2 호스트의 Ollama(11434)/vLLM(9000)에 접근 불가
**해결**: `docker-compose.yml`에 `extra_hosts` 설정 (이미 적용됨):

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

```ini
# .env 또는 docker-compose.yml에서
OLLAMA_BASE_URL=http://host.docker.internal:11434
VLLM_BASE_URL=http://host.docker.internal:9000
```

`host.docker.internal`이 동작하지 않는 경우:

```bash
# WSL2 호스트 IP 직접 확인
ip addr show eth0 | grep inet
# 또는
hostname -I

# 출력된 IP를 직접 사용
OLLAMA_BASE_URL=http://172.25.160.1:11434
```

#### Docker 서비스가 시작되지 않음 (WSL2)

```bash
# Docker 서비스 상태 확인
sudo service docker status

# 시작
sudo service docker start

# systemd 활성화 확인 (/etc/wsl.conf에 [boot] systemd=true 필요)
systemctl is-system-running
```

---

## 빠른 시작 요약

> 모든 명령어는 **WSL2 Ubuntu 터미널**에서 실행합니다.

### 최소 구성 (개발용 — Ollama)

```bash
# 1. Ollama 설치 + 모델 다운로드
curl -fsSL https://ollama.com/install.sh | sh
ollama serve &
ollama pull qwen2.5:3b
ollama pull nomic-embed-text

# 2. Docker로 MySQL + ChromaDB 실행
cd /mnt/d/workspace/hms
docker compose up -d mysql chromadb

# 3. Python LLM 서버 실행
cd python-llm
python3.11 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
# .env에서 LLM_BACKEND=ollama, MYSQL_USER/PASSWORD 설정
uvicorn app:app --host 0.0.0.0 --port 8000 --reload

# 4. 데이터 인덱싱
python index_rule_data.py
python index_medical_data.py --full

# 5. 검증
curl http://localhost:8000/health
```

### 풀스택 구성 (Docker Compose)

```bash
# 1. Ollama 설치 + 모델 다운로드 (WSL2에서)
curl -fsSL https://ollama.com/install.sh | sh
ollama serve &
ollama pull qwen2.5:3b
ollama pull nomic-embed-text

# 2. 전체 서비스 시작
cd /mnt/d/workspace/hms
docker compose up -d --build

# 3. 확인
docker compose ps
curl http://localhost:8000/health
curl http://localhost:8080
```
