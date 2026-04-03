@echo off
echo ========================================
echo 用户数据隔离测试脚本
echo ========================================
echo.
echo 测试步骤：
echo.
echo 1. 清除应用数据（重要！）
echo    adb shell pm clear com.example.funlife
echo.
echo 2. 测试用户A
echo    - 注册用户名: testA
echo    - 密码: 123456
echo    - 昵称: 用户A
echo    - 内测码: 223498
echo    - 添加一个纪念日（例如：生日）
echo.
echo 3. 退出登录
echo    - 点击"我的" -> "退出登录"
echo.
echo 4. 测试用户B
echo    - 注册用户名: testB
echo    - 密码: 123456
echo    - 昵称: 用户B
echo    - 内测码: 223498
echo    - 检查是否能看到用户A的纪念日（应该看不到）
echo    - 添加一个不同的纪念日（例如：结婚纪念日）
echo.
echo 5. 退出登录，用户A重新登录
echo    - 用户名: testA
echo    - 密码: 123456
echo    - 检查是否只能看到自己的纪念日
echo.
echo 6. 测试新用户首次添加
echo    - 注册用户名: testC
echo    - 添加纪念日，应该立即显示
echo.
echo ========================================
echo 按任意键开始清除应用数据...
pause > nul

echo.
echo 正在清除应用数据...
adb shell pm clear com.example.funlife

echo.
echo ========================================
echo 应用数据已清除！
echo 现在可以开始测试了。
echo.
echo 提示：
echo - 每次测试前都要清除数据
echo - 注意观察日志输出
echo - 检查不同用户的数据是否隔离
echo ========================================
echo.
pause
