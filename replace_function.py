#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# 读取原文件
with open('app/src/main/java/com/example/funlife/ui/components/SpinWheel.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# 读取新的ResultAnimation函数
with open('temp_result_animation.kt', 'r', encoding='utf-8') as f:
    new_function = f.read()

# 找到ResultAnimation函数的起始和结束行
start_line = -1
end_line = -1

for i, line in enumerate(lines):
    if '// 结果动画 - 全新精美设计' in line or '// 结果动画 - 紧凑精美设计' in line:
        start_line = i
    if start_line != -1 and '// 连抽结算动画' in line:
        end_line = i
        break

if start_line == -1 or end_line == -1:
    print(f"Error: Could not find function boundaries. start={start_line}, end={end_line}")
    exit(1)

print(f"Found ResultAnimation from line {start_line} to {end_line}")

# 替换函数
new_lines = lines[:start_line] + [new_function + '\n\n'] + lines[end_line:]

# 写回文件
with open('app/src/main/java/com/example/funlife/ui/components/SpinWheel.kt', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print("Successfully replaced ResultAnimation function")
