# PacMaze 长按摇杆自动化：landscape 2400x1080, 左下角摇杆区
param(
    [ValidateSet("RIGHT","LEFT","UP","DOWN")]
    [string]$Dir = "RIGHT",
    [int]$HoldMs = 5000
)

# 156dp @ density 450 ≈ 438px zone; center ≈ (220, 861)
$cx = 220
$cy = 861
$drag = 120

switch ($Dir) {
    "RIGHT" { $tx = $cx + $drag; $ty = $cy }
    "LEFT"  { $tx = $cx - $drag; $ty = $cy }
    "UP"    { $tx = $cx; $ty = $cy - $drag }
    "DOWN"  { $tx = $cx; $ty = $cy + $drag }
}

adb shell input motionevent DOWN $cx $cy
Start-Sleep -Milliseconds 80
adb shell input motionevent MOVE $tx $ty
Start-Sleep -Milliseconds $HoldMs
adb shell input motionevent UP $tx $ty
