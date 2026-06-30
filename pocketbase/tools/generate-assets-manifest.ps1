#Requires -Version 5.1
# 根据 assets_public/bundles/*.zip 生成 manifest.json（含 SHA-256）
#
# 用法:
#   cd d:\soft\pocketbase
#   .\tools\generate-assets-manifest.ps1
#   .\tools\generate-assets-manifest.ps1 -BaseUrl https://assets.yishi.site -Version 10

param(
    [string]$BaseUrl = "https://assets.yishi.site",
    [int]$Version = 0,
    [string]$AssetsRoot = (Join-Path (Split-Path $PSScriptRoot -Parent) "assets_public")
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$bundlesDir = Join-Path $AssetsRoot "bundles"
$manifestPath = Join-Path $AssetsRoot "manifest.json"
$examplePath = Join-Path $AssetsRoot "manifest.json.example"

if (-not (Test-Path $bundlesDir)) {
    New-Item -ItemType Directory -Force -Path $bundlesDir | Out-Null
}

# 默认 bundle 列表（与 backend/tools/asset_manifest.json 对齐）
$defaultBundles = @(
    @{ id = "xiangkuang"; file = "xiangkuang.zip"; targetDir = "xiangkuang" },
    @{ id = "pet"; file = "pet.zip"; targetDir = "pet" },
    @{ id = "login"; file = "login.zip"; targetDir = "login" },
    @{ id = "renge"; file = "renge.zip"; targetDir = "renge" },
    @{ id = "dibu"; file = "dibu.zip"; targetDir = "dibu" },
    @{ id = "wheel"; file = "wheel.zip"; targetDir = "wheel" },
    @{ id = "pac_maze_sfx"; file = "pac_maze_sfx.zip"; targetDir = "pac_maze_sfx" },
    @{ id = "pac_maze_skins"; file = "pac_maze_skins.zip"; targetDir = "pac_maze_skins" },
    @{ id = "platformer_characters"; file = "platformer_characters.zip"; targetDir = "platformer_characters" },
    @{ id = "platformer_sfx"; file = "platformer_sfx.zip"; targetDir = "platformer_sfx" },
    @{ id = "platformer_supertux"; file = "platformer_supertux.zip"; targetDir = "platformer_supertux" }
)

if ($Version -le 0 -and (Test-Path $manifestPath)) {
    try {
        $prev = Get-Content $manifestPath -Raw | ConvertFrom-Json
        $Version = [int]$prev.version + 1
    } catch {
        $Version = 1
    }
} elseif ($Version -le 0) {
    $Version = 1
}

$base = $BaseUrl.TrimEnd('/')
$outBundles = @()
$missing = @()

function Get-ZipBundleVersion {
    param([string]$ZipPath, [string]$EntryPath)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($ZipPath)
    try {
        $entry = $zip.GetEntry($EntryPath)
        if ($null -eq $entry) {
            $alt = $EntryPath -replace '/', '\'
            $entry = $zip.GetEntry($alt)
        }
        if ($null -eq $entry) { return $null }
        $reader = New-Object System.IO.StreamReader($entry.Open())
        try {
            $text = $reader.ReadToEnd().Trim().TrimStart([char]0xFEFF)
            if ($text -match '^\d+$') { return [int]$text }
            return $null
        } finally { $reader.Close() }
    } finally { $zip.Dispose() }
}

foreach ($b in $defaultBundles) {
    $zipPath = Join-Path $bundlesDir $b.file
    if (-not (Test-Path $zipPath)) {
        $missing += $b.file
        continue
    }
    $hash = (Get-FileHash -Path $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
    $mb = [math]::Round((Get-Item $zipPath).Length / 1MB, 2)
    $bundleVersion = $null
    $versionEntry = "$($b.targetDir)/bundle_version.txt"
    if ($b.id -in @("pac_maze_skins", "platformer_characters", "platformer_sfx", "platformer_supertux")) {
        $bundleVersion = Get-ZipBundleVersion -ZipPath $zipPath -EntryPath $versionEntry
    }
    $cacheBust = $hash.Substring(0, 12)
    $url = "$base/bundles/$($b.file)?v=$cacheBust"
    $versionLabel = if ($null -ne $bundleVersion) { " bv=$bundleVersion" } else { "" }
    Write-Host "  $($b.file)  ${mb} MB  sha256=$hash$versionLabel"
    $entry = [ordered]@{
        id        = $b.id
        file      = $b.file
        targetDir = $b.targetDir
        url       = $url
        sha256    = $hash
    }
    if ($null -ne $bundleVersion) {
        $entry["bundleVersion"] = $bundleVersion
    }
    $outBundles += $entry
}

if ($outBundles.Count -eq 0) {
    Write-Host "bundles/ 下无 zip，请先放入资源包" -ForegroundColor Red
    Write-Host "参考: manifest.json.example" -ForegroundColor Yellow
    exit 1
}

$manifest = [ordered]@{
    ok        = $true
    version   = $Version
    updatedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    bundles   = $outBundles
}

$json = ($manifest | ConvertTo-Json -Depth 6)
[System.IO.File]::WriteAllText($manifestPath, $json, (New-Object System.Text.UTF8Encoding $false))
Write-Host "`n[OK] manifest.json v$Version ($($outBundles.Count) bundles)" -ForegroundColor Green
Write-Host "     $manifestPath"

if ($missing.Count -gt 0) {
    Write-Host "`n[WARN] 未找到（已跳过）: $($missing -join ', ')" -ForegroundColor Yellow
}

if (-not (Test-Path $examplePath)) {
    Copy-Item $manifestPath $examplePath
}
