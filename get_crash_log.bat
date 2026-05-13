@echo off
setlocal

:: 设置ADB路径
set ADB_PATH=D:\Androidsdk\platform-tools\adb.exe

echo ===================================
echo Clearing app data and getting crash log
echo ===================================

echo Step 1: Clearing app data...
"%ADB_PATH%" shell pm clear com.example.funlife

echo Step 2: Clearing logcat buffer...
"%ADB_PATH%" logcat -c

echo Step 3: Starting app...
"%ADB_PATH%" shell am start -n com.example.funlife/.MainActivity

echo Step 4: Waiting for crash (5 seconds)...
timeout /t 5 /nobreak

echo Step 5: Getting crash log...
"%ADB_PATH%" logcat -d > crash_log.txt

echo.
echo Crash log saved to crash_log.txt
echo.
echo ===================================
echo Showing FATAL errors:
echo ===================================
findstr /i "FATAL" crash_log.txt

echo.
echo ===================================
echo Showing Exception errors:
echo ===================================
findstr /i "Exception" crash_log.txt | findstr /i "funlife"

pause
