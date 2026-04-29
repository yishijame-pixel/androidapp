# AI 生成宠物美术资源完整教程

## 📋 目录
1. [工具选择](#工具选择)
2. [提示词（Prompt）编写技巧](#提示词编写技巧)
3. [分步生成指南](#分步生成指南)
4. [后期处理](#后期处理)
5. [实战案例](#实战案例)

---

## 🛠️ 一、工具选择

### 1.1 推荐的 AI 工具

#### 免费工具
1. **Stable Diffusion (本地部署)**
   - 优点：完全免费，无限生成
   - 缺点：需要配置环境，需要显卡
   - 适合：有技术基础的用户

2. **Leonardo.ai**
   - 优点：专注游戏美术，每天免费额度
   - 缺点：免费版有限制
   - 网址：https://leonardo.ai
   - 免费额度：每天 150 tokens

3. **Bing Image Creator (DALL-E 3)**
   - 优点：完全免费，质量高
   - 缺点：每天有次数限制
   - 网址：https://www.bing.com/create

4. **Playground AI**
   - 优点：每天 500 张免费
   - 网址：https://playgroundai.com

#### 付费工具（效果更好）
1. **Midjourney**
   - 价格：$10/月起
   - 优点：质量最高，风格统一
   - 网址：https://midjourney.com

2. **DALL-E 3 (ChatGPT Plus)**
   - 价格：$20/月
   - 优点：理解能力强，易用

### 1.2 本教程使用的工具
我们主要使用 **Leonardo.ai**，因为：
- ✅ 免费额度充足
- ✅ 专注游戏美术
- ✅ 界面友好
- ✅ 支持透明背景
- ✅ 有游戏风格模型

---

## 📝 二、提示词（Prompt）编写技巧

### 2.1 提示词结构

```
[主体描述] + [风格] + [细节] + [质量词] + [技术参数]
```

### 2.2 关键词汇表

#### 风格关键词
- `kawaii` - 可爱风格
- `chibi` - Q版风格
- `cartoon` - 卡通风格
- `anime` - 动漫风格
- `cute` - 萌系
- `pastel colors` - 柔和色彩
- `soft lighting` - 柔和光照
- `game art` - 游戏美术
- `mobile game` - 手游风格

#### 质量关键词
- `high quality` - 高质量
- `detailed` - 细节丰富
- `clean` - 干净
- `professional` - 专业
- `polished` - 精致
- `masterpiece` - 杰作
- `best quality` - 最佳质量

#### 技术关键词
- `transparent background` - 透明背景
- `white background` - 白色背景
- `centered` - 居中
- `full body` - 全身
- `front view` - 正面视角
- `side view` - 侧面视角
- `simple background` - 简单背景

#### 排除关键词（Negative Prompt）
- `blurry` - 模糊
- `low quality` - 低质量
- `ugly` - 丑陋
- `deformed` - 变形
- `bad anatomy` - 解剖错误
- `watermark` - 水印
- `text` - 文字

---

## 🎨 三、分步生成指南

### 步骤 1：注册 Leonardo.ai

1. 访问 https://leonardo.ai
2. 点击 "Sign Up" 注册
3. 可以用 Google 账号快速登录
4. 完成后会获得每天 150 tokens

### 步骤 2：选择合适的模型

在 Leonardo.ai 中推荐使用：
- **Leonardo Diffusion XL** - 通用高质量
- **3D Animation Style** - 3D卡通风格
- **Anime Pastel Dream** - 柔和动漫风格

### 步骤 3：生成宠物角色

#### 3.1 生成猫咪（幼年期）

**提示词模板：**
```
A cute baby kitten character for mobile game, chibi style, kawaii, 
pastel pink and white colors, big sparkling eyes, small body, 
sitting pose, happy expression, soft fur texture, 
game character design, centered, white background, 
high quality, detailed, professional game art

Negative: blurry, low quality, ugly, deformed, bad anatomy, 
watermark, text, realistic, photo
```

**参数设置：**
- Image Dimensions: 1024 x 1024
- Number of Images: 4
- Guidance Scale: 7-10

**操作步骤：**
1. 点击 "Image Generation"
2. 粘贴提示词
3. 选择模型
4. 点击 "Generate"
5. 等待 30-60 秒
6. 选择最满意的结果下载

#### 3.2 生成不同成长阶段

**少年期猫咪：**
```
A cute teenage cat character for mobile game, chibi style, kawaii,
medium size body, playful pose, energetic expression,
pastel colors, game character design, white background,
high quality, professional game art
```

**成年期猫咪：**
```
A cute adult cat character for mobile game, chibi style, kawaii,
normal size body, elegant pose, confident expression,
pastel colors, game character design, white background,
high quality, professional game art
```

**完全体猫咪：**
```
A majestic cat character for mobile game, chibi style, kawaii,
large body, royal pose, glowing effects, sparkles,
pastel colors with golden accents, game character design,
white background, high quality, professional game art
```

#### 3.3 生成不同表情/动作

**开心表情：**
```
A cute cat character, chibi style, kawaii, happy expression,
big smile, closed eyes, jumping pose, hearts around,
pastel colors, game character design, white background
```

**难过表情：**
```
A cute cat character, chibi style, kawaii, sad expression,
teary eyes, droopy ears, sitting pose, blue mood,
pastel colors, game character design, white background
```

**吃东西动作：**
```
A cute cat character, chibi style, kawaii, eating food,
happy expression, food bowl in front, sitting pose,
pastel colors, game character design, white background
```

### 步骤 4：生成背景场景

#### 4.1 基础房间背景

**提示词：**
```
Cute kawaii room interior for pet game, pastel pink walls,
wooden floor, window with sunshine, simple furniture,
cozy atmosphere, game background art, soft lighting,
no characters, clean design, high quality, 16:9 ratio

Negative: cluttered, dark, realistic, photo, people
```

**参数：**
- Image Dimensions: 1920 x 1080（横屏）
- 或 1080 x 1920（竖屏）

#### 4.2 花园背景

**提示词：**
```
Cute kawaii garden scene for pet game, green grass,
colorful flowers, butterflies, blue sky, white clouds,
sunny day, game background art, pastel colors,
no characters, clean design, high quality

Negative: dark, realistic, photo, people, animals
```

### 步骤 5：生成物品图标

#### 5.1 食物图标

**普通食物：**
```
Cute kawaii pet food icon, bowl with kibbles,
game item icon, simple design, pastel colors,
white background, centered, high quality, 512x512

Negative: realistic, photo, complex, blurry
```

**高级食物：**
```
Cute kawaii premium pet food icon, golden bowl with fish,
sparkles, game item icon, simple design, pastel colors,
white background, centered, high quality, 512x512
```

#### 5.2 玩具图标

**小球：**
```
Cute kawaii toy ball icon for pet game, colorful stripes,
shiny surface, game item icon, simple design,
white background, centered, high quality, 512x512
```

### 步骤 6：生成特效元素

#### 6.1 爱心特效

**提示词：**
```
Cute kawaii pink heart particle effect, glowing,
sparkles, game effect, transparent background,
simple design, high quality, 256x256

Negative: realistic, complex, photo
```

#### 6.2 星星特效

**提示词：**
```
Cute kawaii golden star particle effect, shining,
sparkles, game effect, transparent background,
simple design, high quality, 256x256
```

---

## 🔧 四、后期处理

### 4.1 去除背景

#### 方法1：使用在线工具
1. **Remove.bg**
   - 网址：https://www.remove.bg
   - 免费，自动去背景
   - 上传图片 → 下载透明背景版本

2. **PhotoRoom**
   - 网址：https://www.photoroom.com
   - 免费，效果好

#### 方法2：使用 Photoshop
1. 打开图片
2. 选择 "魔棒工具" 或 "快速选择工具"
3. 选中背景
4. 按 Delete 删除
5. 保存为 PNG 格式

#### 方法3：使用免费软件 GIMP
1. 下载 GIMP：https://www.gimp.org
2. 打开图片
3. 图层 → 透明 → 添加 Alpha 通道
4. 使用 "按颜色选择工具" 选中背景
5. 按 Delete 删除
6. 导出为 PNG

### 4.2 调整尺寸

#### 使用在线工具
1. **TinyPNG**
   - 网址：https://tinypng.com
   - 压缩图片，减小文件大小

2. **Squoosh**
   - 网址：https://squoosh.app
   - Google 出品，调整尺寸和压缩

#### 批量处理
使用 **XnConvert**（免费）：
1. 下载：https://www.xnview.com/en/xnconvert/
2. 添加图片
3. 设置输出尺寸
4. 批量转换

### 4.3 统一风格

**技巧：**
1. 使用相同的提示词模板
2. 使用相同的 AI 模型
3. 使用相同的参数设置
4. 保持色彩一致性

---

## 💡 五、实战案例

### 案例 1：生成完整的猫咪资源包

#### 第1步：生成基础形象
```
Prompt: A cute cat character for mobile pet game, chibi style, 
kawaii, pastel pink and white fur, big eyes, small body, 
sitting pose, happy expression, game character design, 
white background, high quality, professional

生成 4 张，选择最好的 1 张作为基础形象
```

#### 第2步：生成变体（使用 Image to Image）
1. 上传基础形象
2. 修改提示词描述不同动作
3. 调整 Image Strength: 0.6-0.8（保持风格一致）

**开心版本：**
```
Same cat character, jumping happily, hearts around, 
excited expression, same style and colors
```

**吃东西版本：**
```
Same cat character, eating from food bowl, 
happy expression, same style and colors
```

#### 第3步：批量生成
重复以上步骤，生成所有需要的动作和表情

### 案例 2：生成UI图标集

#### 统一风格的图标

**基础模板：**
```
Cute kawaii [物品名称] icon for mobile game,
simple design, pastel colors, white background,
centered, game UI icon, high quality, 512x512

Negative: realistic, complex, photo, blurry
```

**批量生成：**
1. 食物碗图标
2. 水盆图标
3. 玩具图标
4. 药品图标
5. ...

每个图标使用相同的模板，只修改 [物品名称]

---

## 📊 六、提示词速查表

### 宠物角色提示词

| 需求 | 提示词 |
|------|--------|
| 幼年期 | baby, small, tiny, cute, innocent |
| 少年期 | teenage, medium size, playful, energetic |
| 成年期 | adult, normal size, elegant, confident |
| 完全体 | majestic, large, royal, glowing, sparkles |
| 开心 | happy, smile, jumping, hearts, excited |
| 难过 | sad, teary eyes, droopy, blue mood |
| 饥饿 | hungry, looking at food, drooling |
| 睡觉 | sleeping, curled up, zzz, peaceful |
| 玩耍 | playing, running, chasing, energetic |
| 生病 | sick, tired, weak, sweating |

### 背景场景提示词

| 场景 | 提示词 |
|------|--------|
| 房间 | room interior, cozy, furniture, window, sunshine |
| 花园 | garden, grass, flowers, butterflies, sunny |
| 海滩 | beach, sand, ocean, shells, palm trees |
| 森林 | forest, trees, mushrooms, stream, sunlight |
| 城堡 | castle, stone walls, flags, throne, royal |
| 太空 | space, stars, planets, spaceship, cosmic |

### 物品图标提示词

| 物品 | 提示词 |
|------|--------|
| 食物 | pet food, bowl, kibbles, fish, meat |
| 玩具 | toy, ball, frisbee, rope, mouse |
| 装饰 | hat, scarf, glasses, bow, crown |
| 药品 | medicine, potion, bottle, pill, health |

---

## 🎯 七、常见问题解决

### Q1: 生成的图片风格不统一怎么办？
**解决方案：**
1. 使用相同的 AI 模型
2. 保存第一张满意的图片
3. 使用 Image to Image 功能生成变体
4. 保持提示词结构一致

### Q2: 背景无法完全透明？
**解决方案：**
1. 在提示词中明确写 "transparent background" 或 "white background"
2. 使用 Remove.bg 等工具后期去背景
3. 选择支持透明背景的 AI 模型

### Q3: 生成的图片质量不够高？
**解决方案：**
1. 增加质量关键词：high quality, detailed, professional
2. 提高分辨率：1024x1024 或更高
3. 调整 Guidance Scale: 7-10
4. 使用更好的 AI 模型

### Q4: 生成的角色每次都不一样？
**解决方案：**
1. 使用 Seed 值固定随机性
2. 使用 Image to Image 保持一致性
3. 详细描述角色特征（颜色、大小、特点）

### Q5: 免费额度用完了怎么办？
**解决方案：**
1. 等待第二天刷新额度
2. 注册多个账号
3. 使用其他免费工具（Bing, Playground）
4. 考虑付费订阅

---

## 📦 八、资源整理建议

### 8.1 文件命名规范
```
类型_名称_状态_编号.png

示例：
pet_cat_baby_idle_01.png
pet_cat_baby_happy_01.png
bg_room_basic.png
item_food_premium.png
effect_heart.png
```

### 8.2 文件夹结构
```
ai_generated_assets/
├── pets/
│   ├── cat/
│   │   ├── baby/
│   │   ├── child/
│   │   ├── adult/
│   │   └── perfect/
│   ├── dog/
│   ├── rabbit/
│   └── hamster/
├── backgrounds/
├── items/
├── effects/
└── ui/
```

### 8.3 质量检查清单
- [ ] 分辨率符合要求
- [ ] 背景已去除（如需要）
- [ ] 风格统一
- [ ] 文件大小合理（< 500KB）
- [ ] 命名规范
- [ ] 无水印、无文字
- [ ] 色彩鲜艳清晰

---

## 🚀 九、快速开始行动计划

### 第1天：生成核心资源
- [ ] 生成 1 种宠物（猫）的 4 个成长阶段
- [ ] 生成 5 个核心动作（待机、开心、吃、玩、洗澡）
- [ ] 生成 1 个基础背景

### 第2天：生成辅助资源
- [ ] 生成 10 个物品图标
- [ ] 生成 5 个特效元素
- [ ] 生成 UI 图标

### 第3天：后期处理
- [ ] 去除所有背景
- [ ] 调整统一尺寸
- [ ] 压缩文件大小
- [ ] 整理文件夹

### 第4天：集成测试
- [ ] 将资源放入项目
- [ ] 测试显示效果
- [ ] 调整优化

---

## 💰 十、成本估算

### 完全免费方案
- Leonardo.ai: 每天 150 tokens（约 30-50 张图）
- Bing Image Creator: 每天 15-25 张
- Playground AI: 每天 500 张
- **总计**: 每天可生成 50-100 张高质量图片
- **时间**: 3-5 天完成所有资源

### 付费加速方案
- Leonardo.ai 付费: $12/月（8500 tokens）
- **时间**: 1-2 天完成所有资源

---

## 📚 十一、学习资源

### 推荐教程
1. **Leonardo.ai 官方教程**
   - https://docs.leonardo.ai

2. **Prompt 工程指南**
   - https://www.promptingguide.ai

3. **游戏美术 AI 生成社区**
   - Reddit: r/StableDiffusion
   - Discord: Leonardo.ai 官方服务器

### 提示词库
1. **PromptHero**
   - https://prompthero.com
   - 查看其他人的提示词

2. **Lexica**
   - https://lexica.art
   - Stable Diffusion 提示词搜索

---

## ✅ 总结

使用 AI 生成宠物美术资源的关键：
1. ✅ 选择合适的工具（推荐 Leonardo.ai）
2. ✅ 编写详细的提示词
3. ✅ 保持风格统一
4. ✅ 做好后期处理
5. ✅ 有序整理资源

**预期效果：**
- 3-5 天完成所有基础资源
- 成本：$0（使用免费工具）
- 质量：中上水平，适合原型和 MVP

**下一步：**
1. 注册 Leonardo.ai 账号
2. 使用我提供的提示词模板
3. 开始生成第一批资源
4. 我帮你集成到代码中

准备好了吗？我们可以从生成第一只猫咪开始！
