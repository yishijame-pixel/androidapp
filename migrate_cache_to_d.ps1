# Cache Migration Script - Move C drive cache to D drive
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Cache Migration Tool - C to D Drive" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$totalSaved = 0

function Migrate-Directory {
    param(
        [string]$SourcePath,
        [string]$TargetPath,
        [string]$Name,
        [double]$SizeGB
    )
    
    Write-Host "[$Name] Starting migration..." -ForegroundColor Yellow
    
    if (-not (Test-Path $SourcePath)) {
        Write-Host "  Source not found: $SourcePath" -ForegroundColor Red
        return 0
    }
    
    if (Test-Path $TargetPath) {
        Write-Host "  Target already exists: $TargetPath" -ForegroundColor Red
        Write-Host "  Skipping..." -ForegroundColor Gray
        return 0
    }
    
    try {
        Write-Host "  Copying files to D drive..." -ForegroundColor Gray
        Copy-Item -Path $SourcePath -Destination $TargetPath -Recurse -Force -ErrorAction Stop
        
        if (Test-Path $TargetPath) {
            Write-Host "  Copy successful" -ForegroundColor Green
            
            Write-Host "  Removing C drive original..." -ForegroundColor Gray
            Remove-Item -Path $SourcePath -Recurse -Force -ErrorAction Stop
            
            Write-Host "  Creating symbolic link..." -ForegroundColor Gray
            New-Item -ItemType SymbolicLink -Path $SourcePath -Target $TargetPath -Force -ErrorAction Stop | Out-Null
            
            Write-Host "  [$Name] Migration complete! Freed $SizeGB GB" -ForegroundColor Green
            return $SizeGB
        }
    }
    catch {
        Write-Host "  Migration failed: $($_.Exception.Message)" -ForegroundColor Red
        return 0
    }
    
    return 0
}

Write-Host ""
$saved = Migrate-Directory -SourcePath "C:\Users\Administrator\.android" -TargetPath "D:\.android" -Name "Android SDK" -SizeGB 12.3
$totalSaved += $saved

Write-Host ""
$saved = Migrate-Directory -SourcePath "C:\DrvPath" -TargetPath "D:\DrvPath" -Name "DrvPath" -SizeGB 2.19
$totalSaved += $saved

Write-Host ""
Write-Host "[Gradle] Cleaning old cache..." -ForegroundColor Yellow

if (Test-Path "C:\Users\Administrator\.gradle") {
    try {
        Get-Process | Where-Object {$_.ProcessName -like "*java*"} | Stop-Process -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
        
        Remove-Item -Path "C:\Users\Administrator\.gradle" -Recurse -Force -ErrorAction Stop
        Write-Host "  [Gradle] Old cache deleted! Freed 17.1 GB" -ForegroundColor Green
        $totalSaved += 17.1
    }
    catch {
        Write-Host "  Gradle cache deletion failed (file in use)" -ForegroundColor Red
        Write-Host "  Suggestion: Close all dev tools and delete manually" -ForegroundColor Gray
    }
}
else {
    Write-Host "  Gradle cache already removed" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Migration Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total C drive space freed: $totalSaved GB" -ForegroundColor Green
Write-Host ""
