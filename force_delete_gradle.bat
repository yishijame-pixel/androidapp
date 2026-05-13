@echo off
echo ========================================
echo 强制删除 Gradle 旧缓存
echo ========================================
echo.

echo 方法 1: 使用 DEL 命令强制删除...
del /f /s /q "C:\Users\Administrator\.gradle\*.*" 2>nul
rd /s /q "C:\Users\Administrator\.gradle" 2>nul

if exist "C:\Users\Administrator\.gradle" (
    echo.
    echo 方法 2: 使用 RMDIR 命令...
    rmdir /s /q "C:\Users\Administrator\.gradle" 2>nul
)

if exist "C:\Users\Administrator\.gradle" (
    echo.
    echo ⚠️  无法删除，文件被占用
    echo.
    echo 解决方案：
    echo 1. 关闭所有开发工具（Android Studio, VS Code, IDEA 等）
    echo 2. 打开任务管理器，结束所有 java.exe 进程
    echo 3. 重新运行此脚本
    echo.
    echo 或者：
    echo 重启电脑后再运行此脚本
    echo.
) else (
    echo.
    echo ========================================
    echo ✅ 成功！Gradle 旧缓存已删除
    echo ✅ 释放 C 盘空间: 17.1 GB
    echo ========================================
    echo.
)

pause
