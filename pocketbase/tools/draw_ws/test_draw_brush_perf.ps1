# 你画我猜 · 画家画笔性能验证脚本
# 验证 chunk 不再触发高频 publishUi（画家侧 chunk flush 有日志，但不应伴随 drawStrokes 全树刷新）
#
# 用法:
#   .\pocketbase\tools\draw_ws\test_draw_brush_perf.ps1
#   .\pocketbase\tools\draw_ws\test_draw_brush_perf.ps1 -SkipBuild -DurationSec 30

param(
    [switch]$SkipBuild,
    [int]$DurationSec = 20,
    [string]$Package = "com.example.funlife"
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
Set-Location $Root

Write-Host "=== Draw Guess Brush Perf Test ===" -ForegroundColor Cyan
Write-Host "Root: $Root"

if (-not $SkipBuild) {
    Write-Host "`n[1/4] compileDebugKotlin + unit tests..." -ForegroundColor Yellow
    .\gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.example.funlife.ui.screens.socialgame.play.DrawGuessInkBufferTest" 2>&1 | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build or unit tests failed"
    }
} else {
    Write-Host "`n[1/4] skip build (-SkipBuild)" -ForegroundColor DarkGray
}

Write-Host "`n[2/4] check adb device..." -ForegroundColor Yellow
$devices = adb devices | Select-String "device$"
if (-not $devices) {
    Write-Warning "No adb device. Install APK manually and run log section below."
    Write-Host @"

Manual log capture:
  adb logcat -c
  adb logcat -s DrawGuessCanvas DrawGuessLiveSync | Tee-Object brush_perf.log
  # 在 App 里作画家侧拖动 10 秒

Expected AFTER perf fix:
  - chunk flush lines ~4ms throttle (OK)
  - NO burst of 'ingest ok' during active drag
  - painter drag feels smooth

"@ -ForegroundColor Green
    exit 0
}

Write-Host "`n[3/4] install debug apk..." -ForegroundColor Yellow
.\gradlew :app:installDebug 2>&1 | Select-Object -Last 8
if ($LASTEXITCODE -ne 0) {
    Write-Error "installDebug failed"
}

Write-Host "`n[4/4] capture logcat ${DurationSec}s (start drawing on device NOW)..." -ForegroundColor Yellow
adb logcat -c | Out-Null
$logFile = Join-Path $Root "pocketbase\tools\reports\draw_brush_perf_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"
New-Item -ItemType Directory -Force -Path (Split-Path $logFile) | Out-Null

$job = Start-Job -ScriptBlock {
    param($dur, $out)
    adb logcat -s DrawGuessCanvas DrawGuessLiveSync GamePlaySync 2>&1 |
        ForEach-Object -Begin { $sw = [Diagnostics.Stopwatch]::StartNew() } -Process {
            if ($sw.Elapsed.TotalSeconds -ge $dur) { return }
            $_
        } | Set-Content -Path $out -Encoding utf8
} -ArgumentList $DurationSec, $logFile

Write-Host "Logging to $logFile ... draw strokes in the app for $DurationSec seconds" -ForegroundColor Cyan
Wait-Job $job -Timeout ($DurationSec + 15) | Out-Null
Stop-Job $job -ErrorAction SilentlyContinue
Remove-Job $job -Force -ErrorAction SilentlyContinue

if (-not (Test-Path $logFile)) {
    Write-Warning "No log file captured"
    exit 1
}

$lines = Get-Content $logFile
$chunkFlush = ($lines | Select-String "chunk flush").Count
$ingestOk = ($lines | Select-String "ingest ok move").Count
$ingestDeferPreview = ($lines | Select-String "ingest defer canvas preview").Count
$txChunk = ($lines | Select-String "tx chunk").Count

Write-Host "`n=== Results ===" -ForegroundColor Cyan
Write-Host "  chunk flush (VM):     $chunkFlush"
Write-Host "  tx chunk (WS):        $txChunk"
Write-Host "  ingest ok (canvas):   $ingestOk"
Write-Host "  ingest defer preview: $ingestDeferPreview"
Write-Host "  log file:             $logFile"

$pass = $true
if ($chunkFlush -gt ($DurationSec * 80)) {
    Write-Warning "chunk flush rate very high (>80/s) — check throttle"
}
if ($ingestOk -gt ($DurationSec * 5)) {
    Write-Warning "ingest ok during drag too high — publishUi may still run on chunk path"
    $pass = $false
}

if ($pass) {
    Write-Host "`nPASS (heuristic): low ingest churn during draw window" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nFAIL (heuristic): review GamePlayViewModel publish paths" -ForegroundColor Red
    exit 1
}
