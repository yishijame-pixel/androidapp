#Requires -Version 5.1
# Run in a new PowerShell window for interactive Firebase login.
$Host.UI.RawUI.WindowTitle = "FunLife Firebase Init"
$Root = $PSScriptRoot

if (-not (Get-Command firebase -ErrorAction SilentlyContinue)) {
    $ToolsFirebase = Join-Path $Root 'tools\firebase-cli\node_modules\.bin\firebase.cmd'
    if (-not (Test-Path $ToolsFirebase)) {
        Write-Host "Missing firebase CLI. Run init-firebase.ps1 first." -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
}

& (Join-Path $Root 'init-firebase.ps1')
Read-Host "Press Enter to close this window"
