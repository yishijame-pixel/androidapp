# Deploy pb_hooks to local PocketBase and restart public tunnel (pb.yishi.site)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "=== FunLife PocketBase Hook Deploy ===" -ForegroundColor Cyan
Write-Host "Hooks: pb_hooks/main.pb.js" -ForegroundColor DarkGray

if (-not (Test-Path ".\pb_hooks\main.pb.js")) {
    Write-Host "Missing pb_hooks/main.pb.js" -ForegroundColor Red
    exit 1
}

Get-Process pocketbase -ErrorAction SilentlyContinue | Stop-Process -Force
Get-Process cloudflared -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 2

& "$PSScriptRoot\start-public.ps1"

$healthOk = $false
for ($i = 1; $i -le 12; $i++) {
    Start-Sleep -Seconds 5
    try {
        $health = Invoke-RestMethod -Uri "https://pb.yishi.site/api/health" -TimeoutSec 15
        $healthOk = $true
        break
    } catch {
        Write-Host "Remote health attempt $i failed, retrying..." -ForegroundColor DarkYellow
    }
}
if ($healthOk) {
    Write-Host "Remote health: $($health | ConvertTo-Json -Compress)" -ForegroundColor Green
} else {
    Write-Host "Remote health check failed after retries" -ForegroundColor Yellow
    try {
        $local = Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/health" -TimeoutSec 5
        Write-Host "Local health OK: $($local | ConvertTo-Json -Compress)" -ForegroundColor Yellow
    } catch {
        Write-Host "Local health also failed" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "Running E2E tests..." -ForegroundColor Cyan
node "$PSScriptRoot\tools\test_push_game_invite_hook.js" --base-url https://pb.yishi.site
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
node "$PSScriptRoot\tools\test_draw_guess_invite_start_e2e.js" --base-url https://pb.yishi.site
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
node "$PSScriptRoot\tools\test_draw_guess_sync_e2e.js" --base-url https://pb.yishi.site
exit $LASTEXITCODE
