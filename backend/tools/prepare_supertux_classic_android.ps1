# Download CI artifacts and extract SuperTux classic engine files into the FunLife app.
# Usage:
#   $env:GITHUB_TOKEN="ghp_..."   # repo read + actions:read
#   powershell -File backend/tools/prepare_supertux_classic_android.ps1
#
# Or pass a local CI artifact zip directory:
#   powershell -File backend/tools/prepare_supertux_classic_android.ps1 -NativeZip D:\Downloads\libsupertux2-arm64-v8a.zip -ApkZip D:\Downloads\supertux-fork-apk.zip

param(
    [string]$Repo = "yishijame-pixel/androidapp",
    [long]$RunId = 28449235537,
    [string]$NativeZip = "",
    [string]$ApkZip = ""
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location $RepoRoot

$JniDir = Join-Path $RepoRoot "app\src\main\jniLibs\arm64-v8a"
$AssetsDir = Join-Path $RepoRoot "app\src\main\assets"
$DataZip = Join-Path $AssetsDir "data.zip"
$Tmp = Join-Path $RepoRoot ".tmp_supertux_classic"

New-Item -ItemType Directory -Force -Path $JniDir, $AssetsDir, $Tmp | Out-Null

function Get-GitHubTokenFromCredential {
    if ($env:GITHUB_TOKEN) { return $env:GITHUB_TOKEN }
    if ($env:GH_TOKEN) { return $env:GH_TOKEN }
    $credInput = "protocol=https`nhost=github.com`n`n"
    $credLines = $credInput | git credential fill 2>$null
    $tokenLine = $credLines | Where-Object { $_ -like 'password=*' } | Select-Object -First 1
    if ($tokenLine) { return $tokenLine.Substring('password='.Length) }
    return $null
}

function Invoke-GhDownload($Url, $OutFile) {
    $headers = @{}
    $token = Get-GitHubTokenFromCredential
    if ($token) {
        $headers["Authorization"] = "Bearer $token"
    }
    Write-Host "GET $Url"
    Invoke-WebRequest -Uri $Url -Headers $headers -OutFile $OutFile
}

if (-not $NativeZip) {
    $nativeOut = Join-Path $Tmp "libsupertux2-arm64-v8a.zip"
    $nativeUrl = "https://api.github.com/repos/$Repo/actions/artifacts/7983010678/zip"
    Invoke-GhDownload $nativeUrl $nativeOut
    $NativeZip = $nativeOut
}

if (-not $ApkZip) {
    $apkOut = Join-Path $Tmp "supertux-fork-apk-arm64-release.zip"
    $apkUrl = "https://api.github.com/repos/$Repo/actions/artifacts/7983015824/zip"
    Invoke-GhDownload $apkUrl $apkOut
    $ApkZip = $apkOut
}

Write-Host "Extract native -> $JniDir"
Expand-Archive -Path $NativeZip -DestinationPath (Join-Path $Tmp "native") -Force
Get-ChildItem -Path (Join-Path $Tmp "native") -Recurse -Filter "libsupertux2.so" | ForEach-Object {
    Copy-Item $_.FullName (Join-Path $JniDir "libsupertux2.so") -Force
}
Get-ChildItem -Path (Join-Path $Tmp "native") -Recurse -Filter "libc++_shared.so" | ForEach-Object {
    Copy-Item $_.FullName (Join-Path $JniDir "libc++_shared.so") -Force
}

Write-Host "Extract data.zip from APK artifact -> $DataZip"
Expand-Archive -Path $ApkZip -DestinationPath (Join-Path $Tmp "apk") -Force
$apkFile = Get-ChildItem -Path (Join-Path $Tmp "apk") -Recurse -Filter "*.apk" | Select-Object -First 1
if (-not $apkFile) { throw "No APK found in artifact zip" }
$apkAsZip = Join-Path $Tmp "supertux.apk.zip"
Copy-Item $apkFile.FullName $apkAsZip -Force
Expand-Archive -Path $apkAsZip -DestinationPath (Join-Path $Tmp "apk_unzipped") -Force
$embedded = Join-Path $Tmp "apk_unzipped\assets\data.zip"
if (-not (Test-Path $embedded)) {
    throw "assets/data.zip not found inside APK — rebuild supertux-fork-android CI"
}
Copy-Item $embedded $DataZip -Force

Write-Host ""
Write-Host "OK SuperTux classic assets prepared:"
Write-Host "  $(Join-Path $JniDir 'libsupertux2.so')"
Write-Host "  $(Join-Path $JniDir 'libc++_shared.so')"
Write-Host "  $DataZip"
Write-Host "Rebuild FunLife APK: .\gradlew.bat :app:assembleDebug"
