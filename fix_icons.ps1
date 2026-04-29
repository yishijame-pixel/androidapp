# Read file
$content = Get-Content -Path "app/src/main/java/com/example/funlife/ui/screens/RegisterScreen.kt" -Raw -Encoding UTF8

# Count occurrences
$count = ([regex]::Matches($content, "// Icon tubiao_1")).Count
Write-Host "Found $count icon1 references"

# Replace password field (3rd occurrence) with icon2
$pattern = '(// 密码输入框[\s\S]{1,500}?leadingIcon = \{[\s\S]{1,200}?)// Icon tubiao_1[\s\S]{1,200}?icon1Bitmap'
$replacement = '$1// Lock icon (tubiao_2)
                                icon2Bitmap'
$content = $content -replace $pattern, $replacement

# Replace beta code field (5th occurrence) with icon3  
$pattern = '(// 内测码输入框[\s\S]{1,500}?leadingIcon = \{[\s\S]{1,200}?)// Icon tubiao_1[\s\S]{1,200}?icon1Bitmap'
$replacement = '$1// Key icon (tubiao_3)
                                icon3Bitmap'
$content = $content -replace $pattern, $replacement

# Write back
$content | Set-Content -Path "app/src/main/java/com/example/funlife/ui/screens/RegisterScreen.kt" -Encoding UTF8 -NoNewline

Write-Host "Fixed icons!"
