# HMS 개발 서버 실행 스크립트
# .env 파일에서 환경 변수를 로드한 뒤 Spring Boot를 기동합니다.
#
# 사용법:
#   .\run-dev.ps1
#
# 최초 실행 전:
#   copy .env.example .env
#   .env 파일에 CLAUDE_API_KEY 등 필요한 값을 채워 넣으세요.

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

# ─────────────────────────────────────────────────────────────────────────────
# .env 로드
# ─────────────────────────────────────────────────────────────────────────────
$EnvPath = Join-Path $ProjectRoot ".env"
if (Test-Path $EnvPath) {
    Get-Content $EnvPath | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line -match '^([^=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
    Write-Host "[run-dev] .env 로드 완료" -ForegroundColor Green
} else {
    Write-Host "[run-dev] .env 파일이 없습니다. .env.example 을 복사하여 .env 를 생성하세요." -ForegroundColor Yellow
    Write-Host "  copy .env.example .env" -ForegroundColor Gray
}

# ─────────────────────────────────────────────────────────────────────────────
# dev 환경: prod 전용 datasource 환경변수 제거 (H2 사용)
# ─────────────────────────────────────────────────────────────────────────────
[Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_URL", $null, "Process")
[Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_USERNAME", $null, "Process")
[Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_PASSWORD", $null, "Process")

# ─────────────────────────────────────────────────────────────────────────────
# Spring Boot 실행
# ─────────────────────────────────────────────────────────────────────────────
Set-Location $ProjectRoot
& .\gradlew.bat bootRun --args='--spring.profiles.active=dev'
