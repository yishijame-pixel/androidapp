#Requires -Version 5.1
# Push stack diagnostics
$Root = $PSScriptRoot
$Secrets = Join-Path $Root 'secrets'
$PushEnv = Join-Path $Secrets 'push.env'

Write-Host "`n=== FunLife Push Diagnostics ===" -ForegroundColor Cyan

# 1. FCM relay
try {
    $h = Invoke-RestMethod 'http://127.0.0.1:8787/health' -TimeoutSec 3
    Write-Host "[OK] FCM relay :8787" -ForegroundColor Green
} catch {
    Write-Host "[FAIL] FCM relay not running on :8787" -ForegroundColor Red
    Write-Host "       Run: .\setup-push.ps1 -RelayOnly -SkipInstall" -ForegroundColor Yellow
}

# 2. PocketBase
try {
    Invoke-WebRequest 'http://127.0.0.1:8090/api/health' -UseBasicParsing -TimeoutSec 3 | Out-Null
    Write-Host "[OK] PocketBase :8090" -ForegroundColor Green
} catch {
    Write-Host "[FAIL] PocketBase not running" -ForegroundColor Red
}

# 3. push.env
if (Test-Path $PushEnv) {
    Write-Host "[OK] push.env exists" -ForegroundColor Green
    Get-Content $PushEnv | ForEach-Object { Write-Host "     $_" -ForegroundColor DarkGray }
} else {
    Write-Host "[FAIL] missing secrets/push.env" -ForegroundColor Red
}

# 4. Firebase files
$gs = Join-Path $Root '..\app\google-services.json'
$admin = Join-Path $Secrets 'firebase-adminsdk.json'
Write-Host $(if (Test-Path $gs) { '[OK] google-services.json' } else { '[FAIL] google-services.json' }) -ForegroundColor $(if (Test-Path $gs) { 'Green' } else { 'Red' })
Write-Host $(if (Test-Path $admin) { '[OK] firebase-adminsdk.json' } else { '[FAIL] firebase-adminsdk.json' }) -ForegroundColor $(if (Test-Path $admin) { 'Green' } else { 'Red' })

# 5. User fcm_token in PocketBase
try {
    $auth = Invoke-RestMethod -Method POST -Uri 'http://127.0.0.1:8090/api/collections/_superusers/auth-with-password' -ContentType 'application/json' -Body '{"identity":"admin@funlife.local","password":"FunLifePB2026!"}'
    $token = $auth.token
    $users = Invoke-RestMethod -Uri 'http://127.0.0.1:8090/api/collections/users/records?perPage=50' -Headers @{ Authorization = "Bearer $token" }
    Write-Host "`nUser fcm_token status:" -ForegroundColor Cyan
    foreach ($u in $users.items) {
        $has = if ($u.fcm_token) { 'YES (' + $u.fcm_token.Substring(0, [Math]::Min(16, $u.fcm_token.Length)) + '...)' } else { 'EMPTY - open App and login' }
        $color = if ($u.fcm_token) { 'Green' } else { 'Yellow' }
        Write-Host "  $($u.funlife_username): $has" -ForegroundColor $color
    }
} catch {
    Write-Host "[WARN] Cannot query users: $_" -ForegroundColor Yellow
}

Write-Host "`nPhone checklist:" -ForegroundColor Cyan
Write-Host "  1. Open App -> login -> stay on home (not in chat)"
Write-Host "  2. Allow notification permission when prompted"
Write-Host "  3. Settings -> Apps -> FunLife -> Notifications -> enable all"
Write-Host "  4. Run diagnostics again; fcm_token should be YES"
Write-Host ""
