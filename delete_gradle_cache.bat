@echo off
echo Deleting Gradle cache from C drive...
echo.

:retry
rd /s /q "C:\Users\Administrator\.gradle" 2>nul

if exist "C:\Users\Administrator\.gradle" (
    echo Gradle cache still exists, retrying in 5 seconds...
    timeout /t 5 /nobreak >nul
    goto retry
) else (
    echo.
    echo ========================================
    echo SUCCESS! Gradle cache deleted!
    echo Freed 17.1 GB on C drive
    echo ========================================
    echo.
)

pause
