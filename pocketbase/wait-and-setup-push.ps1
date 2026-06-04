#Requires -Version 5.1
# Wait for Firebase files then run setup-push.ps1
$Root = $PSScriptRoot
$AppJson = Join-Path $Root '..\app\google-services.json'
$AdminSdk = Join-Path $Root 'secrets\firebase-adminsdk.json'
$SecretsGs = Join-Path $Root 'secrets\google-services.json'

Write-Host "Waiting for Firebase files..." -ForegroundColor Cyan
Write-Host "  app/google-services.json OR secrets/google-services.json"
Write-Host "  secrets/firebase-adminsdk.json"
Write-Host "Complete init-firebase-interactive.ps1 in another window.`n"

$deadline = (Get-Date).AddMinutes(15)
while ((Get-Date) -lt $deadline) {
    $hasGs = (Test-Path $AppJson) -or (Test-Path $SecretsGs)
    $hasAdmin = (Test-Path $AdminSdk) -or (Get-ChildItem (Join-Path $Root 'secrets') -Filter '*adminsdk*.json' -ErrorAction SilentlyContinue)
    if ($hasGs -and $hasAdmin) {
        Write-Host "`nFiles found. Running setup-push.ps1 ..." -ForegroundColor Green
        & (Join-Path $Root 'setup-push.ps1') -RelayOnly -SkipInstall
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        Write-Host "`nBuilding and installing App (FCM_ENABLED=true)..." -ForegroundColor Cyan
        Push-Location (Split-Path $Root -Parent)
        & .\gradlew.bat installDebug 2>&1 | Out-Host
        Pop-Location
        Write-Host "`nRestart public stack:" -ForegroundColor Yellow
        Write-Host "  cd d:\soft\pocketbase; .\start-public.ps1" -ForegroundColor Yellow
        exit $LASTEXITCODE
    }
    Start-Sleep -Seconds 8
}
Write-Host "Timeout. When files are ready run: .\setup-push.ps1 -RelayOnly -SkipInstall" -ForegroundColor Yellow
