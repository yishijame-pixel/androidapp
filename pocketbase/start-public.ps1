#Requires -Version 5.1
# 启动 PocketBase + Cloudflare 隧道（pb.yishi.site）
$ErrorActionPreference = "Stop"
$env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
    [System.Environment]::GetEnvironmentVariable("Path", "User")

$pbDir = $PSScriptRoot
$pushEnv = Join-Path $pbDir 'secrets\push.env'
if (Test-Path $pushEnv) {
    Get-Content $pushEnv | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            $name = $Matches[1].Trim()
            $val = $Matches[2].Trim()
            if ($name -eq "FCM_RELAY_URL" -or $name -eq "FCM_RELAY_KEY") {
                Set-Item -Path "env:$name" -Value $val
            }
        }
    }
    Write-Host "Loaded FCM relay env from secrets/push.env" -ForegroundColor DarkGray
}

if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
    Write-Host "Run .\install-cloudflared.ps1 first" -ForegroundColor Red
    exit 1
}

$adminSdk = Join-Path $pbDir 'secrets\firebase-adminsdk.json'
$relayDir = Join-Path $pbDir 'tools\fcm_relay'
if ((Test-Path $adminSdk) -and (Test-Path (Join-Path $relayDir 'server.js'))) {
    $relayKey = $env:FCM_RELAY_KEY
    if (-not $relayKey -and (Test-Path (Join-Path $pbDir 'secrets\fcm-relay.key'))) {
        $relayKey = (Get-Content (Join-Path $pbDir 'secrets\fcm-relay.key') -Raw).Trim()
        $env:FCM_RELAY_KEY = $relayKey
    }
    if ($relayKey) {
        $portPid = (Get-NetTCPConnection -LocalPort 8787 -State Listen -ErrorAction SilentlyContinue |
            Select-Object -First 1).OwningProcess
        if ($portPid) {
            Stop-Process -Id $portPid -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 1
        }
        $relayCmd = @"
`$env:FCM_SERVICE_ACCOUNT='$adminSdk'; `$env:FCM_RELAY_KEY='$relayKey'; `$env:PORT='8787'; Set-Location '$relayDir'; node server.js
"@
        Start-Process powershell -ArgumentList "-NoExit", "-Command", $relayCmd -WindowStyle Minimized
        $relayOk = $false
        for ($i = 1; $i -le 8; $i++) {
            Start-Sleep -Seconds 1
            try {
                $h = Invoke-RestMethod -Uri "http://127.0.0.1:8787/health" -TimeoutSec 2
                if ($h.ok) { $relayOk = $true; break }
            } catch { }
        }
        if ($relayOk) {
            Write-Host "Started FCM relay :8787 (health OK)" -ForegroundColor Green
        } else {
            Write-Host "FCM relay :8787 started but health not ready (push may skip)" -ForegroundColor Yellow
        }
    }
}

$drawWsDir = Join-Path $pbDir 'tools\draw_ws'
if (Test-Path (Join-Path $drawWsDir 'server.js')) {
    $portPid8790 = (Get-NetTCPConnection -LocalPort 8790 -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1).OwningProcess
    if ($portPid8790) {
        Stop-Process -Id $portPid8790 -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
    }
    $pbUrl = if ($env:PB_BASE_URL) { $env:PB_BASE_URL } else { "http://127.0.0.1:8090" }
    $drawWsCmd = @"
`$env:PB_BASE_URL='$pbUrl'; `$env:PORT='8790'; Set-Location '$drawWsDir'; node server.js
"@
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $drawWsCmd -WindowStyle Minimized
    Write-Host "Started draw_ws :8790 (pb=$pbUrl)" -ForegroundColor Green
}

$pbEnv = @{}
if ($env:FCM_RELAY_URL) { $pbEnv["FCM_RELAY_URL"] = $env:FCM_RELAY_URL }
if ($env:FCM_RELAY_KEY) { $pbEnv["FCM_RELAY_KEY"] = $env:FCM_RELAY_KEY }

$pbExe = Join-Path $pbDir 'pocketbase.exe'
if ($pbEnv.Count -gt 0) {
    $pbCmd = @"
Set-Location '$pbDir'
`$env:FCM_RELAY_URL='$($pbEnv.FCM_RELAY_URL)'
`$env:FCM_RELAY_KEY='$($pbEnv.FCM_RELAY_KEY)'
& '$pbExe' serve --http=0.0.0.0:8090
"@
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $pbCmd -WindowStyle Normal
} else {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$pbDir'; & '$pbExe' serve --http=0.0.0.0:8090" -WindowStyle Normal
}
Start-Sleep -Seconds 2
Start-Process -FilePath "cloudflared" -ArgumentList "tunnel", "run", "funlife-pb" -WindowStyle Minimized

Write-Host "Started PocketBase :8090 + tunnel funlife-pb" -ForegroundColor Green
Write-Host "Verify: https://pb.yishi.site/api/health" -ForegroundColor Cyan
Write-Host "Draw WS: https://draw.yishi.site/health  (wss://draw.yishi.site/ws)" -ForegroundColor Cyan
Write-Host "Stop: taskkill /IM pocketbase.exe /F; taskkill /IM cloudflared.exe /F" -ForegroundColor DarkGray
