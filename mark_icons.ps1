# Read file
$content = Get-Content -Path "app/src/main/java/com/example/funlife/ui/screens/RegisterScreen.kt" -Raw -Encoding UTF8

# Mark each icon with a unique comment
$content = $content -replace '(// 用户名输入框[\s\S]{1,500}?leadingIcon = \{[\s\S]{1,50}?)// 可爱的狗爪图标', '$1// ICON_USERNAME'
$content = $content -replace '(// 昵称输入框[\s\S]{1,500}?leadingIcon = \{[\s\S]{1,50}?)// 可爱的狗爪图标', '$1// ICON_NICKNAME'
$content = $content -replace '(// 密码输入框[\s\S]{1,500}?leadingIcon = \{[\s\S]{1,50}?)// 可爱的狗爪图标', '$1// ICON_PASSWORD'
$content = $content -replace '(// 确认密码输入框[\s\S]{1,500}?leadingIcon = \{[\s\S]{1,50}?)// 可爱的狗爪图标', '$1// ICON_CONFIRM'
$content = $content -replace '(// 内测码输入框[\s\S]{1,500}?leadingIcon = \{[\s\S]{1,50}?)// 可爱的狗爪图标', '$1// ICON_BETACODE'

# Write back
$content | Set-Content -Path "app/src/main/java/com/example/funlife/ui/screens/RegisterScreen.kt" -Encoding UTF8 -NoNewline

Write-Host "Marked all icons!"
