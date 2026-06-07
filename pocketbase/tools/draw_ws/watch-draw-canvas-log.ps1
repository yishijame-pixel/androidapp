# Draw-guess canvas log watcher (drawer lift / guesser stroke disappear)
# Usage: .\watch-draw-canvas-log.ps1
# Requires adb connected device/emulator

$adb = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adb) {
    Write-Error "adb not in PATH; install Android SDK platform-tools"
    exit 1
}

Write-Host "Watching DrawGuessCanvas (Ctrl+C to stop)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Guesser OK:" -ForegroundColor Yellow
Write-Host "  guesser finalize publish ui=1" -ForegroundColor DarkGray
Write-Host "  ingest ok move#=1 kind=draw_stroke ui=1" -ForegroundColor DarkGray
Write-Host ""
Write-Host "Stroke disappear - check for:" -ForegroundColor Yellow
Write-Host "  ws clear nonce passive ... (canvas cleared)" -ForegroundColor DarkGray
Write-Host "  guesser finalize publish ui=0 (spurious empty publish)" -ForegroundColor DarkGray
Write-Host "  ingest defer canvas ... awaiting clear (old build)" -ForegroundColor DarkGray
Write-Host ""

adb logcat -c
adb logcat -s DrawGuessCanvas:I DrawGuessLiveSync:I GamePlaySync:D *:S
