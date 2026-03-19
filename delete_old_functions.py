#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Delete old EditOptionsDialog and EditableOptionItem functions from EnhancedSpinWheelScreen.kt

file_path = "app/src/main/java/com/example/funlife/ui/screens/EnhancedSpinWheelScreen.kt"

with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Find the start of EditOptionsDialog function (around line 1749)
# and the end of EditableOptionItem function (around line 1963)
start_line = None
end_line = None
brace_count = 0
in_function = False

for i, line in enumerate(lines):
    # Find the @Composable annotation before EditOptionsDialog
    if i > 1740 and '@Composable' in line and i < 1760:
        # Check if next line has EditOptionsDialog
        if i + 1 < len(lines) and 'fun EditOptionsDialog' in lines[i + 1]:
            start_line = i
            in_function = True
            print(f"Found start at line {i + 1}: {line.strip()}")
    
    # Count braces to find the end
    if in_function:
        brace_count += line.count('{') - line.count('}')
        if brace_count == 0 and start_line is not None:
            # Check if this is after EditableOptionItem
            if i > start_line + 100:  # Should be at least 100 lines
                end_line = i + 1  # Include this line
                print(f"Found end at line {i + 1}: {line.strip()}")
                break

if start_line is not None and end_line is not None:
    print(f"\nDeleting lines {start_line + 1} to {end_line + 1}")
    new_lines = lines[:start_line] + lines[end_line:]
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
    
    print(f"Successfully deleted {end_line - start_line} lines")
    print(f"File now has {len(new_lines)} lines (was {len(lines)})")
else:
    print("Could not find the functions to delete")
    print(f"start_line: {start_line}, end_line: {end_line}")
