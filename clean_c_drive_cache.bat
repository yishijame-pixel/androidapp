@echo off
echo ========================================
echo 清理 C 盘缓存并迁移到 D 盘
echo ========================================
echo.

echo [1/3] 删除 Gradle 旧缓存 (17.1 GB)...
rd /s /q "C:\Users\Administrator\.gradle" 2>nul
if exist "C:\Users\Administrator\.gradle" (
    echo ⚠️  Gradle 缓存删除失败（文件被占用），请关闭所有开发工具后重试
) else (
    echo ✅ Gradle 缓存已删除
)
echo.

echo [2/3] 迁移 Android SDK 缓存到 D 盘 (12.3 GB)...
if not exist "D:\.android" (
    echo 正在移动 .android 目录...
    robocopy "C:\Users\Administrator\.android" "D:\.android" /E /MOVE /R:3 /W:5 /MT:8
    if exist "D:\.android" (
        echo ✅ Android 缓存已迁移到 D:\.android
        mklink /D "C:\Users\Administrator\.android" "D:\.android"
        echo ✅ 已创建符号链接
    )
) else (
    echo ⚠️  D:\.android 已存在，跳过迁移
)
echo.

echo [3/3] 迁移 DrvPath 到 D 盘 (2.19 GB)...
if not exist "D:\DrvPath" (
    echo 正在移动 DrvPath 目录...
    robocopy "C:\DrvPath" "D:\DrvPath" /E /MOVE /R:3 /W:5 /MT:8
    if exist "D:\DrvPath" (
        echo ✅ DrvPath 已迁移到 D:\DrvPath
        mklink /D "C:\DrvPath" "D:\DrvPath"
        echo ✅ 已创建符号链接
    )
) else (
    echo ⚠️  D:\DrvPath 已存在，跳过迁移
)
echo.

echo ========================================
echo 清理完成！
echo ========================================
echo 预计释放 C 盘空间: 31.6 GB
echo.
pause
