@echo off
echo ========================================
echo 监控用户相关日志
echo ========================================
echo.
echo 正在监控以下标签的日志：
echo - UserSessionManager
echo - AnniversaryViewModel
echo - AuthViewModel
echo - HabitViewModel
echo - GoalViewModel
echo - MoodViewModel
echo.
echo 按 Ctrl+C 停止监控
echo ========================================
echo.

adb logcat -c
adb logcat | findstr /C:"UserSessionManager" /C:"AnniversaryViewModel" /C:"AuthViewModel" /C:"HabitViewModel" /C:"GoalViewModel" /C:"MoodViewModel" /C:"AnniversaryScreen"
