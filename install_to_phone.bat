@echo off
echo ========================================
echo Installing FunLife App to Phone
echo ========================================
echo.

set ADB_PATH=D:\Androidsdk\platform-tools\adb.exe

if not exist "%ADB_PATH%" (
    echo ERROR: ADB not found at %ADB_PATH%
    pause
    exit /b 1
)

echo Step 1: Killing ADB server...
"%ADB_PATH%" kill-server
timeout /t 2 /nobreak >nul

echo Step 2: Starting ADB server...
"%ADB_PATH%" start-server
timeout /t 3 /nobreak >nul

echo.
echo Step 3: Checking connected devices...
"%ADB_PATH%" devices
echo.

echo Step 4: Installing app to phone...
echo This may take a moment...
echo.
call gradlew.bat installDebug

echo.
echo ========================================
echo Installation complete!
echo ========================================
pause
