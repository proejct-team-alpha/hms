# HMS 운영 프로필 로컬 실행 스크립트
# .env 파일에서 환경 변수를 로드한 뒤 prod 프로필로 Spring Boot를 기동합니다.
#
# 사용법:
#   .\run-prod.ps1
#
# 최초 실행 전:
#   .env 파일에 SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD 등을 채워 넣으세요.

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

# .env 로드
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
    Write-Host "[run-prod] .env 로드 완료" -ForegroundColor Green
} else {
    Write-Host "[run-prod] .env 파일이 없습니다. .env.example 을 복사하여 .env 를 생성하세요." -ForegroundColor Yellow
    exit 1
}

# 필수 환경 변수 검증
$required = @("SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD")
foreach ($var in $required) {
    if (-not [Environment]::GetEnvironmentVariable($var, "Process")) {
        Write-Host "[run-prod] 필수 환경 변수 $var 가 설정되지 않았습니다." -ForegroundColor Red
        exit 1
    }
}

# Spring Boot 실행 (prod 프로필)
Set-Location $ProjectRoot
& .\gradlew.bat bootRun --args='--spring.profiles.active=prod'
