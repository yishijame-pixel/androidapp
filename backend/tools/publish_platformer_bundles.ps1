# 发布 platformer_sfx + platformer_supertux 到 assets.yishi.site（静态 CDN）
# 用法:
#   powershell -File backend/tools/publish_platformer_bundles.ps1
#   powershell -File backend/tools/publish_platformer_bundles.ps1 -RestartServices
#
# 前置: dist/asset-bundles/platformer_sfx.zip 与 platformer_supertux.zip 已存在
#       （运行 python backend/tools/import_supertux_platformer.py）

param(
    [switch]$RestartServices,
    [switch]$SkipImport
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$ImportScript = Join-Path $PSScriptRoot "import_supertux_platformer.py"
$ValidateScript = Join-Path $PSScriptRoot "validate_platformer_supertux.py"
$UploadScript = Join-Path $RepoRoot "pocketbase\tools\upload-assets-static.ps1"

if (-not $SkipImport) {
    Write-Host "`n=== [1/4] 重新导入 dist（可选跳过 -SkipImport）===" -ForegroundColor Cyan
    python $ImportScript
    if ($LASTEXITCODE -ne 0) { throw "import_supertux_platformer failed" }
} else {
    Write-Host "`n=== [1/4] 跳过 import（-SkipImport）===" -ForegroundColor DarkGray
}

Write-Host "`n=== [2/4] 校验 dist ===" -ForegroundColor Cyan
python $ValidateScript
if ($LASTEXITCODE -ne 0) { throw "validate_platformer_supertux failed" }

foreach ($name in @("platformer_sfx", "platformer_supertux")) {
    $zip = Join-Path $RepoRoot "dist\asset-bundles\$name.zip"
    if (-not (Test-Path $zip)) {
        throw "Missing $zip — run import_supertux_platformer.py first"
    }
}

Write-Host "`n=== [3/4] 复制 zip + 生成 manifest ===" -ForegroundColor Cyan
$uploadArgs = @("-File", $UploadScript)
if ($RestartServices) { $uploadArgs += "-RestartServices" }
& powershell @uploadArgs
if ($LASTEXITCODE -ne 0) { throw "upload-assets-static failed" }

Write-Host "`n=== [4/4] 验证 CDN manifest 条目 ===" -ForegroundColor Cyan
$manifestPath = Join-Path $RepoRoot "pocketbase\assets_public\manifest.json"
$manifest = Get-Content $manifestPath -Raw | ConvertFrom-Json
foreach ($id in @("platformer_sfx", "platformer_supertux")) {
    $entry = $manifest.bundles | Where-Object { $_.id -eq $id }
    if (-not $entry) { throw "manifest missing bundle id=$id" }
    Write-Host "  OK $id sha256=$($entry.sha256.Substring(0,12))… bv=$($entry.bundleVersion)" -ForegroundColor Green
}

Write-Host "`nPublish done. Restart App to fetch platformer_sfx." -ForegroundColor Green
Write-Host "Public manifest: https://assets.yishi.site/manifest.json (v$($manifest.version))"
