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
$commit = "$($pin.commit)".Trim()
if (-not $commit) { throw "Invalid commit in $PinFile" }

New-Item -ItemType Directory -Force -Path (Split-Path $UpstreamRoot) | Out-Null
if (Test-Path $UpstreamRoot) { Remove-Item -Recurse -Force $UpstreamRoot }

Write-Host "Clone SuperTux @ $commit -> $UpstreamRoot"
git clone --depth 1 --revision=$commit https://github.com/SuperTux/supertux.git $UpstreamRoot
if ($LASTEXITCODE -ne 0) { throw "git clone failed" }

git -C $UpstreamRoot submodule update --init --recursive --depth 1 2>$null
$head = git -C $UpstreamRoot rev-parse HEAD
if ($head.Trim() -ne $commit) { throw "Checkout mismatch: want $commit got $head" }
Write-Host "OK upstream $(git -C $UpstreamRoot rev-parse --short HEAD)" -ForegroundColor Green
