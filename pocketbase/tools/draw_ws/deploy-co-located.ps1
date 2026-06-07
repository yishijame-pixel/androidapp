# 同区域部署 draw_ws（Windows 一键检查）

param(
    [string]$PbBase = "https://pb.yishi.site",
    [string]$DrawWsPath = "/draw-ws"
)

$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location (Join-Path $root "tools\draw_ws")

Write-Host "=== draw_ws 同区域部署检查 ===" -ForegroundColor Cyan
Write-Host "PB: $PbBase"
Write-Host "WS: ${PbBase}${DrawWsPath}"

$healthUrl = "$PbBase$DrawWsPath/health".Replace("https://", "https://").Replace("//draw", "/draw")
try {
    $resp = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 10
    if ($resp.ok -and $resp.service -eq "funlife-draw-ws") {
        Write-Host "[OK] health: $($resp | ConvertTo-Json -Compress)" -ForegroundColor Green
    } else {
        Write-Host "[WARN] health 响应异常" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[FAIL] health 不可达: $_" -ForegroundColor Red
    Write-Host "请确认: 1) draw_ws 已启动  2) Nginx /draw-ws 反代  3) 隧道指向同机"
    exit 1
}

$bench = Join-Path $root "tools\test_draw_guess_sync_benchmark.js"
if (Test-Path $bench) {
    Write-Host "`n=== 延迟基准（可选）===" -ForegroundColor Cyan
    $wsUrl = ($PbBase -replace "^https", "wss") + "$DrawWsPath/ws"
    Set-Location $root
    node $bench --base-url $PbBase --ws-url $wsUrl --samples 5
}

Write-Host "`nDone. Leave DRAW_WS_URL empty in local.properties for co-located WS." -ForegroundColor Green
