@echo off
echo ========================================
echo 测试纪念日添加功能
echo ========================================
echo.
echo 1. 清除旧日志
D:\Androidsdk\platform-tools\adb.exe -s 3URNU24B26102576 logcat -c
echo.
echo 2. 请在手机上添加一个纪念日...
echo 3. 添加完成后按任意键查看日志
pause
echo.
echo ========================================
echo 查看纪念日相关日志：
echo ========================================
D:\Androidsdk\platform-tools\adb.exe -s 3URNU24B26102576 logcat -d | findstr /i "Anniversary"
echo.
echo ========================================
echo 查看用户会话日志：
echo ========================================
D:\Androidsdk\platform-tools\adb.exe -s 3URNU24B26102576 logcat -d | findstr /i "UserSession"
echo.
echo ========================================
echo 查看数据库日志：
echo ========================================
D:\Androidsdk\platform-tools\adb.exe -s 3URNU24B26102576 logcat -d | findstr /i "Room"
echo.
pause
