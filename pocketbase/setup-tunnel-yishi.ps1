#Requires -Version 5.1
param(
    [string]$Domain = "yishi.site",
    [string]$Subdomain = "pb",
    [string]$TunnelName = "funlife-pb",
    [string]$TunnelUuid = "",
    [int]$LocalPort = 8090
)

$ErrorActionPreference = "Stop"
$Hostname = "$Subdomain.$Domain"
$CloudflaredDir = Join-Path $env:USERPROFILE ".cloudflared"
$ConfigPath = Join-Path $CloudflaredDir "config.yml"

function Write-Step($msg) { Write-Host "`n>> $msg" -ForegroundColor Cyan }

Write-Host "FunLife PocketBase -> https://$Hostname (local :$LocalPort)" -ForegroundColor Cyan

$cf = Get-Command cloudflared -ErrorAction SilentlyContinue
if (-not $cf) {
    Write-Host "cloudflared not found. Run install-cloudflared.ps1 first." -ForegroundColor Red
    exit 1
}

New-Item -ItemType Directory -Force -Path $CloudflaredDir | Out-Null

$cert = Join-Path $CloudflaredDir "cert.pem"
if (-not (Test-Path $cert)) {
    Write-Step "Cloudflare login (browser)"
    & cloudflared tunnel login
    if (-not (Test-Path $cert)) {
        Write-Host "Login failed." -ForegroundColor Red
        exit 1
    }
}

if (-not $TunnelUuid) {
    $existing = & cloudflared tunnel list 2>&1 | Out-String
    if ($existing -notmatch [regex]::Escape($TunnelName)) {
        Write-Step "Creating tunnel: $TunnelName"
        $createOut = & cloudflared tunnel create $TunnelName 2>&1 | Out-String
        Write-Host $createOut
    } else {
        Write-Host "Tunnel $TunnelName already exists, reusing." -ForegroundColor Yellow
    }
    $listOut = & cloudflared tunnel list 2>&1 | Out-String
    $uuidPattern = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
    if ($listOut -match $uuidPattern) {
        $TunnelUuid = $Matches[0]
    }
}

if (-not $TunnelUuid) {
    Write-Host "Cannot resolve tunnel UUID. Run: cloudflared tunnel list" -ForegroundColor Red
    exit 1
}

$CredFile = Join-Path $CloudflaredDir "$TunnelUuid.json"
if (-not (Test-Path $CredFile)) {
    Write-Host "Missing credentials: $CredFile" -ForegroundColor Red
    exit 1
}

Write-Step "Writing $ConfigPath"
$lines = @(
    "tunnel: $TunnelUuid"
    "credentials-file: $CredFile"
    ""
    "ingress:"
    "  - hostname: $Hostname"
    "    service: http://127.0.0.1:$LocalPort"
    "    originRequest:"
    "      connectTimeout: 30s"
    "      tcpKeepAlive: 30s"
    "      keepAliveTimeout: 90s"
    "      keepAliveConnections: 100"
    "      httpHostHeader: $Hostname"
    "  - service: http_status:404"
)
Set-Content -Path $ConfigPath -Value ($lines -join [Environment]::NewLine) -Encoding UTF8
Write-Host "OK" -ForegroundColor Green

Write-Step "DNS route: $Hostname"
& cloudflared tunnel route dns $TunnelName $Hostname 2>&1 | Out-Host

Write-Step "Set in local.properties:"
Write-Host "POCKETBASE_URL=https://$Hostname" -ForegroundColor Yellow
Write-Host "Terminal1: cd d:\soft\pocketbase; .\start.ps1" -ForegroundColor Green
Write-Host "Terminal2: cloudflared tunnel run $TunnelName" -ForegroundColor Green
Write-Host "Verify: https://$Hostname/api/health" -ForegroundColor Green
