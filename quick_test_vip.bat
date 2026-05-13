@echo off
chcp 65001 >nul
cls
echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║                                                            ║
echo ║           🎉 VIP激活动画快速测试工具 🎉                   ║
echo ║                                                            ║
echo ╚════════════════════════════════════════════════════════════╝
echo.
echo 📱 应用已启动！
echo.
echo ════════════════════════════════════════════════════════════
echo 🎯 测试步骤：
echo ════════════════════════════════════════════════════════════
echo.
echo   1️⃣  登录账号（或注册新账号）
echo   2️⃣  进入VIP页面
echo   3️⃣  点击"输入兑换码"
echo   4️⃣  输入：HZ223498
echo   5️⃣  点击"立即兑换"
echo   6️⃣  欣赏华丽动画！
echo.
echo ════════════════════════════════════════════════════════════
echo 🎨 预期效果：
echo ════════════════════════════════════════════════════════════
echo.
echo   👑 超级华丽的金色皇冠
echo   💎 三颗紫色宝石闪烁
echo   ⭐ 12颗星星环绕
echo   💫 光芒爆发特效
echo   🌧️  30个金币从天而降
echo   💰 获得1000金币奖励
echo   🎊 终生VIP身份激活
echo.
echo ════════════════════════════════════════════════════════════
echo 🔑 其他测试兑换码：
echo ════════════════════════════════════════════════════════════
echo.
echo   VIP2024   → 普通VIP + 100金币  (金色徽章)
echo   SUPERVIP  → 年费VIP + 500金币  (蓝紫徽章)
echo   HZ223498  → 终生VIP + 1000金币 (金色皇冠) ⭐推荐
echo.
echo ════════════════════════════════════════════════════════════
echo 🛠️  快捷操作：
echo ════════════════════════════════════════════════════════════
echo.
echo   [1] 查看实时日志
echo   [2] 清除数据重新测试
echo   [3] 查看测试指南
echo   [4] 退出
echo.
set /p choice="请选择操作 (1-4): "

if "%choice%"=="1" (
    echo.
    echo 正在启动日志监控...
    start cmd /k "d:\soft\monitor_vip_logs.bat"
    goto menu
)

if "%choice%"=="2" (
    echo.
    echo 正在清除应用数据...
    D:\Androidsdk\platform-tools\adb.exe shell pm clear com.example.funlife
    echo.
    echo ✅ 数据已清除！请重新登录测试。
    timeout /t 3 >nul
    goto menu
)

if "%choice%"=="3" (
    echo.
    echo 正在打开测试指南...
    start notepad d:\soft\VIP_TEST_GUIDE.md
    goto menu
)

if "%choice%"=="4" (
    echo.
    echo 👋 感谢使用！祝测试愉快！
    timeout /t 2 >nul
    exit
)

echo.
echo ❌ 无效选择，请重新输入。
timeout /t 2 >nul

:menu
echo.
echo ════════════════════════════════════════════════════════════
echo.
set /p choice="请选择操作 (1-4): "
if "%choice%"=="1" (
    echo.
    echo 正在启动日志监控...
    start cmd /k "d:\soft\monitor_vip_logs.bat"
    goto menu
)
if "%choice%"=="2" (
    echo.
    echo 正在清除应用数据...
    D:\Androidsdk\platform-tools\adb.exe shell pm clear com.example.funlife
    echo.
    echo ✅ 数据已清除！请重新登录测试。
    timeout /t 3 >nul
    goto menu
)
if "%choice%"=="3" (
    echo.
    echo 正在打开测试指南...
    start notepad d:\soft\VIP_TEST_GUIDE.md
    goto menu
)
if "%choice%"=="4" (
    echo.
    echo 👋 感谢使用！祝测试愉快！
    timeout /t 2 >nul
    exit
)
echo.
echo ❌ 无效选择，请重新输入。
timeout /t 2 >nul
goto menu
