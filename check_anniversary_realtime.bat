@echo off
echo ========================================
echo 纪念日实时更新日志监控
echo ========================================
echo.
echo 请在手机上添加纪念日，观察日志输出
echo 按 Ctrl+C 停止监控
echo.
echo ========================================
echo.

D:\Androidsdk\platform-tools\adb.exe -s 3URNU24B26102576 logcat -c
D:\Androidsdk\platform-tools\adb.exe -s 3URNU24B26102576 logcat | findstr /i "AnniversaryViewModel AnniversaryScreen"

pause
