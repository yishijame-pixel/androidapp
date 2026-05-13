@echo off
chcp 65001 >nul
echo ========================================
echo VIP激活实时日志监控
echo ========================================
echo.
echo 正在清除旧日志...
D:\Androidsdk\platform-tools\adb.exe logcat -c
echo.
echo 开始监控VIP相关日志...
echo ========================================
echo.
D:\Androidsdk\platform-tools\adb.exe logcat | findstr /i "VipRepository VipViewModel VipScreen VipActivation"
