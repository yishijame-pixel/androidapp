#Requires -Version 5.1
param(
    [string]$PbBase = "https://pb.yishi.site",
    [int]$DrawPort = 8790,
    [switch]$SkipTunnel
)

$ErrorActionPreference = "Stop"
$drawDir = $PSScriptRoot
$cfConfig = Join-Path $env:USERPROFILE ".cloudflared\config.yml"

function Write-Step($msg) { Write-Host ""; Write-Host ">> $msg" -ForegroundColor Cyan }

Write-Step "npm install draw_ws"
Set-Location $drawDir
if (-not (Test-Path "node_modules\ws")) {
    npm install --omit=dev 2>&1 | Out-Host
}

Write-Step "restart draw_ws on port $DrawPort"
$pbLocal = "http://127.0.0.1:8090"
$old = Get-NetTCPConnection -LocalPort $DrawPort -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1 -ExpandProperty OwningProcess
if ($old) {
    Stop-Process -Id $old -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}
$cmd = "`$env:PB_BASE_URL='$pbLocal'; `$env:PORT='$DrawPort'; Set-Location '$drawDir'; node server.js"
Start-Process powershell -ArgumentList "-NoWindow", "-Command", $cmd -WindowStyle Hidden

$health = $null
for ($i = 1; $i -le 10; $i++) {
    Start-Sleep -Seconds 1
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:$DrawPort/health" -TimeoutSec 3
        if ($health.ok) { break }
    } catch {
        $health = $null
    }
}
if (-not $health -or -not $health.ok) {
    Write-Host "[FAIL] local draw_ws health" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] local health $($health | ConvertTo-Json -Compress)" -ForegroundColor Green

if (-not $SkipTunnel) {
    Write-Step "update cloudflared path route"
    if (Test-Path $cfConfig) {
        $raw = Get-Content $cfConfig -Raw
        if ($raw -notmatch 'path:\s*\^/draw-ws') {
            $hostname = ([uri]$PbBase).Host
            $tunnelLine = ($raw -split "`n" | Where-Object { $_ -match '^\s*tunnel:' } | Select-Object -First 1).Trim()
            $credLine = ($raw -split "`n" | Where-Object { $_ -match '^\s*credentials-file:' } | Select-Object -First 1).Trim()
            if (-not $tunnelLine -or -not $credLine) {
                Write-Host "[FAIL] missing tunnel lines in config.yml" -ForegroundColor Red
                exit 1
            }
            $o = @(
                '      connectTimeout: 30s',
                '      tcpKeepAlive: 30s',
                '      keepAliveTimeout: 90s',
                '      keepAliveConnections: 100'
            )
            $lines = New-Object System.Collections.Generic.List[string]
            $lines.Add($tunnelLine)
            $lines.Add($credLine)
            $lines.Add('')
            $lines.Add('ingress:')
            $lines.Add("  - hostname: $hostname")
            $lines.Add('    path: ^/draw-ws')
            $lines.Add("    service: http://127.0.0.1:$DrawPort")
            $lines.Add('    originRequest:')
            foreach ($x in $o) { $lines.Add($x) }
            $lines.Add("      httpHostHeader: $hostname")
            $lines.Add("  - hostname: $hostname")
            $lines.Add('    service: http://127.0.0.1:8090')
            $lines.Add('    originRequest:')
            foreach ($x in $o) { $lines.Add($x) }
            $lines.Add("      httpHostHeader: $hostname")
            $lines.Add('  - hostname: draw.yishi.site')
            $lines.Add("    service: http://127.0.0.1:$DrawPort")
            $lines.Add('    originRequest:')
            foreach ($x in $o) { $lines.Add($x) }
            $lines.Add('      httpHostHeader: draw.yishi.site')
            $lines.Add('  - service: http_status:404')
            Set-Content -Path $cfConfig -Value ($lines.ToArray() -join [Environment]::NewLine) -Encoding UTF8
            Write-Host "[OK] wrote /draw-ws path route" -ForegroundColor Green
        } else {
            Write-Host "[OK] /draw-ws route already present" -ForegroundColor Green
        }
        $cfPid = Get-Process cloudflared -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Id
        if ($cfPid) {
            Stop-Process -Id $cfPid -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 2
        }
        Start-Process cloudflared -ArgumentList 'tunnel', 'run', 'funlife-pb' -WindowStyle Minimized
        Write-Host "[OK] cloudflared restarted" -ForegroundColor Green
        Start-Sleep -Seconds 5
    } else {
        Write-Host "[WARN] no cloudflared config, skip tunnel" -ForegroundColor Yellow
    }
}

Write-Step "public health check"
& (Join-Path $drawDir 'deploy-co-located.ps1') -PbBase $PbBase
