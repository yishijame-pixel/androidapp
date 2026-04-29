@echo off
echo ========================================
echo Launching FunLife App
echo ========================================
echo.

echo Checking if app is installed...
gradlew.bat installDebug

echo.
echo Starting app...
gradlew.bat :app:launchDebug

echo.
echo App should be running now!
pause
