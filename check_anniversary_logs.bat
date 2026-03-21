@echo off
echo ========================================
echo 查看纪念日添加日志
echo ========================================
echo.
D:\Androidsdk\platform-tools\adb.exe -s 3URNU24B26102576 logcat -d | findstr "AnniversaryViewModel"
echo.
echo ========================================
echo 日志查看完成
echo ========================================
pause
