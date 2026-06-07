#Requires -Version 5.1
# draw_ws 守护：8790 不可用时自动拉起（配合 cloudflared /draw-ws 路由）
param(
    [string]$PbBase = "http://127.0.0.1:8090",
    [int]$Port = 8790,
    [int]$IntervalSec = 20,
)

$drawDir = $PSScriptRoot
$logFile = Join-Path $drawDir "watch-draw-ws.log"

function Write-Log([string]$msg) {
    $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $msg"
    Add-Content -Path $logFile -Value $line -Encoding UTF8
}

function Test-DrawWsHealth {
    try {
        $h = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/health" -TimeoutSec 3
        return ($null -ne $h -and $h.ok -eq $true)
    } catch {
        return $false
    }
}

function Start-DrawWsProcess {
    $pid8790 = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty OwningProcess
    if ($pid8790) {
        Stop-Process -Id $pid8790 -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
    }
    $cmd = "`$env:PB_BASE_URL='$PbBase'; `$env:PORT='$Port'; Set-Location '$drawDir'; node server.js"
    Start-Process powershell -ArgumentList "-NoWindow", "-Command", $cmd -WindowStyle Hidden
    for ($i = 1; $i -le 8; $i++) {
        Start-Sleep -Seconds 1
        if (Test-DrawWsHealth) { return $true }
    }
    return $false
}

Write-Log "watchdog started pb=$PbBase port=$Port interval=${IntervalSec}s"

while ($true) {
    if (-not (Test-DrawWsHealth)) {
        Write-Log "draw_ws down, restarting..."
        if (Start-DrawWsProcess) {
            Write-Log "draw_ws restarted OK"
        } else {
            Write-Log "draw_ws restart FAILED"
        }
    }
    Start-Sleep -Seconds $IntervalSec
}
