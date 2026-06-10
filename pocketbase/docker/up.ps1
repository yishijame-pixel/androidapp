#Requires -Version 5.1
# FunLife Docker 一键启动（Windows）
param(
    [switch]$Push,
    [switch]$Tunnel,
    [switch]$Build,
    [switch]$Logs
)

$ErrorActionPreference = "Stop"
$pbDir = Split-Path $PSScriptRoot -Parent
Set-Location $pbDir

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "请先安装 Docker Desktop: https://www.docker.com/products/docker-desktop/" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path ".env")) {
    if (Test-Path "docker\.env.example") {
        Copy-Item "docker\.env.example" ".env"
        Write-Host "已创建 .env（请按需修改 FCM_RELAY_KEY 等）" -ForegroundColor Yellow
    }
}

$profiles = @()
if ($Push) { $profiles += "push" }
if ($Tunnel) { $profiles += "tunnel" }

if ($Push -and -not (Test-Path "secrets\firebase-adminsdk.json")) {
    Write-Host "缺少 secrets\firebase-adminsdk.json，无法启用 push 配置" -ForegroundColor Red
    Write-Host "运行 .\setup-push.ps1 或手动放置 Firebase 服务账号 JSON" -ForegroundColor Yellow
    exit 1
}

if ($Tunnel) {
    $cfg = "docker\cloudflared\config.yml"
    if (-not (Test-Path $cfg)) {
        Write-Host "缺少 $cfg" -ForegroundColor Red
        Write-Host "复制 docker\cloudflared\config.example.yml 并填入隧道 UUID" -ForegroundColor Yellow
        exit 1
    }
}

$args = @("compose")
foreach ($p in $profiles) { $args += @("--profile", $p) }
if ($Build) { $args += "build" }
$args += @("up", "-d")

Write-Host "docker $($args -join ' ')" -ForegroundColor Cyan
& docker @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Start-Sleep -Seconds 3
Write-Host ""
Write-Host "健康检查:" -ForegroundColor Green
try { Invoke-RestMethod "http://127.0.0.1:8090/api/health" -TimeoutSec 5 | Out-Null; Write-Host "  PocketBase :8090 OK" -ForegroundColor Green } catch { Write-Host "  PocketBase :8090 未就绪" -ForegroundColor Yellow }
try { Invoke-RestMethod "http://127.0.0.1:8790/health" -TimeoutSec 5 | Out-Null; Write-Host "  draw_ws    :8790 OK" -ForegroundColor Green } catch { Write-Host "  draw_ws    :8790 未就绪" -ForegroundColor Yellow }

if ($Logs) {
    $logArgs = @("compose")
    foreach ($p in $profiles) { $logArgs += @("--profile", $p) }
    $logArgs += "logs", "-f"
    & docker @logArgs
}
