# Nginx HTTPS 설정 가이드 (Docker 운영 환경)

## 개요

Docker Compose 환경에서 Nginx 리버스 프록시를 통해 HTTPS를 적용하는 설정 가이드입니다.
자체 서명 인증서(Self-signed Certificate)를 사용하며, Spring Boot 앱은 내부적으로 HTTP(8080)로 동작합니다.

```
[브라우저] --HTTPS(443)--> [Nginx] --HTTP(8080)--> [Spring Boot]
                           |
                     SSL 종료 (SSL Termination)
```

---

## 1. 아키텍처

| 구성 요소   | 역할                     | 포트                                    |
| ----------- | ------------------------ | --------------------------------------- |
| Nginx       | SSL 종료 + 리버스 프록시 | 443 (HTTPS), 80 (HTTP→HTTPS 리다이렉트) |
| Spring Boot | 애플리케이션 서버        | 8080 (내부 전용, expose only)           |
| MySQL       | 데이터베이스             | 3306                                    |
| Redis       | 세션/캐시                | 6379                                    |
| Python LLM  | LLM 추론 서비스          | 8000                                    |

**핵심**: Spring Boot는 외부에 포트를 노출하지 않고(`expose`), Nginx만 443/80을 외부에 노출합니다.

---

## 2. SSL 인증서 생성

Docker 호스트(WSL2 또는 Linux 서버)에서 자체 서명 인증서를 생성합니다.

```bash
# 프로젝트 루트에서 실행
mkdir -p nginx/ssl

# 자체 서명 인증서 생성 (유효기간 365일)
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/server.key \
  -out nginx/ssl/server.crt \
  -subj "/CN=192.168.0.22"
```

### Windows Git Bash에서 실행 시 주의

Git Bash는 `/CN=...` 경로를 Windows 경로로 변환하는 문제가 있습니다. 아래와 같이 환경변수를 설정하세요:

```bash
MSYS_NO_PATHCONV=1 openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/server.key \
  -out nginx/ssl/server.crt \
  -subj "/CN=192.168.0.22"
```

### 인증서 파일 확인

```bash
ls -la nginx/ssl/
# server.crt  (인증서)
# server.key  (개인키)
```

> `.gitignore`에 `nginx/ssl/`이 등록되어 있어 인증서는 Git에 포함되지 않습니다.

---

## 3. Nginx 설정

### `nginx/default.conf`

```nginx
# HTTPS 서버
server {
    listen 443 ssl;
    server_name 192.168.0.22;

    ssl_certificate     /etc/nginx/ssl/server.crt;
    ssl_certificate_key /etc/nginx/ssl/server.key;

    location / {
        proxy_pass http://spring-app:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}

# HTTP -> HTTPS 리다이렉트
server {
    listen 80;
    server_name 192.168.0.22;
    return 301 https://$host$request_uri;
}
```

**주요 헤더 설명**:

- `X-Forwarded-Proto https` — Spring Boot가 원본 요청이 HTTPS임을 인식하도록 전달
- `X-Real-IP` — 클라이언트 실제 IP 전달 (Rate Limiting 등에 활용)
- `X-Forwarded-For` — 프록시 체인 IP 전달

---

## 4. Docker Compose 설정

### `docker-compose.yml` — spring-app 서비스

```yaml
spring-app:
  build:
    context: .
    dockerfile: Dockerfile
  container_name: hms-spring
  expose:
    - "8080" # ports 대신 expose (내부 전용)
  environment:
    - SERVER_PORT=8080
    - HMS_CORS_ALLOWED_ORIGINS=https://192.168.0.22,https://localhost
    # ... 기타 환경변수
```

> `ports: "8080:8080"` → `expose: "8080"`으로 변경하여 외부 직접 접근을 차단합니다.

### `docker-compose.yml` — nginx 서비스

```yaml
nginx:
  image: nginx:alpine
  container_name: hms-nginx
  ports:
    - "0.0.0.0:443:443" # HTTPS
    - "0.0.0.0:80:80" # HTTP (→ HTTPS 리다이렉트)
  volumes:
    - ./nginx/default.conf:/etc/nginx/conf.d/default.conf
    - ./nginx/ssl:/etc/nginx/ssl
  depends_on:
    - spring-app
```

---

## 5. Spring Boot 설정

### `application-prod.properties`

```properties
# 세션 쿠키 — HTTPS에서만 전송
server.servlet.session.cookie.secure=true

# 프록시 헤더 신뢰 (X-Forwarded-Proto 등)
server.forward-headers-strategy=framework

# CORS 허용 Origin (HTTPS)
hms.cors.allowed-origins=https://192.168.0.22,https://localhost
```

**`server.forward-headers-strategy=framework`가 필수인 이유**:

- Nginx가 `X-Forwarded-Proto: https` 헤더를 전달
- Spring Boot가 이 헤더를 읽어 원본 요청이 HTTPS임을 인식
- 이 설정이 없으면 Spring이 HTTP로 인식 → 리다이렉트 URL이 `http://`로 생성되는 문제 발생

---

## 6. 기동 및 확인

### 6.1 Docker Compose 기동

```bash
docker compose up -d --build
```

### 6.2 컨테이너 상태 확인

```bash
docker ps

# 정상 상태 예시:
# hms-nginx    nginx:alpine     0.0.0.0:443->443, 0.0.0.0:80->80   Up
# hms-spring   ...              8080/tcp                             Up
# hms-db       mysql:8.0        0.0.0.0:3306->3306                  Up
# hms-redis    redis:7-alpine   0.0.0.0:6379->6379                  Up
# hms-python   ...              0.0.0.0:8000->8000                  Up
```

### 6.3 Nginx 로그 확인

```bash
# 인증서 로드 실패 등 에러 확인
docker logs hms-nginx
```

### 6.4 HTTPS 접속 테스트

```bash
# CLI 테스트 (자체 서명 인증서이므로 -k 옵션 필요)
curl -k https://192.168.0.22

# HTTP → HTTPS 리다이렉트 확인
curl -I http://192.168.0.22
# HTTP/1.1 301 Moved Permanently
# Location: https://192.168.0.22/
```

브라우저에서 `https://192.168.0.22` 접속 시 자체 서명 인증서 경고가 표시됩니다.
"고급" → "안전하지 않음 계속 진행"을 선택하면 정상 접속됩니다.

---

## 7. WSL2 환경 추가 설정 (Docker Engine 직접 설치 시)

WSL2에 Docker Engine을 직접 설치한 경우 (Docker Desktop 미사용), WSL2의 NAT 네트워킹으로 인해 Windows 호스트에서 직접 접근이 불가능합니다.

### 7.1 WSL2 IP 확인

```bash
# WSL2 내부에서 실행
ip addr show eth0 | grep "inet "
# 예: inet 172.31.73.81/20
```

### 7.2 Windows 포트 포워딩 설정

Windows PowerShell (관리자 권한)에서 실행:

```powershell
# WSL2 IP 확인
$wslIp = (wsl hostname -I).Trim().Split(" ")[0]
echo "WSL2 IP: $wslIp"

# 포트 포워딩 추가 (443, 80)
netsh interface portproxy add v4tov4 listenport=443 listenaddress=0.0.0.0 connectport=443 connectaddress=$wslIp
netsh interface portproxy add v4tov4 listenport=80  listenaddress=0.0.0.0 connectport=80  connectaddress=$wslIp

# 설정 확인
netsh interface portproxy show v4tov4
```

### 7.3 포트 포워딩 삭제 (필요 시)

```powershell
netsh interface portproxy delete v4tov4 listenport=443 listenaddress=0.0.0.0
netsh interface portproxy delete v4tov4 listenport=80  listenaddress=0.0.0.0
```

### 7.4 WSL2 IP 변경 시

WSL2 재시작 시 IP가 변경될 수 있습니다. 이 경우 포트 포워딩을 다시 설정해야 합니다.

**영구 해결**: `.wslconfig`에서 mirrored 모드 사용 (Windows 11 22H2+)

```ini
# %USERPROFILE%\.wslconfig
[wsl2]
networkingMode=mirrored
```

mirrored 모드에서는 포트 포워딩 없이 `localhost` 또는 LAN IP로 직접 접근 가능합니다.

---

## 8. 트러블슈팅

### Nginx가 시작 직후 종료됨

```bash
docker logs hms-nginx
# "cannot load certificate" 에러 시:
```

- `nginx/ssl/` 디렉토리에 `server.crt`, `server.key` 파일이 존재하는지 확인
- 인증서는 **Docker 호스트**에서 생성해야 함 (Windows가 아닌 WSL2/Linux)
- 파일 권한 확인: `chmod 644 server.crt && chmod 600 server.key`

### HTTP 접속 시 ERR_CONNECTION_REFUSED

- Docker 컨테이너 상태 확인: `docker ps`
- WSL2 환경이면 포트 포워딩 설정 확인 (7장 참고)
- 방화벽에서 443, 80 포트 허용 여부 확인

### Spring Boot 리다이렉트가 http://로 생성됨

- `application-prod.properties`에 아래 설정 확인:
  ```properties
  server.forward-headers-strategy=framework
  ```
- Nginx 설정에서 `proxy_set_header X-Forwarded-Proto https;` 확인

### 세션 쿠키가 전송되지 않음 (로그인 유지 안됨)

- `server.servlet.session.cookie.secure=true` 설정 시 HTTPS에서만 쿠키 전송
- HTTP로 접근하면 세션 쿠키가 저장되지 않음 → 반드시 HTTPS로 접속

---

## 9. 관련 파일 목록

| 파일                                             | 설명                                     |
| ------------------------------------------------ | ---------------------------------------- | ---------------------- | ---------------------- | ---------------------- | ---------------------- | ---------------------- | ---------------------- | ---------------------- |
| `nginx/default.conf`                             | Nginx SSL + 리버스 프록시 설정           |
| `nginx/ssl/server.crt`                           | SSL 인증서 (.gitignore 대상)             |
| `nginx/ssl/server.key`                           | SSL 개인키 (.gitignore 대상)             |
| `docker-compose.yml`                             | nginx 서비스 및 spring-app expose 설정   |
| `src/main/resources/application-prod.properties` | Spring Boot 프록시 헤더, 쿠키, CORS 설정 |
| `.gitignore`                                     | `nginx/ssl/` 제외 설정                   | gitignore 대상)        |
| `nginx/ssl/server.key`                           | SSL 개인키 (.gitignore 대상)             |
| `docker-compose.yml`                             | nginx 서비스 및 spring-app expose 설정   |
| `src/main/resources/application-prod.properties` | Spring Boot 프록시 헤더, 쿠키, CORS 설정 |
| `.gitignore`                                     | `nginx/ssl/` 제외 설정                   | spring-app expose 설정 |
| `src/main/resources/application-prod.properties` | Spring Boot 프록시 헤더, 쿠키, CORS 설정 |
| `.gitignore`                                     | `nginx/ssl/` 제외 설정                   |                        | `nginx/ssl/` 제외 설정 | spring-app expose 설정 |
| `src/main/resources/application-prod.properties` | Spring Boot 프록시 헤더, 쿠키, CORS 설정 |
| `.gitignore`                                     | `nginx/ssl/` 제외 설정                   |                        | `nginx/ssl/` 제외 설정 |                        | `nginx/ssl/` 제외 설정 | spring-app expose 설정 |
| `src/main/resources/application-prod.properties` | Spring Boot 프록시 헤더, 쿠키, CORS 설정 |
| `.gitignore`                                     | `nginx/ssl/` 제외 설정                   |                        | `nginx/ssl/` 제외 설정 |                        | `nginx/ssl/` 제외 설정 |                        | `nginx/ssl/` 제외 설정 | spring-app expose 설정 |
| `src/main/resources/application-prod.properties` | Spring Boot 프록시 헤더, 쿠키, CORS 설정 |
| `.gitignore`                                     | `nginx/ssl/` 제외 설정                   |              | `nginx/ssl/` 제외 설정                   |              | `nginx/ssl/` 제외 설정                   |              | `nginx/ssl/` 제외 설정                   |              | `nginx/ssl/` 제외 설정                   |spring-app expose 설정 |
| `src/main/resources/application-prod.properties` | Spring Boot 프록시 헤더, 쿠키, CORS 설정 |
| `.gitignore` | `nginx/ssl/` 제외 설정 |
