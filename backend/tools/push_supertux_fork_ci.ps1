# Push main to GitHub and trigger supertux-fork CI workflows.
# Usage:
#   powershell -File backend/tools/push_supertux_fork_ci.ps1
#   powershell -File backend/tools/push_supertux_fork_ci.ps1 -Proxy http://127.0.0.1:7890

param(
    [string]$Proxy = $(if ($env:HTTPS_PROXY) { $env:HTTPS_PROXY } else { "http://127.0.0.1:7897" })
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location $RepoRoot

if ($Proxy) {
    $env:HTTPS_PROXY = $Proxy
    $env:HTTP_PROXY = $Proxy
    Write-Host "Using proxy: $Proxy" -ForegroundColor Cyan
}

$ahead = git rev-list --count origin/main..HEAD 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "WARN: cannot compare with origin/main (first push?)" -ForegroundColor Yellow
} elseif ([int]$ahead -eq 0) {
    Write-Host "Nothing to push (already up to date with origin/main)." -ForegroundColor Green
    exit 0
} else {
    Write-Host "Commits to push: $ahead" -ForegroundColor Cyan
    git log -1 --oneline
}

Write-Host "`nPushing to origin main..." -ForegroundColor Cyan
git push -u origin main
if ($LASTEXITCODE -ne 0) {
    Write-Host @"

PUSH FAILED — this machine cannot reach github.com:443.

Fix options:
  1. Enable VPN / system proxy, then re-run with:
       powershell -File backend/tools/push_supertux_fork_ci.ps1 -Proxy http://127.0.0.1:YOUR_PORT
  2. Push from another PC that can access GitHub (clone repo, pull, push).
  3. Use GitHub Desktop / mobile hotspot if corporate firewall blocks GitHub.

After push succeeds, open:
  https://github.com/yishijame-pixel/androidapp/actions
  - supertux-fork-linux
  - supertux-fork-android
"@ -ForegroundColor Red
    exit 1
}

Write-Host @"

Push OK. Watch CI:
  https://github.com/yishijame-pixel/androidapp/actions/workflows/supertux-fork-linux.yml
  https://github.com/yishijame-pixel/androidapp/actions/workflows/supertux-fork-android.yml

Artifacts (Android job): supertux-fork-apk-arm64-debug, libsupertux2-arm64-v8a
"@ -ForegroundColor Green
