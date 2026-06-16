# Pac-Maze WS 同区域部署检查

param(
    [string]$PbBase = "http://127.0.0.1:8090",
    [string]$WsPath = "/pac-maze-ws"
)

$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location $root

Write-Host "=== pac-maze-ws 同区域部署检查 ===" -ForegroundColor Cyan
Write-Host "PB: $PbBase"
Write-Host "WS health: ${PbBase}${WsPath}/health"

$healthUrl = "$PbBase$WsPath/health"
try {
    $resp = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 10
    if ($resp.ok -and $resp.service -eq "funlife-pac-maze-ws") {
        Write-Host "[OK] health: $($resp | ConvertTo-Json -Compress)" -ForegroundColor Green
    } else {
        Write-Host "[WARN] health 响应异常: $($resp | ConvertTo-Json -Compress)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[FAIL] health 不可达: $_" -ForegroundColor Red
    Write-Host "请确认: 1) pac-maze-server 已启动  2) Nginx /pac-maze-ws 反代  3) 端口 8791"
    exit 1
}

Write-Host "`nAndroid: 留空 PAC_MAZE_WS_URL，配置 POCKETBASE_URL 即可同域推导 WS。" -ForegroundColor Green
Write-Host "Done."
