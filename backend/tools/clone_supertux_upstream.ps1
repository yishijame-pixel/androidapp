# Clone SuperTux upstream at pin commit (CI / local before bootstrap)
param(
    [string]$UpstreamRoot = "",
    [string]$PinFile = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $UpstreamRoot) { $UpstreamRoot = Join-Path $RepoRoot "reference-assets\supertux" }
if (-not $PinFile) { $PinFile = Join-Path $RepoRoot "engine\supertux-fork\source_pin.json" }

if (-not (Test-Path $PinFile)) { throw "Missing $PinFile" }
$pin = Get-Content $PinFile -Raw | ConvertFrom-Json
$commit = $pin.commit
if (-not $commit) { throw "Invalid commit in $PinFile" }

New-Item -ItemType Directory -Force -Path (Split-Path $UpstreamRoot) | Out-Null

if (Test-Path (Join-Path $UpstreamRoot ".git")) {
    Write-Host "Fetch upstream $commit -> $UpstreamRoot"
    git -C $UpstreamRoot fetch --depth 1 origin $commit
    git -C $UpstreamRoot checkout -q FETCH_HEAD
} else {
    if (Test-Path $UpstreamRoot) { Remove-Item -Recurse -Force $UpstreamRoot }
    Write-Host "Clone upstream $commit -> $UpstreamRoot"
    git init $UpstreamRoot
    git -C $UpstreamRoot remote add origin https://github.com/SuperTux/supertux.git
    git -C $UpstreamRoot fetch --depth 1 origin $commit
    git -C $UpstreamRoot checkout -q FETCH_HEAD
}

git -C $UpstreamRoot submodule update --init --recursive --depth 1 2>$null
$head = git -C $UpstreamRoot rev-parse --short HEAD
Write-Host "OK upstream $head" -ForegroundColor Green
