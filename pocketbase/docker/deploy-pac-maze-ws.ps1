#Requires -Version 5.1
# 仅构建并启动 pac-maze-ws（依赖 PocketBase 容器已运行或一并启动）

param(
    [switch]$BuildOnly,
    [switch]$WithPocketBase,
)

$ErrorActionPreference = "Stop"
$pbDir = Split-Path $PSScriptRoot -Parent
$repoRoot = Split-Path $pbDir -Parent

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "请先安装 Docker Desktop" -ForegroundColor Red
    exit 1
}

Write-Host "=== Gradle installDist ===" -ForegroundColor Cyan
Set-Location $repoRoot
& .\gradlew.bat :pac-maze-server:installDist --no-daemon -q
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Set-Location $pbDir
Write-Host "=== Docker build pac-maze-ws ===" -ForegroundColor Cyan
docker compose build pac-maze-ws
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($BuildOnly) {
    Write-Host "Build complete." -ForegroundColor Green
    exit 0
}

if ($WithPocketBase) {
    docker compose up -d pocketbase pac-maze-ws
} else {
    docker compose up -d pac-maze-ws
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Start-Sleep -Seconds 3
try {
    $h = Invoke-RestMethod "http://127.0.0.1:8791/health" -TimeoutSec 10
    Write-Host "[OK] pac-maze-ws :8791 $($h | ConvertTo-Json -Compress)" -ForegroundColor Green
} catch {
    Write-Host "[WARN] health 未就绪，查看日志: docker compose logs pac-maze-ws" -ForegroundColor Yellow
}

Write-Host "WS: ws://127.0.0.1:8791/pac-maze-ws" -ForegroundColor Green
