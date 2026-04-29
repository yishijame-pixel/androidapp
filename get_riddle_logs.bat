@echo off
echo Getting Riddle logs...
echo.
gradlew :app:installDebug
timeout /t 2 /nobreak > nul
adb logcat -d | findstr /i "Riddle"
echo.
echo Done!
pause
