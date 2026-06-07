# Draw-guess canvas sync verification
# - JVM unit tests (fingerprint / append policy)
# - Optional adb logcat capture + heuristic analysis (rebuild storm, bad patterns)
#
# Usage:
#   .\pocketbase\tools\draw_ws\test_draw_canvas_sync.ps1
#   .\pocketbase\tools\draw_ws\test_draw_canvas_sync.ps1 -SkipBuild -DurationSec 30
#   .\pocketbase\tools\draw_ws\test_draw_canvas_sync.ps1 -AnalyzeOnly -LogFile C:\path\to\log.txt

param(
    [switch]$SkipBuild,
    [switch]$AnalyzeOnly,
    [int]$DurationSec = 25,
    [string]$LogFile = ""
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
Set-Location $Root

function Parse-LayerEvents {
    param([string[]]$Lines)
    $events = New-Object System.Collections.Generic.List[object]
    foreach ($line in $Lines) {
        if ($line -notmatch 'DrawGuessCanvas:') { continue }
        if ($line -match '(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}).*layer (rebuild|append|clear)(?: n=(\d+))?') {
            $tsText = $matches[1]
            $year = (Get-Date).Year
            $ts = [datetime]::ParseExact("$year-$tsText", "yyyy-MM-dd HH:mm:ss.fff", $null)
            $events.Add([pscustomobject]@{
                Time   = $ts
                Action = $matches[2]
                N      = if ($matches[3]) { [int]$matches[3] } else { 0 }
                Line   = $line.Trim()
            })
        }
    }
    return $events
}

function Analyze-DrawCanvasLog {
    param([string[]]$Lines)

    $issues = New-Object System.Collections.Generic.List[string]
    $layerEvents = Parse-LayerEvents -Lines $Lines

    $append = @($layerEvents | Where-Object { $_.Action -eq 'append' })
    $rebuild = @($layerEvents | Where-Object { $_.Action -eq 'rebuild' })
    $clear = @($layerEvents | Where-Object { $_.Action -eq 'clear' })

    # Same n: multiple rebuild within 500ms (rebuild storm)
    for ($i = 0; $i -lt $rebuild.Count; $i++) {
        $a = $rebuild[$i]
        $burst = @($rebuild | Where-Object {
            $_.N -eq $a.N -and ($_.Time - $a.Time).TotalMilliseconds -ge 0 -and
            ($_.Time - $a.Time).TotalMilliseconds -le 500
        })
        if ($burst.Count -ge 3) {
            $msg = "rebuild storm n=$($a.N): $($burst.Count) rebuilds in 500ms (flicker)"
            if ($issues -notcontains $msg) { $issues.Add($msg) }
        }
    }

    # Back-to-back rebuild same n within 150ms
    for ($i = 1; $i -lt $rebuild.Count; $i++) {
        $prev = $rebuild[$i - 1]
        $cur = $rebuild[$i]
        $dt = ($cur.Time - $prev.Time).TotalMilliseconds
        if ($cur.N -eq $prev.N -and $dt -le 150) {
            $msg = "double rebuild n=$($cur.N) within ${dt}ms"
            if ($issues -notcontains $msg) { $issues.Add($msg) }
        }
    }

    # Legacy / bad VM patterns
    if ($Lines | Select-String 'ws clear nonce waterline=\d+ suppress=true') {
        $issues.Add('old build: passive clear with suppress=true')
    }
    if ($Lines | Select-String 'ingest defer canvas.*awaiting clear') {
        $issues.Add('drawer stuck awaiting clear on ingest')
    }
    if ($Lines | Select-String 'guesser finalize publish ui=0') {
        $issues.Add('spurious guesser finalize ui=0')
    }
    if ($Lines | Select-String 'finish local .* ui=0 awaiting=true') {
        $issues.Add('drawer lift ui=0 while awaiting clear')
    }

    $finalize = @($Lines | Select-String 'guesser finalize publish ui=(\d+)')
    $ingestOk = @($Lines | Select-String 'ingest ok move#=\d+ kind=draw_stroke ui=(\d+)')
    $finishLocal = @($Lines | Select-String 'finish local .* ui=(\d+)')

    [pscustomobject]@{
        LayerAppend   = $append.Count
        LayerRebuild  = $rebuild.Count
        LayerClear    = $clear.Count
        GuesserFinalize = $finalize.Count
        IngestOk      = $ingestOk.Count
        FinishLocal   = $finishLocal.Count
        Issues        = $issues
        Pass          = ($issues.Count -eq 0)
    }
}

Write-Host "=== Draw Guess Canvas Sync Test ===" -ForegroundColor Cyan
Write-Host "Root: $Root"

if (-not $AnalyzeOnly) {
    if (-not $SkipBuild) {
        Write-Host "`n[1] JVM unit tests (DrawGuessEnterpriseCanvasTest)..." -ForegroundColor Yellow
        .\gradlew :app:testDebugUnitTest --tests "com.example.funlife.ui.screens.socialgame.play.DrawGuessEnterpriseCanvasTest" 2>&1 | ForEach-Object { Write-Host $_ }
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Unit tests failed"
        }
        Write-Host "Unit tests PASS" -ForegroundColor Green
    } else {
        Write-Host "`n[1] skip build (-SkipBuild)" -ForegroundColor DarkGray
    }
} else {
    Write-Host "`n[1] analyze-only mode (-AnalyzeOnly)" -ForegroundColor DarkGray
}

$logPath = $LogFile
if (-not $logPath) {
    $devices = adb devices 2>$null | Select-String "device$"
    if (-not $devices) {
        if ($AnalyzeOnly) {
            Write-Error "-AnalyzeOnly requires -LogFile when no adb device"
        }
        Write-Warning "No adb device — JVM tests only. Connect device and re-run for log capture."
        Write-Host @"

Manual capture:
  adb logcat -c
  adb logcat -s DrawGuessCanvas:I DrawGuessLiveSync:I GamePlaySync:D *:S | Tee-Object canvas.log
  # Guesser: watch drawer draw 5-6 strokes quickly
  .\pocketbase\tools\draw_ws\test_draw_canvas_sync.ps1 -AnalyzeOnly -LogFile canvas.log

"@ -ForegroundColor Green
        exit 0
    }

    if (-not $AnalyzeOnly) {
        Write-Host "`n[2] installDebug..." -ForegroundColor Yellow
        .\gradlew :app:installDebug 2>&1 | Select-Object -Last 6
        if ($LASTEXITCODE -ne 0) { Write-Error "installDebug failed" }
    }

    Write-Host "`n[$(if ($AnalyzeOnly) { '2' } else { '3' })] capture logcat ${DurationSec}s — draw on device NOW (guesser watching drawer)..." -ForegroundColor Yellow
    adb logcat -c | Out-Null
    $logPath = Join-Path $Root "pocketbase\tools\reports\draw_canvas_sync_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"
    New-Item -ItemType Directory -Force -Path (Split-Path $logPath) | Out-Null

    $deadline = (Get-Date).AddSeconds($DurationSec)
    $buffer = New-Object System.Collections.Generic.List[string]
    adb logcat -s DrawGuessCanvas:I DrawGuessLiveSync:I GamePlaySync:D *:S 2>&1 | ForEach-Object {
        if ((Get-Date) -gt $deadline) { break }
        $buffer.Add($_)
        Write-Host $_
    }
    $buffer | Set-Content -Path $logPath -Encoding utf8
}

if (-not (Test-Path $logPath)) {
    Write-Error "Log file not found: $logPath"
}

Write-Host "`n[analyze] $logPath" -ForegroundColor Yellow
$lines = Get-Content $logPath -Encoding utf8
$result = Analyze-DrawCanvasLog -Lines $lines

Write-Host "`n=== Log stats ===" -ForegroundColor Cyan
Write-Host "  layer append:        $($result.LayerAppend)"
Write-Host "  layer rebuild:       $($result.LayerRebuild)"
Write-Host "  layer clear:         $($result.LayerClear)"
Write-Host "  guesser finalize:    $($result.GuesserFinalize)"
Write-Host "  ingest ok (stroke):  $($result.IngestOk)"
Write-Host "  finish local:        $($result.FinishLocal)"

if ($result.LayerRebuild -gt 0 -and $result.LayerAppend -gt 0) {
    $ratio = [math]::Round($result.LayerRebuild / ($result.LayerAppend + $result.LayerRebuild) * 100, 1)
    Write-Host "  rebuild ratio:       ${ratio}% (lower is better; aim append-only chain)"
}

Write-Host "`n=== Checks ===" -ForegroundColor Cyan
if ($result.Issues.Count -eq 0) {
    Write-Host "  No heuristic failures detected" -ForegroundColor Green
} else {
    foreach ($issue in $result.Issues) {
        Write-Host "  FAIL: $issue" -ForegroundColor Red
    }
}

# Reference: analyze user's known-bad snippet if present in log
$userBad = $lines | Select-String 'layer rebuild n=6.*05:24:47'
if ($userBad) {
    Write-Host "  NOTE: log contains known pre-fix concurrent rebuild pattern (05:24:47 n=6)" -ForegroundColor DarkYellow
}

if ($result.Pass) {
    Write-Host "`nPASS" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nFAIL — see issues above" -ForegroundColor Red
    exit 1
}
