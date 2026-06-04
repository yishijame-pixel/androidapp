#Requires -Version 5.1
# Manual Firebase setup: open all pages in browser, wait for downloaded files.
$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$Secrets = Join-Path $Root 'secrets'
$AppJson = Join-Path $Root '..\app\google-services.json'
$AdminSdk = Join-Path $Secrets 'firebase-adminsdk.json'

New-Item -ItemType Directory -Force -Path $Secrets | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path $AppJson -Parent) | Out-Null

Write-Host ""
Write-Host "FunLife Firebase manual setup" -ForegroundColor Cyan
Write-Host "Opening Firebase Console in your browser..." -ForegroundColor Yellow
Write-Host ""

Start-Process "https://console.firebase.google.com/"
Start-Sleep -Seconds 2
Start-Process "https://console.firebase.google.com/u/0/project/_/overview"

Write-Host "In the browser, do these steps:" -ForegroundColor Green
Write-Host "  1. Create project (any name, e.g. FunLife Push)"
Write-Host "  2. Add Android app, package name: com.example.funlife"
Write-Host "  3. Download google-services.json"
Write-Host "  4. Save it as:"
Write-Host "     $AppJson" -ForegroundColor White
Write-Host ""
Write-Host "  5. Project settings -> Service accounts -> Generate new private key"
Write-Host "  6. Save JSON as:"
Write-Host "     $AdminSdk" -ForegroundColor White
Write-Host ""

$projectId = Read-Host "After you create the project, paste Firebase Project ID here (or press Enter to skip)"
if ($projectId) {
    $projectId = $projectId.Trim()
    Start-Process "https://console.firebase.google.com/project/$projectId/settings/general"
    Start-Sleep -Seconds 1
    Start-Process "https://console.firebase.google.com/project/$projectId/settings/serviceaccounts/adminsdk"
    Write-Host "Opened project settings + service account pages." -ForegroundColor Green
}

Write-Host ""
Write-Host "Waiting for both files (up to 20 minutes)..." -ForegroundColor Cyan
$deadline = (Get-Date).AddMinutes(20)
while ((Get-Date) -lt $deadline) {
    $hasGs = Test-Path $AppJson
    $hasAdmin = (Test-Path $AdminSdk) -or (Get-ChildItem $Secrets -Filter '*adminsdk*.json' -ErrorAction SilentlyContinue)
    if ($hasGs -and $hasAdmin) {
        if (-not (Test-Path $AdminSdk)) {
            $found = Get-ChildItem $Secrets -Filter '*adminsdk*.json' | Select-Object -First 1
            Copy-Item $found.FullName $AdminSdk -Force
        }
        Write-Host ""
        Write-Host "Both files found. Running setup-push.ps1 ..." -ForegroundColor Green
        & (Join-Path $Root 'setup-push.ps1') -RelayOnly -SkipInstall
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        Write-Host ""
        Write-Host "Building and installing App..." -ForegroundColor Cyan
        Push-Location (Split-Path $Root -Parent)
        & .\gradlew.bat installDebug 2>&1 | Out-Host
        Pop-Location
        Write-Host ""
        Write-Host "Done. Restart public stack:" -ForegroundColor Green
        Write-Host "  cd d:\soft\pocketbase; .\start-public.ps1"
        Read-Host "Press Enter to close"
        exit 0
    }
    $gs = if ($hasGs) { "OK" } else { "..." }
    $ad = if ($hasAdmin) { "OK" } else { "..." }
    Write-Host "  google-services.json [$gs]  adminsdk [$ad]" -ForegroundColor DarkGray
    Start-Sleep -Seconds 5
}

Write-Host "Timeout. Put files in place then run:" -ForegroundColor Yellow
Write-Host "  .\setup-push.ps1 -RelayOnly -SkipInstall"
Read-Host "Press Enter to close"
