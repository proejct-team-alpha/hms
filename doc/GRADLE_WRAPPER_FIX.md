# gradle-wrapper.jar 누락으로 빌드 실패 해결 가이드

## 증상

```
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
```

`git clone` 후 `./gradlew build` 실행 시 위 오류가 발생하며 빌드가 되지 않는다.

---

## 원인

`.gitignore`에서 **패턴 순서 문제**로 `gradle-wrapper.jar`가 Git에 추적되지 않는다.

```gitignore
# 7행: 먼저 선언된 negation (무시 제외)
!gradle/wrapper/gradle-wrapper.jar

# 16행: 나중에 선언된 *.jar (모든 jar 무시)
*.jar
```

`.gitignore`는 **마지막에 매칭되는 패턴이 우선** 적용된다.
따라서 7행의 negation(`!`)이 16행의 `*.jar`에 의해 덮어씌워져, `gradle-wrapper.jar`가 무시된다.

---

## 해결 방법

### 1단계: `.gitignore` 수정

`!gradle/wrapper/gradle-wrapper.jar` 를 `*.jar` **아래로** 이동한다.

```gitignore
# Java
*.class
*.jar          # 모든 jar 무시
*.war
*.ear
*.log

# Gradle Wrapper는 예외 (반드시 *.jar 아래에 위치해야 함)
!gradle/wrapper/gradle-wrapper.jar
```

> **핵심**: negation 패턴(`!`)은 반드시 해당 ignore 패턴보다 **아래에** 위치해야 한다.

### 2단계: gradle-wrapper.jar 재생성 (로컬에 파일이 없는 경우)

시스템에 Gradle이 설치되어 있다면:

```bash
gradle wrapper --gradle-version 9.3.1
```

Gradle이 설치되어 있지 않다면, 수동으로 다운로드:

```bash
mkdir -p gradle/wrapper
curl -o gradle/wrapper/gradle-wrapper.jar \
  https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
```

### 3단계: Git에 추가 및 커밋

```bash
# gitignore 수정 후 캐시 초기화 (기존 ignore 상태 제거)
git rm --cached gradle/wrapper/gradle-wrapper.jar 2>/dev/null
git add .gitignore gradle/wrapper/gradle-wrapper.jar
git commit -m "fix: gradle-wrapper.jar gitignore 순서 수정 및 파일 추적"
```

### 4단계: 빌드 확인

```bash
./gradlew build
```

---

## 참고: `.gitignore` 패턴 우선순위 규칙

| 순서 | 규칙 |
|------|------|
| 1 | 파일에서 **아래에 있는 패턴**이 위의 패턴을 덮어쓴다 |
| 2 | `!` (negation)은 해당 ignore 패턴 **이후에** 선언해야 효과가 있다 |
| 3 | 디렉토리 패턴(`dir/`)으로 무시된 경우, 내부 파일은 `!`로 복구할 수 없다 |

---

## 예방 수칙

- `gradle/wrapper/gradle-wrapper.jar`는 반드시 **Git에 커밋**되어야 한다 (Gradle 공식 권장)
- `.gitignore`에서 `*.jar` 같은 와일드카드 패턴을 사용할 때, 예외 파일의 negation은 반드시 **그 아래에** 배치한다
