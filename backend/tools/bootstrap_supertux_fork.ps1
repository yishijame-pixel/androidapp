# 将 reference-assets/supertux 同步到 engine/supertux-fork（FunLife fork 工作树）
# 用法: powershell -File backend/tools/bootstrap_supertux_fork.ps1

param(
    [string]$UpstreamRoot = "",
    [string]$ForkRoot = ""
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $UpstreamRoot) { $UpstreamRoot = Join-Path $RepoRoot "reference-assets\supertux" }
if (-not $ForkRoot) { $ForkRoot = Join-Path $RepoRoot "engine\supertux-fork" }

if (-not (Test-Path (Join-Path $UpstreamRoot "CMakeLists.txt"))) {
    throw "Missing upstream at $UpstreamRoot — clone SuperTux to reference-assets/supertux first"
}

$preserve = @(
    "patches",
    "NOTICE",
    "source_pin.json",
    "FUNLIFE.md",
    ".gitignore"
)

Write-Host "Sync upstream -> fork" -ForegroundColor Cyan
Write-Host "  from: $UpstreamRoot"
Write-Host "  to:   $ForkRoot"

New-Item -ItemType Directory -Force -Path $ForkRoot | Out-Null

$robocopyArgs = @(
    $UpstreamRoot,
    $ForkRoot,
    "/MIR",
    "/XD", ".git",
    "/NFL", "/NDL", "/NJH", "/NJS", "/NC", "/NS"
)
foreach ($name in $preserve) {
    $robocopyArgs += "/XF"
    $robocopyArgs += $name
    $item = Join-Path $ForkRoot $name
    if ($name -eq "patches" -and -not (Test-Path $item)) {
        New-Item -ItemType Directory -Force -Path $item | Out-Null
    }
}

$rc = Start-Process -FilePath "robocopy.exe" -ArgumentList $robocopyArgs -Wait -PassThru -NoNewWindow
if ($rc.ExitCode -ge 8) {
    throw "robocopy failed with exit code $($rc.ExitCode)"
}

# 记录 pin
$pinPath = Join-Path $ForkRoot "source_pin.json"
$commit = ""
if (Test-Path (Join-Path $UpstreamRoot ".git")) {
    $commit = git -C $UpstreamRoot rev-parse HEAD 2>$null
}
if ($commit) {
    $pin = @{
        upstream = "https://github.com/SuperTux/supertux"
        commit = $commit.Trim()
        referencePath = "reference-assets/supertux"
        forkPhase = 1
        updatedAt = (Get-Date).ToString("yyyy-MM-dd")
        productDecision = "FunLife classic fork = primary; Kotlin adaptation = fallback"
    } | ConvertTo-Json -Depth 3
    Set-Content -Path $pinPath -Value $pin -Encoding UTF8
    Write-Host "OK source_pin.json -> $commit" -ForegroundColor Green
}

Write-Host "Bootstrap done. Next: cmake -B build (see docs/supertux-fork-build-guide.md)" -ForegroundColor Green
