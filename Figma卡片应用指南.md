# Figma设计卡片应用指南

## 方案一：使用图片资源（最简单）

### 步骤1：从Figma导出图片
1. 在Figma中选中你设计的VIP卡片
2. 右侧面板找到"Export"选项
3. 设置导出参数：
   - 格式：PNG
   - 分辨率：2x 或 3x（推荐3x，更清晰）
   - 背景：透明（如果需要）
4. 点击"Export"导出图片，命名为 `vip_card.png`

### 步骤2：将图片放入项目
将导出的图片放到以下任一位置：

**选项A：放入assets目录（推荐）**
```
d:\soft\app\src\main\assets\vip\vip_card.png
```

**选项B：放入drawable目录**
```
d:\soft\app\src\main\res\drawable\vip_card.png
```

### 步骤3：在代码中使用图片

#### 如果放在assets目录：
```kotlin
@Composable
fun VipCardImage() {
    val context = LocalContext.current
    val bitmap = remember {
        try {
            context.assets.open("vip/vip_card.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "VIP会员卡",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Fit
        )
    }
}
```

#### 如果放在drawable目录：
```kotlin
@Composable
fun VipCardImage() {
    Image(
        painter = painterResource(id = R.drawable.vip_card),
        contentDescription = "VIP会员卡",
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp)),
        contentScale = ContentScale.Fit
    )
}
```

---

## 方案二：使用Compose代码重现（更灵活）

我已经为你创建了 `VipMemberCard.kt` 组件，它根据你的Figma设计用代码实现。

### 使用方法：

#### 1. 在VIP页面中使用
```kotlin
// 在VipScreen.kt中
@Composable
fun VipScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 使用VIP会员卡片
        VipMemberCard(
            vipLevel = vipLevel,
            expiryDate = "2025-12-31",
            onUpgradeClick = {
                // 处理升级点击
            },
            onRenewClick = {
                // 处理续费点击
            },
            onActivateClick = {
                // 处理激活点击
            }
        )
        
        // 其他内容...
    }
}
```

#### 2. 在个人主页中使用
```kotlin
// 在VipProfileScreen.kt中
VipMemberCard(
    vipLevel = vipLevel,
    expiryDate = userVip?.expiryDate?.toString(),
    onUpgradeClick = { /* 跳转到VIP页面 */ },
    onRenewClick = { /* 显示续费对话框 */ },
    onActivateClick = { /* 显示开通对话框 */ }
)
```

### 组件特性：
- ✅ 根据VIP等级自动切换渐变色
- ✅ 闪光动画效果
- ✅ 显示会员特权
- ✅ 根据VIP状态显示不同按钮
- ✅ 完全可自定义

---

## 方案三：混合使用（最佳效果）

结合图片和代码的优势：

1. **背景使用Figma导出的图片**（保留设计细节）
2. **文字和按钮用Compose代码**（方便动态更新）

```kotlin
@Composable
fun HybridVipCard(
    vipLevel: VipLevel,
    expiryDate: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // 背景图片（从Figma导出）
        val context = LocalContext.current
        val backgroundBitmap = remember {
            try {
                context.assets.open("vip/vip_card_bg.png").use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
        
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // 前景内容（用代码实现，方便更新）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // VIP信息
            Text(
                "VIP会员",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // 到期时间
            if (expiryDate != null) {
                Text(
                    "有效期至 $expiryDate",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            // 操作按钮
            Button(
                onClick = { /* ... */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                )
            ) {
                Text("立即开通", color = Color(0xFF8B6914))
            }
        }
    }
}
```

---

## 推荐方案

根据你的需求选择：

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **方案一：纯图片** | 简单快速，100%还原设计 | 不能动态更新文字，文件较大 | 静态展示，设计复杂 |
| **方案二：纯代码** | 灵活可定制，文件小，支持动画 | 需要手动还原设计 | 需要动态内容，简单设计 |
| **方案三：混合** | 兼具两者优点 | 稍微复杂 | 复杂设计+动态内容 |

**我的建议：使用方案二（纯代码）**
- 我已经帮你创建了 `VipMemberCard.kt` 组件
- 支持动态显示VIP等级、到期时间
- 有闪光动画效果
- 完全可自定义
- 文件小，性能好

---

## 下一步

1. **如果选择方案一或方案三**：
   - 从Figma导出图片
   - 放入项目对应目录
   - 使用上面的代码加载图片

2. **如果选择方案二**：
   - 直接使用我创建的 `VipMemberCard` 组件
   - 在需要的地方调用即可

3. **测试效果**：
   ```bash
   .\gradlew.bat clean assembleDebug
   .\gradlew.bat installDebug
   ```

需要我帮你在具体页面中集成这个卡片吗？
