# Monitor SpinWheelViewModel logs
$adb = "D:\Androidsdk\platform-tools\adb.exe"

Write-Host "=== Monitoring SpinWheelViewModel logs ===" -ForegroundColor Green
Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""

& $adb logcat -v time | Select-String "SpinWheelViewModel"
