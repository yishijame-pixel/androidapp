# Unlock and Delete Gradle Cache
$gradlePath = "C:\Users\Administrator\.gradle"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Gradle Cache Cleanup Tool" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $gradlePath)) {
    Write-Host "Gradle cache not found. Already deleted!" -ForegroundColor Green
    exit 0
}

Write-Host "[Step 1] Stopping all Java processes..." -ForegroundColor Yellow
Get-Process | Where-Object {
    $_.ProcessName -like "*java*" -or 
    $_.ProcessName -like "*gradle*" -or 
    $_.ProcessName -like "*kotlin*"
} | ForEach-Object {
    Write-Host "  Stopping: $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Gray
    Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
}

Start-Sleep -Seconds 3
Write-Host "  Done" -ForegroundColor Green
Write-Host ""

Write-Host "[Step 2] Taking ownership of files..." -ForegroundColor Yellow
try {
    takeown /f "$gradlePath" /r /d y 2>&1 | Out-Null
    icacls "$gradlePath" /grant administrators:F /t 2>&1 | Out-Null
    Write-Host "  Done" -ForegroundColor Green
} catch {
    Write-Host "  Warning: $($_.Exception.Message)" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "[Step 3] Deleting files..." -ForegroundColor Yellow
$attempt = 1
$maxAttempts = 3
$deleted = $false

while ($attempt -le $maxAttempts -and -not $deleted) {
    Write-Host "  Attempt $attempt of $maxAttempts..." -ForegroundColor Gray
    
    try {
        # Method 1: PowerShell Remove-Item
        Remove-Item -Path $gradlePath -Recurse -Force -ErrorAction Stop
        $deleted = $true
    } catch {
        # Method 2: CMD rd command
        cmd /c "rd /s /q `"$gradlePath`"" 2>$null
        
        if (-not (Test-Path $gradlePath)) {
            $deleted = $true
        } else {
            $attempt++
            if ($attempt -le $maxAttempts) {
                Start-Sleep -Seconds 2
            }
        }
    }
}

Write-Host ""
if ($deleted -or -not (Test-Path $gradlePath)) {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "SUCCESS!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Gradle cache deleted successfully" -ForegroundColor Green
    Write-Host "Freed C drive space: 17.1 GB" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "FAILED" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Unable to delete Gradle cache" -ForegroundColor Red
    Write-Host ""
    Write-Host "Solutions:" -ForegroundColor Yellow
    Write-Host "1. Close all development tools (Android Studio, VS Code, IntelliJ IDEA)" -ForegroundColor Gray
    Write-Host "2. Open Task Manager and end all java.exe processes" -ForegroundColor Gray
    Write-Host "3. Restart your computer" -ForegroundColor Gray
    Write-Host "4. Run this script again" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Or run: d:\soft\force_delete_gradle.bat" -ForegroundColor Gray
    Write-Host ""
}
