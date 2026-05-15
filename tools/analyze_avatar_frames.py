#!/usr/bin/env python3
"""
头像框自动分析工具

功能：
1. 扫描所有头像框PNG文件
2. 分析每个PNG的尺寸和透明区域
3. 自动生成Kotlin配置代码
4. 生成JSON配置文件（可用于服务器同步）

使用方法：
python analyze_avatar_frames.py
"""

import os
import json
from PIL import Image
import numpy as np
from pathlib import Path

# 配置
ASSETS_PATH = r"app\src\main\assets\xiangkuang"
OUTPUT_KOTLIN = r"app\src\main\java\com\example\funlife\data\model\AvatarFrameConfigGenerated.kt"
OUTPUT_JSON = r"tools\avatar_frame_configs.json"

def analyze_transparent_area(image_path):
    """
    分析PNG图片的透明区域
    
    返回：
    - transparent_ratio: 中心透明圆形区域占整体的比例
    - center_offset_x: X轴偏移比例
    - center_offset_y: Y轴偏移比例
    """
    try:
        img = Image.open(image_path).convert("RGBA")
        width, height = img.size
        
        # 转换为numpy数组
        img_array = np.array(img)
        alpha_channel = img_array[:, :, 3]
        
        # 找到透明区域（alpha < 128）
        transparent_mask = alpha_channel < 128
        
        # 找到透明区域的边界
        transparent_rows = np.any(transparent_mask, axis=1)
        transparent_cols = np.any(transparent_mask, axis=0)
        
        if not np.any(transparent_rows) or not np.any(transparent_cols):
            # 没有透明区域，使用默认值
            return 0.60, 0.0, 0.0
        
        # 计算透明区域的边界
        top = np.argmax(transparent_rows)
        bottom = len(transparent_rows) - np.argmax(transparent_rows[::-1])
        left = np.argmax(transparent_cols)
        right = len(transparent_cols) - np.argmax(transparent_cols[::-1])
        
        # 计算透明区域的中心
        trans_center_x = (left + right) / 2
        trans_center_y = (top + bottom) / 2
        
        # 计算透明区域的尺寸
        trans_width = right - left
        trans_height = bottom - top
        trans_diameter = min(trans_width, trans_height)
        
        # 计算透明区域占整体的比例
        transparent_ratio = trans_diameter / min(width, height)
        
        # 计算中心偏移（相对于图片中心）
        img_center_x = width / 2
        img_center_y = height / 2
        offset_x = (trans_center_x - img_center_x) / width
        offset_y = (trans_center_y - img_center_y) / height
        
        # 限制透明区域比例在合理范围内（0.50-0.80）
        transparent_ratio = max(0.50, min(0.80, transparent_ratio))
        
        return round(transparent_ratio, 2), round(offset_x, 3), round(offset_y, 3)
        
    except Exception as e:
        print(f"  ⚠️  分析失败: {e}")
        return 0.60, 0.0, 0.0

def scan_avatar_frames(base_path):
    """
    扫描所有头像框文件
    
    返回：字典 {系列路径: [配置列表]}
    """
    configs = {}
    base_path = Path(base_path)
    
    if not base_path.exists():
        print(f"❌ 路径不存在: {base_path}")
        return configs
    
    # 遍历所有子目录
    for series_dir in base_path.iterdir():
        if not series_dir.is_dir():
            continue
        
        series_name = series_dir.name
        series_path = f"xiangkuang/{series_name}"
        
        print(f"\n📁 分析系列: {series_name}")
        
        # 找到该系列的所有PNG文件
        png_files = list(series_dir.glob("*.png"))
        
        if not png_files:
            print(f"  ⚠️  没有找到PNG文件")
            continue
        
        # 分析前3个PNG文件，取平均值作为该系列的配置
        sample_files = png_files[:min(3, len(png_files))]
        
        ratios = []
        offsets_x = []
        offsets_y = []
        widths = []
        heights = []
        
        for png_file in sample_files:
            print(f"  🔍 分析: {png_file.name}")
            
            # 获取图片尺寸
            img = Image.open(png_file)
            width, height = img.size
            widths.append(width)
            heights.append(height)
            
            # 分析透明区域
            ratio, offset_x, offset_y = analyze_transparent_area(png_file)
            ratios.append(ratio)
            offsets_x.append(offset_x)
            offsets_y.append(offset_y)
            
            print(f"     尺寸: {width}x{height}, 透明区域: {ratio*100:.0f}%, 偏移: ({offset_x:.3f}, {offset_y:.3f})")
        
        # 计算平均值
        avg_width = int(np.mean(widths))
        avg_height = int(np.mean(heights))
        avg_ratio = round(np.mean(ratios), 2)
        avg_offset_x = round(np.mean(offsets_x), 3)
        avg_offset_y = round(np.mean(offsets_y), 3)
        
        # 如果偏移很小，设为0
        if abs(avg_offset_x) < 0.02:
            avg_offset_x = 0.0
        if abs(avg_offset_y) < 0.02:
            avg_offset_y = 0.0
        
        configs[series_path] = {
            "seriesPath": series_path,
            "originalWidth": avg_width,
            "originalHeight": avg_height,
            "transparentAreaRatio": avg_ratio,
            "offsetX": avg_offset_x,
            "offsetY": avg_offset_y,
            "fileCount": len(png_files)
        }
        
        print(f"  ✅ 系列配置: {avg_width}x{avg_height}, 透明区域: {avg_ratio*100:.0f}%, 偏移: ({avg_offset_x:.3f}, {avg_offset_y:.3f})")
        print(f"     共 {len(png_files)} 个文件")
    
    return configs

def generate_kotlin_code(configs):
    """生成Kotlin配置代码"""
    
    kotlin_code = '''// AvatarFrameConfigGenerated.kt - 自动生成的头像框配置
// ⚠️ 此文件由 analyze_avatar_frames.py 自动生成，请勿手动编辑
package com.example.funlife.data.model

/**
 * 自动生成的头像框配置初始化器
 * 
 * 使用方法：
 * 在 AvatarFrameConfigManager.init() 中调用 initializeGeneratedConfigs()
 */
object AvatarFrameConfigGeneratedInitializer {
    
    fun initializeGeneratedConfigs() {
'''
    
    for series_path, config in sorted(configs.items()):
        kotlin_code += f'''        
        // {config['seriesPath']} - {config['fileCount']}个文件
        AvatarFrameConfigManager.addConfigForSeries(
            seriesPath = "{config['seriesPath']}",
            originalWidth = {config['originalWidth']},
            originalHeight = {config['originalHeight']},
            transparentAreaRatio = {config['transparentAreaRatio']}f,
            offsetX = {config['offsetX']}f,
            offsetY = {config['offsetY']}f
        )
'''
    
    kotlin_code += '''    }
}
'''
    
    return kotlin_code

def generate_json_config(configs):
    """生成JSON配置文件（用于服务器同步）"""
    json_configs = []
    
    for series_path, config in sorted(configs.items()):
        json_configs.append({
            "assetPath": config['seriesPath'],
            "originalWidth": config['originalWidth'],
            "originalHeight": config['originalHeight'],
            "transparentAreaRatio": config['transparentAreaRatio'],
            "offsetX": config['offsetX'],
            "offsetY": config['offsetY']
        })
    
    return json.dumps(json_configs, indent=2, ensure_ascii=False)

def main():
    print("=" * 60)
    print("🎨 头像框自动分析工具")
    print("=" * 60)
    
    # 扫描头像框
    print(f"\n📂 扫描目录: {ASSETS_PATH}")
    configs = scan_avatar_frames(ASSETS_PATH)
    
    if not configs:
        print("\n❌ 没有找到任何头像框配置")
        return
    
    print(f"\n✅ 共分析 {len(configs)} 个系列")
    
    # 生成Kotlin代码
    print(f"\n📝 生成Kotlin配置代码...")
    kotlin_code = generate_kotlin_code(configs)
    
    with open(OUTPUT_KOTLIN, 'w', encoding='utf-8') as f:
        f.write(kotlin_code)
    
    print(f"   ✅ 已保存到: {OUTPUT_KOTLIN}")
    
    # 生成JSON配置
    print(f"\n📝 生成JSON配置文件...")
    json_config = generate_json_config(configs)
    
    with open(OUTPUT_JSON, 'w', encoding='utf-8') as f:
        f.write(json_config)
    
    print(f"   ✅ 已保存到: {OUTPUT_JSON}")
    
    print("\n" + "=" * 60)
    print("🎉 分析完成！")
    print("=" * 60)
    print("\n📋 下一步操作：")
    print("1. 在 AvatarFrameConfigManager.init() 中调用:")
    print("   AvatarFrameConfigGeneratedInitializer.initializeGeneratedConfigs()")
    print("2. 重新编译应用")
    print("3. 所有头像框将自动适配！")

if __name__ == "__main__":
    main()
