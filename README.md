# 🏥 병원 예약 & 내부 업무 시스템

Spring Boot + Mustache SSR + MySQL + Claude API

## 기술 스택
- Java 17 / Spring Boot 3.x
- Spring Security (세션 기반, 4 ROLE)
- JPA / MySQL
- Mustache (SSR)
- Claude API (Anthropic)

## 팀 구성
| 역할 | 담당 영역 |
|------|-----------|
| 경력자 (Tech Lead) | 아키텍처·Security·LLM 서비스·배포 |
| 개발자 A | 외부 예약 흐름 (/reservation/**) |
| 개발자 B | 내부 직원 화면 (STAFF·DOCTOR·NURSE) |
| 개발자 C | 관리자 화면 (/admin/**) |

## 로컬 실행
1. `application-local.properties` 에 DB 접속 정보 + Claude API Key 입력
2. `./mvnw spring-boot:run`
```

---

### 3. .gitignore 반드시 추가할 항목
```
# 환경 설정 (API Key 포함)
application-local.properties
application-secret.properties
*.env

# IDE
.idea/
*.iml
.vscode/

# Build
target/
```

---

### 4. Branch Protection Rules (Settings → Branches)

| 브랜치 | 규칙 |
|--------|------|
| `main` | ☑ Require pull request before merging / ☑ Require 1 approving review (경력자) / ☑ Do not allow bypassing |
| `develop` | ☑ Require pull request before merging |

---

### 5. Teams 구성 (Organization → Teams)

| Team 이름 | 멤버 | Repository 권한 |
|-----------|------|----------------|
| `tech-lead` | 경력자 | Admin |
| `dev-frontend-a` | 개발자 A | Write |
| `dev-internal-b` | 개발자 B | Write |
| `dev-admin-c` | 개발자 C | Write |

---

### 6. Issues Labels 추가 (Issues → Labels)
```
feature-reservation   #0075ca   개발자 A 담당
feature-staff         #e4e669   개발자 B 담당
feature-doctor        #e4e669
feature-nurse         #e4e669
feature-admin         #d93f0b   개발자 C 담당
feature-llm           #5319e7   경력자 담당
blocker               #b60205   30분 이상 블로킹
collision-risk        #f9d0c4   소유권 충돌 위험
```

---

### 7. Projects (칸반 보드) 설정
```
Project name:  Hospital System MVP
Template:      Board (Kanban)

Column 구성:
  📋 Backlog → 🔄 In Progress → 👀 In Review → ✅ Done
