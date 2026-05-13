@echo off
echo ========================================
echo VIP个人主页系统 - 编译和测试脚本
echo ========================================
echo.

echo [1/5] 清理项目...
call gradlew clean
if %errorlevel% neq 0 (
    echo 清理失败！
    pause
    exit /b 1
)
echo 清理完成！
echo.

echo [2/5] 编译项目...
call gradlew assembleDebug
if %errorlevel% neq 0 (
    echo 编译失败！请检查错误信息。
    pause
    exit /b 1
)
echo 编译成功！
echo.

echo [3/5] 检查设备连接...
adb devices
if %errorlevel% neq 0 (
    echo ADB未找到或设备未连接！
    pause
    exit /b 1
)
echo.

echo [4/5] 卸载旧版本（清除数据）...
adb uninstall com.example.funlife
echo 旧版本已卸载
echo.

echo [5/5] 安装新版本...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo 安装失败！
    pause
    exit /b 1
)
echo.

echo ========================================
echo 安装成功！
echo ========================================
echo.
echo 测试步骤：
echo 1. 打开应用
echo 2. 登录账号
echo 3. 进入个人主页
echo 4. 测试以下功能：
echo    - 点击头像上传
echo    - 点击头像框按钮
echo    - 点击背景按钮
echo    - 点击每日签到
echo    - 查看VIP特权
echo.
echo 按任意键退出...
pause > nul
