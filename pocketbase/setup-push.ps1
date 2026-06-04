#Requires -Version 5.1
param(
    [switch]$SkipInstall,
    [switch]$InitFirebase,
    [switch]$RelayOnly
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$RepoRoot = Split-Path $Root -Parent
$Secrets = Join-Path $Root 'secrets'
$PushEnv = Join-Path $Secrets 'push.env'
$RelayDir = Join-Path $Root 'tools\fcm_relay'
$AppJson = Join-Path $RepoRoot 'app\google-services.json'
$AdminSdk = Join-Path $Secrets 'firebase-adminsdk.json'

function Write-Step($msg) { Write-Host "`n>> $msg" -ForegroundColor Cyan }

New-Item -ItemType Directory -Force -Path $Secrets | Out-Null

if ($InitFirebase) {
    & (Join-Path $Root 'init-firebase.ps1')
}

$secretsGs = Join-Path $Secrets 'google-services.json'
if (-not (Test-Path $AppJson) -and (Test-Path $secretsGs)) {
    Copy-Item $secretsGs $AppJson -Force
}
if (-not (Test-Path $AppJson)) {
    Write-Host "Missing app/google-services.json" -ForegroundColor Red
    Write-Host "Run init-firebase-manual.ps1 or download from Firebase Console" -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $AdminSdk)) {
    $found = Get-ChildItem -Path $Secrets -Filter '*adminsdk*.json' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        Copy-Item $found.FullName $AdminSdk -Force
    }
}
if (-not (Test-Path $AdminSdk)) {
    Write-Host "Missing secrets/firebase-adminsdk.json" -ForegroundColor Red
    Write-Host "Save service account key to: $AdminSdk" -ForegroundColor Yellow
    exit 1
}

$keyFile = Join-Path $Secrets 'fcm-relay.key'
if (-not (Test-Path $keyFile)) {
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $key = [Convert]::ToBase64String($bytes) -replace '[+/=]', 'x'
    Set-Content -Path $keyFile -Value $key -NoNewline -Encoding ASCII
}
$RelayKey = (Get-Content $keyFile -Raw).Trim()

@"
FCM_RELAY_URL=http://127.0.0.1:8787/push
FCM_RELAY_KEY=$RelayKey
FCM_SERVICE_ACCOUNT=$AdminSdk
"@ | Set-Content $PushEnv -Encoding UTF8

Write-Step "Install FCM relay dependencies"
Push-Location $RelayDir
npm install 2>&1 | Out-Host
Pop-Location

function Stop-Port($port) {
    Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}
Stop-Port 8787
if (-not $RelayOnly) { Stop-Port 8090 }
Start-Sleep -Seconds 1

Write-Step "Start FCM relay on :8787"
$relayLog = Join-Path $Secrets 'fcm-relay.log'
$relayJob = Start-Job -ScriptBlock {
    param($dir, $adminSdk, $key, $log)
    Set-Location $dir
    $env:FCM_SERVICE_ACCOUNT = $adminSdk
    $env:FCM_RELAY_KEY = $key
    $env:PORT = '8787'
    node server.js *>&1 | Tee-Object -FilePath $log
} -ArgumentList $RelayDir, $AdminSdk, $RelayKey, $relayLog

Start-Sleep -Seconds 3
try {
    $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8787/health' -TimeoutSec 5
    if (-not $health.ok) { throw 'health not ok' }
    Write-Host "FCM relay OK" -ForegroundColor Green
} catch {
    Write-Host "FCM relay failed. See $relayLog" -ForegroundColor Red
    Receive-Job $relayJob -Keep | Select-Object -Last 20
    exit 1
}

$pbJob = $null
$pbLog = Join-Path $Secrets 'pocketbase-push.log'
if (-not $RelayOnly) {
    Write-Step "Start PocketBase :8090 with FCM hooks"
    $pbExe = Join-Path $Root 'pocketbase.exe'
    if (-not (Test-Path $pbExe)) {
        Write-Host "Missing pocketbase.exe in pocketbase/" -ForegroundColor Red
        exit 1
    }

    $pbJob = Start-Job -ScriptBlock {
        param($root, $relayUrl, $relayKey, $log)
        Set-Location $root
        $env:FCM_RELAY_URL = $relayUrl
        $env:FCM_RELAY_KEY = $relayKey
        & (Join-Path $root 'pocketbase.exe') serve --http=0.0.0.0:8090 *>&1 | Tee-Object -FilePath $log
    } -ArgumentList $Root, 'http://127.0.0.1:8787/push', $RelayKey, $pbLog

    Start-Sleep -Seconds 2
    try {
        Invoke-WebRequest -Uri 'http://127.0.0.1:8090/api/health' -UseBasicParsing -TimeoutSec 5 | Out-Null
        Write-Host "PocketBase OK -> http://127.0.0.1:8090" -ForegroundColor Green
    } catch {
        Write-Host "PocketBase starting or failed. See $pbLog" -ForegroundColor Yellow
    }
} else {
    Write-Host "RelayOnly: push.env written. Run start-public.ps1 next." -ForegroundColor Yellow
}

if (-not $SkipInstall -and -not $RelayOnly) {
    Write-Step "Build and install App (FCM_ENABLED=true)"
    Push-Location $RepoRoot
    & .\gradlew.bat installDebug 2>&1 | Out-Host
    Pop-Location
    if ($LASTEXITCODE -ne 0) {
        Write-Host "installDebug failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "App installed on connected device" -ForegroundColor Green
}

Write-Host @"

Push stack ready
  FCM relay    http://127.0.0.1:8787/health
  PocketBase   http://127.0.0.1:8090
  Config       $PushEnv
$(if ($RelayOnly) { "  Mode         RelayOnly`n" } else { "" })
Job IDs: relay=$($relayJob.Id)$(if ($pbJob) { " pocketbase=$($pbJob.Id)" } else { "" })
Logs: $relayLog$(if ($pbJob) { " , $pbLog" } else { "" })

Test: kill App on phone B -> A sends chat -> B gets notification
Next: cd d:\soft\pocketbase; .\start-public.ps1
Stop: Stop-Job $($relayJob.Id)$(if ($pbJob) { ",$($pbJob.Id)" } else { "" }); Get-Job | Remove-Job -Force
"@ -ForegroundColor Green
