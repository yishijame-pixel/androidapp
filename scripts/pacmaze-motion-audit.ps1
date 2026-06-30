# PacMaze 位图顿挫自动化审计
# 用法:
#   powershell -File scripts/pacmaze-motion-audit.ps1              # 修复后验收 (RIGHT)
#   powershell -File scripts/pacmaze-motion-audit.ps1 -Baseline     # 复现换帧跳 (强制动画)
param(
    [ValidateSet("RIGHT","LEFT","UP","DOWN")]
    [string]$Dir = "RIGHT",
    [int]$HoldMs = 12000,
    [int]$WarmupSec = 22,
    [switch]$SkipInstall,
    [switch]$Baseline
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

$LogFile = Join-Path $Root "motion_audit_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"
$ReportFile = Join-Path $Root "motion_audit_report.txt"

function Write-Report([string]$Line) {
    Write-Host $Line
    Add-Content -Path $ReportFile -Value $Line -Encoding utf8
}

function Parse-LogicX([string]$Line) {
    if ($Line -match 'logic=\(([0-9\.-]+),') { return [double]$Matches[1] }
    return $null
}

if (-not $SkipInstall) {
    Write-Host ">> gradlew installDebug..."
    & .\gradlew :app:installDebug 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "installDebug failed" }
}

$mode = if ($Baseline) { "baseline_animated" } else { "fix_stable" }
Write-Host ">> mode=$mode launch pac_maze..."
adb shell am force-stop com.example.funlife | Out-Null
adb logcat -c | Out-Null
adb shell am start -n com.example.funlife/.MainActivity --es fun_deep_link "pac_maze?autoStart=true" | Out-Null
Start-Sleep -Seconds $WarmupSec

Write-Host ">> hold joystick $Dir for ${HoldMs}ms..."
& (Join-Path $Root "scripts\pacmaze-joystick-hold.ps1") -Dir $Dir -HoldMs $HoldMs | Out-Null
Start-Sleep -Seconds 2
Write-Host ">> exit play (trigger AUDIT_SUMMARY)..."
adb shell input keyevent 4 | Out-Null
Start-Sleep -Seconds 2

Write-Host ">> pull logcat -> $LogFile"
adb logcat -d -s PacMazeMotionDiag 2>&1 | Out-File $LogFile -Encoding utf8

$lines = Get-Content $LogFile -ErrorAction SilentlyContinue
if (-not $lines) { $lines = @() }

$logicXs = @()
foreach ($line in ($lines | Select-String 'logic=\(')) {
    $x = Parse-LogicX $line.Line
    if ($null -ne $x) { $logicXs += $x }
}
$logicXMin = if ($logicXs.Count) { ($logicXs | Measure-Object -Minimum).Minimum } else { $null }
$logicXMax = if ($logicXs.Count) { ($logicXs | Measure-Object -Maximum).Maximum } else { $null }
$moved = ($logicXs.Count -gt 1) -and ($logicXMax - $logicXMin -gt 0.05)

$counts = @{
    BITMAP_DRAW       = ($lines | Select-String 'BITMAP_DRAW').Count
    BITMAP_FEET_JUMP  = ($lines | Select-String 'BITMAP_FEET_JUMP').Count
    VISUAL_HEAD_JUMP  = ($lines | Select-String 'VISUAL_HEAD_JUMP').Count
    VISUAL_SOLE_JUMP  = ($lines | Select-String 'VISUAL_SOLE_JUMP').Count
    LAYOUT_FEET_JUMP  = ($lines | Select-String 'LAYOUT_FEET_JUMP').Count
    SCREEN_Y_DRIFT    = ($lines | Select-String 'SCREEN_Y_DRIFT').Count
    SCREEN_Y_JUMP     = ($lines | Select-String 'SCREEN_Y_JUMP').Count
    RENDER_CLAMP      = ($lines | Select-String 'RENDER_CLAMP').Count
    MULTI_TICK        = ($lines | Select-String 'MULTI_TICK').Count
    RENDER_SAMPLES    = ($lines | Select-String 'logic=\(').Count
}

$auditSummary = $lines | Select-String 'AUDIT_SUMMARY' | Select-Object -Last 1
$headJumps = $lines | Select-String 'VISUAL_HEAD_JUMP'
$soleJumps = $lines | Select-String 'VISUAL_SOLE_JUMP'
$bitmapDraws = $lines | Select-String 'BITMAP_DRAW'

$soleDyValues = @()
$frameIndices = @()
$pivotFYs = @()
foreach ($line in $bitmapDraws) {
    if ($line -match 'soleDy=([\d\.-]+)') { $soleDyValues += [float]$Matches[1] }
    if ($line -match 'frame=(\d+)') { $frameIndices += [int]$Matches[1] }
    if ($line -match 'pivotFY=([\d\.-]+)') { $pivotFYs += [float]$Matches[1] }
}
$uniqueFrames = ($frameIndices | Sort-Object -Unique)
$nonZeroSoleDy = ($soleDyValues | Where-Object { [math]::Abs($_) -gt 0.5 }).Count
$pivotFYRange = if ($pivotFYs.Count -gt 1) { ($pivotFYs | Measure-Object -Maximum).Maximum - ($pivotFYs | Measure-Object -Minimum).Minimum } else { 0 }

$screenYs = @()
foreach ($line in ($lines | Select-String 'screenY=\d+')) {
    if ($line -match 'screenY=(\d+)') { $screenYs += [int]$Matches[1] }
}
$screenYRange = if ($screenYs.Count -gt 1) { ($screenYs | Measure-Object -Maximum).Maximum - ($screenYs | Measure-Object -Minimum).Minimum } else { 0 }

$skinLine = ($bitmapDraws | Select-Object -First 1).Line
$skin = if ($skinLine -match 'skin=(\S+)') { $Matches[1] } else { "?" }

"" | Out-File $ReportFile -Encoding utf8
Write-Report "========================================"
Write-Report " PacMaze Motion Audit Report"
Write-Report " $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Report " mode=$mode dir=$Dir skin=$skin"
Write-Report " log=$LogFile"
Write-Report "========================================"
Write-Report ""
Write-Report "[Movement]"
Write-Report "  logicX min=$logicXMin max=$logicXMax delta=$([math]::Round(($logicXMax - $logicXMin), 3)) moved=$moved"
Write-Report "  screenY range=${screenYRange}px"
Write-Report ""
Write-Report "[Counts]"
foreach ($k in $counts.Keys | Sort-Object) {
    Write-Report ("  {0,-18} {1}" -f $k, $counts[$k])
}
Write-Report ""
Write-Report "[Bitmap draw]"
Write-Report "  uniqueFrames=$($uniqueFrames -join ',') count=$($uniqueFrames.Count)"
Write-Report "  nonZeroSoleDy=$nonZeroSoleDy pivotFYRange=$([math]::Round($pivotFYRange, 4))"
Write-Report ""
if ($auditSummary) {
    Write-Report "[App diagnosis]"
    Write-Report "  $($auditSummary.Line.Trim())"
    Write-Report ""
}
Write-Report "[Head jumps (max 8)]"
$headJumps | Select-Object -First 8 | ForEach-Object { Write-Report "  $($_.Line.Trim())" }
Write-Report ""
Write-Report "[Sole jumps (max 5)]"
$soleJumps | Select-Object -First 5 | ForEach-Object { Write-Report "  $($_.Line.Trim())" }
Write-Report ""

$verdict = "UNKNOWN"
if (-not $moved) {
    $verdict = "FAIL_NO_MOVEMENT(try_RIGHT_or_reposition)"
} elseif ($Baseline -and $counts.VISUAL_HEAD_JUMP -ge 2) {
    $verdict = "BASELINE_CONFIRMED_HEAD_BOUNCE"
} elseif (-not $Baseline -and $counts.VISUAL_HEAD_JUMP -eq 0 -and $counts.VISUAL_SOLE_JUMP -eq 0 -and $screenYRange -le 1) {
    $verdict = "PASS(stable_after_fix)"
} elseif ($counts.VISUAL_HEAD_JUMP -ge 2) {
    $verdict = "FAIL_HEAD_BOUNCE"
} elseif ($counts.VISUAL_SOLE_JUMP -ge 2) {
    $verdict = "FAIL_SOLE_MISALIGN"
} elseif ($counts.SCREEN_Y_DRIFT -ge 2) {
    $verdict = "FAIL_RENDER_DRIFT"
}

Write-Report "[Verdict] $verdict"
Write-Report ""
Write-Report "Commands:"
Write-Report "  adb logcat -s PacMazeMotionDiag"
Write-Report "  powershell -File scripts/pacmaze-motion-audit.ps1 -Dir RIGHT"
Write-Report "  powershell -File scripts/pacmaze-motion-audit.ps1 -Baseline -Dir RIGHT"

if ($verdict -match '^PASS') { exit 0 } else { exit 1 }
