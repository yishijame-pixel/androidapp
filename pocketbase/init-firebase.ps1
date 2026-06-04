#Requires -Version 5.1
param(
    [string]$ProjectId = "",
    [string]$DisplayName = "FunLife Push"
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$Secrets = Join-Path $Root 'secrets'
$AppJson = Join-Path $Root '..\app\google-services.json'
$AdminSdk = Join-Path $Secrets 'firebase-adminsdk.json'
$ToolsDir = Join-Path $Root 'tools\firebase-cli'
$Firebase = Join-Path $ToolsDir 'node_modules\.bin\firebase.cmd'
if (Get-Command firebase -ErrorAction SilentlyContinue) {
    $Firebase = "firebase"
}

New-Item -ItemType Directory -Force -Path $Secrets | Out-Null

function Write-Step($msg) { Write-Host "`n>> $msg" -ForegroundColor Cyan }

if (-not (Test-Path $Firebase)) {
    Write-Step "Installing Firebase CLI"
    New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
    Push-Location $ToolsDir
    if (-not (Test-Path 'package.json')) { npm init -y 2>$null | Out-Null }
    npm install firebase-tools@11.30.0 2>&1 | Out-Host
    Pop-Location
}

Write-Step "Firebase login (browser will open or show a link)"
if ($env:FIREBASE_TOKEN) {
    Write-Host "Using FIREBASE_TOKEN env var (skip login)" -ForegroundColor Yellow
} else {
    & $Firebase login --no-localhost
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Login failed. Run: cd d:\soft\pocketbase; .\init-firebase-interactive.ps1" -ForegroundColor Red
        exit 1
    }
}

if (-not $ProjectId) {
    $ProjectId = "funlife-push-" + (Get-Random -Maximum 999999)
}

Write-Step "Create Firebase project: $ProjectId"
& $Firebase projects:create $ProjectId --display-name $DisplayName
if ($LASTEXITCODE -ne 0) {
    Write-Host "Project may already exist, continuing with $ProjectId ..." -ForegroundColor Yellow
}

Write-Step "Register Android app com.example.funlife"
$appOut = & $Firebase apps:create ANDROID com.example.funlife --project $ProjectId 2>&1 | Out-String
Write-Host $appOut
$appId = ""
if ($appOut -match "App ID:\s*(\S+)") { $appId = $Matches[1] }
if (-not $appId -and $appOut -match "1:\d+:android:[a-f0-9]+") { $appId = $Matches[0] }
if (-not $appId) {
    $listOut = & $Firebase apps:list ANDROID --project $ProjectId --json 2>&1 | Out-String
    if ($listOut -match '"appId"\s*:\s*"([^"]+)"') { $appId = $Matches[1] }
}
if (-not $appId) {
    Write-Host "Cannot parse App ID. Download google-services.json from Firebase Console." -ForegroundColor Red
    Start-Process "https://console.firebase.google.com/project/$ProjectId/settings/general"
    exit 1
}

Write-Step "Download google-services.json -> app/"
& $Firebase apps:sdkconfig ANDROID $appId --project $ProjectId -o $AppJson
if (-not (Test-Path $AppJson)) {
    Copy-Item (Join-Path $Secrets 'google-services.json') $AppJson -ErrorAction SilentlyContinue
}
if (-not (Test-Path $AppJson)) {
    Write-Host "google-services.json missing. Download from Console into app/" -ForegroundColor Red
    Start-Process "https://console.firebase.google.com/project/$ProjectId/settings/general"
    exit 1
}
Write-Host "OK: $AppJson" -ForegroundColor Green

Write-Step "Service account key (manual download once)"
$saUrl = "https://console.firebase.google.com/project/$ProjectId/settings/serviceaccounts/adminsdk"
Write-Host "Browser: Generate new private key, save as:" -ForegroundColor Yellow
Write-Host "  $AdminSdk" -ForegroundColor Yellow
Start-Process $saUrl

$deadline = (Get-Date).AddMinutes(10)
while (-not (Test-Path $AdminSdk)) {
    if ((Get-Date) -gt $deadline) {
        Write-Host "Timeout. Save JSON as secrets/firebase-adminsdk.json then run setup-push.ps1" -ForegroundColor Red
        exit 1
    }
    $minsLeft = [math]::Max(0, [int]($deadline - (Get-Date)).TotalMinutes)
    Write-Host "Waiting for firebase-adminsdk.json ... ($minsLeft min left)" -ForegroundColor DarkGray
    Start-Sleep -Seconds 5
}
Write-Host "OK: $AdminSdk" -ForegroundColor Green

@{
    projectId = $ProjectId
    appId     = $appId
} | ConvertTo-Json | Set-Content (Join-Path $Secrets 'firebase-project.json') -Encoding UTF8

Write-Host "`nFirebase init done. Next: .\setup-push.ps1 -RelayOnly -SkipInstall" -ForegroundColor Green
