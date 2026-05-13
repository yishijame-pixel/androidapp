# Add ADB to PATH environment variable
# Run as Administrator if needed

$adbPath = "D:\Androidsdk\platform-tools"

# Check if path exists
if (-not (Test-Path $adbPath)) {
    Write-Host "Error: Path does not exist: $adbPath" -ForegroundColor Red
    exit 1
}

# Get current user PATH
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")

# Check if already added
if ($currentPath -like "*$adbPath*") {
    Write-Host "ADB path already in environment variable" -ForegroundColor Yellow
} else {
    # Add to PATH
    $newPath = $currentPath + ";" + $adbPath
    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
    Write-Host "Successfully added ADB to user environment variable" -ForegroundColor Green
    Write-Host "Path: $adbPath" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "Please restart PowerShell for changes to take effect" -ForegroundColor Yellow
Write-Host ""
Write-Host "Test command: adb version" -ForegroundColor Cyan

# Temporarily add to current session
$env:Path += ";$adbPath"
Write-Host ""
Write-Host "ADB temporarily added to current session" -ForegroundColor Green
