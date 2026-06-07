# Draw-guess full regression: PocketBase cold path + WebSocket hot path
param(
    [string]$BaseUrl = $env:POCKETBASE_URL,
    [string]$WsUrl = $env:DRAW_WS_URL
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not $BaseUrl) { $BaseUrl = "http://127.0.0.1:8090" }
if (-not $WsUrl) { $WsUrl = "ws://127.0.0.1:8790/ws" }

Write-Host "=== Draw Guess Regression ===" -ForegroundColor Cyan
Write-Host "PocketBase: $BaseUrl"
Write-Host "draw_ws:    $WsUrl"
Write-Host ""

$drawWsDir = Join-Path $PSScriptRoot "tools\draw_ws"
if (-not (Test-Path (Join-Path $drawWsDir "node_modules\ws"))) {
    Write-Host ">> Installing draw_ws deps..." -ForegroundColor Yellow
    Push-Location $drawWsDir
    npm install --silent
    Pop-Location
}

$healthUrl = $WsUrl -replace "^wss://", "https://" -replace "^ws://", "http://"
$healthUrl = ($healthUrl -replace "/ws$", "") + "/health"
$wsRunning = $false
try {
    $resp = Invoke-WebRequest -Uri $healthUrl -TimeoutSec 3 -UseBasicParsing
    if ($resp.StatusCode -eq 200) {
        $wsRunning = $true
        Write-Host "draw_ws running: $healthUrl" -ForegroundColor Green
    }
} catch {
    Write-Host "draw_ws not running, trying to start..." -ForegroundColor Yellow
    $env:PB_BASE_URL = $BaseUrl
    $env:PORT = "8790"
    Start-Process -FilePath "node" -ArgumentList "server.js" -WorkingDirectory $drawWsDir -WindowStyle Hidden
    Start-Sleep -Seconds 2
    try {
        $resp = Invoke-WebRequest -Uri $healthUrl -TimeoutSec 5 -UseBasicParsing
        if ($resp.StatusCode -eq 200) { $wsRunning = $true }
    } catch { }
}

if (-not $wsRunning) {
    Write-Host "WARN: draw_ws not available, WS tests will fail" -ForegroundColor Red
    Write-Host "Start manually: cd pocketbase\tools\draw_ws; `$env:PB_BASE_URL='$BaseUrl'; npm start"
}

$scriptFiles = @(
    "tools\test_draw_guess_sync_e2e.js",
    "tools\test_draw_ws_e2e.js",
    "tools\test_draw_guess_sync_benchmark.js"
)
$scriptNames = @(
    "PB cold path (game_moves / hooks)",
    "WS hot path (stroke_chunk / replay)",
    "Sync latency benchmark (v1 vs v2)"
)

$failed = 0
for ($i = 0; $i -lt $scriptFiles.Count; $i++) {
    $file = $scriptFiles[$i]
    $name = $scriptNames[$i]
    Write-Host ""
    Write-Host ">> $name" -ForegroundColor Yellow
    Write-Host "   $file" -ForegroundColor DarkGray
    $env:POCKETBASE_URL = $BaseUrl
    $env:DRAW_WS_URL = $WsUrl
    & node ".\$file" --base-url $BaseUrl --ws-url $WsUrl
    if ($LASTEXITCODE -ne 0) { $failed++ }
}

Write-Host ""
$total = $scriptFiles.Count
if ($failed -eq 0) {
    Write-Host "ALL PASS ($total suites)" -ForegroundColor Green
    exit 0
} else {
    Write-Host "FAILED: $failed / $total suites" -ForegroundColor Red
    exit 1
}
