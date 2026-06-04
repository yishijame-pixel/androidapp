#Requires -Version 5.1
<#
.SYNOPSIS
  安装 cloudflared（winget）并完成 pb.yishi.site 隧道配置。
  首次运行需在弹出窗口中完成 Cloudflare 浏览器授权。
#>
param([switch]$AfterLogin)
$ErrorActionPreference = "Stop"

function Refresh-Path {
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
        [System.Environment]::GetEnvironmentVariable("Path", "User")
}

Refresh-Path
if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
    Write-Host ">> 安装 cloudflared (winget)..." -ForegroundColor Cyan
    winget install --id Cloudflare.cloudflared -e --accept-source-agreements --accept-package-agreements
    Refresh-Path
}

if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
    Write-Host "cloudflared 安装失败，请手动安装后重试" -ForegroundColor Red
    exit 1
}

Write-Host "cloudflared $(cloudflared --version)" -ForegroundColor Green

$cert = Join-Path $env:USERPROFILE ".cloudflared\cert.pem"
if (-not (Test-Path $cert)) {
    Write-Host @"

需要一次 Cloudflare 浏览器授权（仅首次）：
  1. 将弹出 PowerShell 窗口
  2. 浏览器选择 yishi.site 并授权
  3. 窗口会自动继续创建隧道

"@ -ForegroundColor Yellow
    Start-Process powershell -ArgumentList '-NoExit', '-ExecutionPolicy', 'Bypass', '-File', $PSCommandPath, '-AfterLogin'
    exit 0
}

if ($AfterLogin) {
    & (Join-Path $PSScriptRoot "setup-tunnel-yishi.ps1")
    exit $LASTEXITCODE
}

& (Join-Path $PSScriptRoot "setup-tunnel-yishi.ps1")
