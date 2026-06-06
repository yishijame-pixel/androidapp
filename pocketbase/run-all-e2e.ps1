# FunLife PocketBase E2E 一键回归
param(
    [string]$BaseUrl = $env:POCKETBASE_URL
)
if (-not $BaseUrl) { $BaseUrl = "http://127.0.0.1:8090" }

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "=== PocketBase E2E @ $BaseUrl ===" -ForegroundColor Cyan

$scripts = @(
    "tools\test_social_e2e.js",
    "tools\test_social_chat_e2e.js",
    "tools\test_game_room_e2e.js",
    "tools\test_game_invite_delivery_e2e.js",
    "tools\test_game_room_extended_e2e.js"
)

$failed = 0
foreach ($s in $scripts) {
    Write-Host "`n>> $s" -ForegroundColor Yellow
    $env:POCKETBASE_URL = $BaseUrl
    node ".\$s" --base-url $BaseUrl
    if ($LASTEXITCODE -ne 0) { $failed++ }
}

Write-Host ""
if ($failed -eq 0) {
    Write-Host "ALL PASS ($($scripts.Count) suites)" -ForegroundColor Green
    exit 0
} else {
    Write-Host "FAILED: $failed / $($scripts.Count) suites" -ForegroundColor Red
    exit 1
}
