# Fast loop: CI native .so -> FunLife jniLibs -> installDebug -> optional logcat
#
# Typical use (after pushing patch to main):
#   powershell -File backend/tools/quick_supertux_native.ps1 -TriggerCi
#
# Or wait for / reuse an existing green run:
#   powershell -File backend/tools/quick_supertux_native.ps1
#   powershell -File backend/tools/quick_supertux_native.ps1 -RunId 28457850084
#
# Requires: gh CLI (logged in) OR GITHUB_TOKEN with actions:read

param(
    [string]$Repo = "yishijame-pixel/androidapp",
    [string]$Branch = "main",
    [long]$RunId = 0,
    [switch]$TriggerCi,
    [switch]$SkipInstall,
    [switch]$Logcat
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location $RepoRoot

$JniDir = Join-Path $RepoRoot "app\src\main\jniLibs\arm64-v8a"
$Tmp = Join-Path $RepoRoot ".tmp_supertux_quick"
New-Item -ItemType Directory -Force -Path $JniDir, $Tmp | Out-Null

function Assert-Gh {
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        throw "gh CLI not found. Install: https://cli.github.com/ — or download artifact manually and run prepare_supertux_classic_android.ps1 -NativeZip ..."
    }
}

function Invoke-GhJson([string]$Args) {
    $raw = gh $Args.Split(' ') --repo $Repo 2>&1
    if ($LASTEXITCODE -ne 0) { throw "gh failed: gh $Args`n$raw" }
    return $raw | ConvertFrom-Json
}

function Wait-WorkflowRun([long]$Id, [int]$TimeoutMinutes = 25) {
    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    Write-Host "Watching run $Id ..."
    while ((Get-Date) -lt $deadline) {
        $run = Invoke-GhJson "run view $Id --json status,conclusion,displayTitle,url"
        Write-Host ("  [{0}] {1} / {2} — {3}" -f (Get-Date -Format "HH:mm:ss"), $run.status, $run.conclusion, $run.url)
        if ($run.status -eq "completed") {
            if ($run.conclusion -ne "success") {
                throw "Workflow run $Id finished with: $($run.conclusion). See: $($run.url)"
            }
            return
        }
        Start-Sleep -Seconds 20
    }
    throw "Timed out waiting for run $Id"
}

function Get-LatestGreenRunId {
    $runs = Invoke-GhJson "run list --workflow supertux-fork-android.yml --branch $Branch --limit 10 --json databaseId,conclusion,createdAt"
    $green = $runs | Where-Object { $_.conclusion -eq "success" } | Select-Object -First 1
    if (-not $green) { throw "No successful supertux-fork-android run on $Branch" }
    return [long]$green.databaseId
}

function Download-NativeSo([long]$Id) {
    $out = Join-Path $Tmp "run-$Id"
    if (Test-Path $out) { Remove-Item $out -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $out | Out-Null
    Write-Host "Downloading libsupertux2-arm64-v8a from run $Id ..."
    gh run download $Id --repo $Repo --name libsupertux2-arm64-v8a --dir $out
    if ($LASTEXITCODE -ne 0) { throw "gh run download failed for run $Id" }
    $so = Get-ChildItem -Path $out -Recurse -Filter "libsupertux2.so" |
        Sort-Object @{
            Expression = {
                if ($_.FullName -match 'stripReleaseDebugSymbols|merged_native_libs') { 0 }
                elseif ($_.FullName -match 'stripped') { 1 }
                else { 2 }
            }
        }, Length |
        Select-Object -First 1
    if (-not $so) { throw "libsupertux2.so not found in artifact" }
    Copy-Item $so.FullName (Join-Path $JniDir "libsupertux2.so") -Force
    $cxx = Get-ChildItem -Path $out -Recurse -Filter "libc++_shared.so" | Select-Object -First 1
    if ($cxx) {
        Copy-Item $cxx.FullName (Join-Path $JniDir "libc++_shared.so") -Force
    }
    Write-Host "Updated $(Join-Path $JniDir 'libsupertux2.so') ($([math]::Round($so.Length / 1MB, 1)) MB)"
}

Assert-Gh

if ($TriggerCi) {
    Write-Host "Triggering supertux-fork-android on $Branch ..."
    gh workflow run supertux-fork-android.yml --repo $Repo --ref $Branch
    if ($LASTEXITCODE -ne 0) { throw "gh workflow run failed" }
    Start-Sleep -Seconds 5
    $pending = Invoke-GhJson "run list --workflow supertux-fork-android.yml --branch $Branch --limit 1 --json databaseId,status"
    $RunId = [long]$pending.databaseId
    Write-Host "Started run $RunId"
}

if ($RunId -le 0) {
    $RunId = Get-LatestGreenRunId
    Write-Host "Using latest green run: $RunId"
} elseif (-not $TriggerCi) {
    # Verify run exists; if still running, wait
    $run = Invoke-GhJson "run view $RunId --json status,conclusion"
    if ($run.status -ne "completed") {
        Wait-WorkflowRun $RunId
    } elseif ($run.conclusion -ne "success") {
        throw "Run $RunId conclusion=$($run.conclusion)"
    }
}

if ($TriggerCi) {
    Wait-WorkflowRun $RunId
}

Download-NativeSo $RunId

if (-not $SkipInstall) {
    Write-Host "installDebug (only repacks jniLibs — ~1–2 min, no CI) ..."
    & .\gradlew.bat :app:installDebug --quiet
    if ($LASTEXITCODE -ne 0) { throw "gradlew installDebug failed" }
    Write-Host "Installed on device."
}

if ($Logcat) {
    Write-Host "Logcat (Ctrl+C to stop). Tip: reproduce crash, look for PHYSFS / SIGSEGV"
    adb logcat -c
    adb logcat -b crash -b main *:E SDL:V SuperTuxClassic:I findlocale:I
}

Write-Host ""
Write-Host "Done. Native-only changes do NOT need to re-download data.zip."
Write-Host "Next: open classic engine level on device."
