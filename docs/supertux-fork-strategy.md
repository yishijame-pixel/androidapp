# SuperTux Fork × FunLife 企业级战略与实施规划

> **文档性质：** 企业级技术战略 + 分期交付路线图（**先设计、后开发**）  
> **读者：** 产品、客户端、引擎、QA、法务、运维  
> **关联文档：**  
> - `[platformer-supertux-integration-plan.md](./platformer-supertux-integration-plan.md)` — **改编模式**（Kotlin 资源转换，非 1:1）  
> - `[game-assets-static-hosting.md](./game-assets-static-hosting.md)` — CDN / manifest  
> **上游：** [SuperTux/supertux](https://github.com/SuperTux/supertux)（引擎 GPL v2+ · `data/` 多许可）  
> **版本：** v1.2 · 2026-06-26  
> **状态：** **Phase 1 进行中**（upstream 已同步 · Linux CI 已配置）  
> **产品决策（2026-06-26）：** ✅ **已确认** — Fork 经典模式 = FunLife 1:1 主线；Kotlin 改编 = fallback  
> **产品路线图：** [`supertux-funlife-product-roadmap.md`](./supertux-funlife-product-roadmap.md)

---

## 文档控制

| 项 | 内容 |
|----|------|
| 战略代号 | **ST-FORK**（SuperTux Fork 经典模式） |
| 产品入口（规划） | 趣玩中心 → 横版冒险 → **「南极 · 经典模式」** |
| 与主线关系 | FunLife 横版 1–74 + 改编南极 901–910 **并行**；经典模式 **独立引擎** |
| 目标 fidelity | **100%** 复刻 SuperTux 关卡玩法（同 `.stl`、同物理、同脚本） |
| 仓库规划 | `reference-assets/supertux`（只读上游）→ `engine/supertux-fork`（FunLife 维护 fork） |
| Android 交付物 | `libsupertux2.so` + `data/` + FunLife 壳 Activity |

### 修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.2 | 2026-06-26 | 产品确认 Fork 主线；bootstrap + Linux CI Phase 1 |
| v1.1 | 2026-06-26 | Phase 0 脚手架：engine/supertux-fork、build-guide、ClassicActivity |
| v1.0 | 2026-06-26 | 首版：双轨战略、Fork 分期、角色 Mod、合规、与改编模式边界 |

### 术语表

| 术语 | 定义 |
|------|------|
| **改编模式** | FunLife Kotlin 引擎 + 离线转换 `.stl` → bundle；**不** 1:1 |
| **经典模式** | Fork SuperTux C++ 引擎；**1:1** 关卡与玩法 |
| **Mod** | 在 fork 上替换 UI / 角色 sprite / Logo，**不**重写 tile/物理/脚本 |
| **data/** | SuperTux 游戏数据（关卡、图、音）；与引擎分离打包 |
| **.sprite** | SuperTux 精灵描述（动作名 + 帧序列 + hitbox） |

---

## 一、执行摘要

### 1.1 核心结论

若目标是 **100% 复刻 SuperTux**（地图、autotile、敌人、Boss、脚本、物理一致），**不应**在 FunLife Kotlin 里继续「解析 `.stl` + 自实现」。  
**正确路径：** **Fork SuperTux 官方源码 → Mod 成 FunLife 子产品**，Compose 壳负责账号、选关、CDN、排行榜。

| 维度 | 改编模式（已做） | Fork 经典模式（本战略） |
|------|------------------|-------------------------|
| 引擎 | FunLife `PlatformerPhysics` | SuperTux C++ / SDL |
| 关卡 | 转换 `#` / visual tile id | 原生 `.stl` + Squirrel |
| 1:1 | ❌ 永远近似 | ✅ 可验收 |
| 角色 | 坤坤等 catalog | 需 `.sprite` Mod（见 §六） |
| 工期 | 2–4 周 MVP | Phase 0–2 约 6–10 周首包 |
| 维护 | 与主线同栈 | Fork 跟进 upstream + 自有 patch |

### 1.2 产品双轨（推荐）

```
┌─────────────────────────────────────────────────────────┐
│                    FunLife App (Compose)                 │
├──────────────────────────┬──────────────────────────────┤
│  坤坤大冒险 · 主线 1–74     │  南极 · 改编 901–910          │
│  + 高空 / 群英              │  (platformer_supertux v3)   │
│  Kotlin 引擎               │  Kotlin 引擎 · 非 1:1        │
├──────────────────────────┴──────────────────────────────┤
│  南极 · 经典模式 901–910  ──►  SuperTuxForkActivity       │
│                               libsupertux2.so + data/     │
│                               100% 原关 · 默认 Tux        │
└─────────────────────────────────────────────────────────┘
```

**原则：** 经典模式 **不** 阻塞主线发版；改编模式 **不** 再投入追 1:1。

---

## 二、战略依据（为何 Fork，而非复刻）

SuperTux 已包含（二十年积累）：

| 子系统 | 上游位置 | Kotlin 重写成本 |
|--------|----------|-----------------|
| 地图 / 多 layer tilemap | `.stl` + 编辑器 | 极高 |
| Autotile | `autotiles.satc` + `tiles.strf` | 高 |
| 碰撞 / 物理 | `src/object/player.cpp`, `tile` | 极高 |
| 敌人 AI / Boss | `src/badguy/` | 极高 |
| Squirrel 脚本 | `*.nut`, sector script | 需 VM |
| 粒子 / 相机 / 存档 | 引擎内 | 高 |

**企业结论：** 在 Kotlin 里重写 = 第二个 SuperTux 项目；**Mod fork** = 行业常见做法（开源底座 + 自有 UI/联网/角色）。

---

## 三、范围与边界

### 3.1 本战略 **包含**

- Fork 仓库与 patch 管理策略  
- PC / Android 编译与 CI  
- FunLife Activity 集成（启动参数：关卡 id、语言、存档路径）  
- 品牌替换（Icon、名称、关于页、Credits）  
- 角色 Mod 框架（可配置 `.sprite` 路径）  
- 与 FunLife 账号 / 解锁 / 排行榜的 **JNI 或后端 API** 接口设计  
- GPL 合规与 `ATTRIBUTION`  
- 分期验收标准（Phase 0–9）

### 3.2 本战略 **不包含**（首期）

- 将 FunLife **1–74 关** 迁入 SuperTux 格式  
- 世界地图 `worldmap.stwm` 完整复刻（可 Phase 8+）  
- 编辑器对外发布  
- iOS / Web（SuperTux 有 emscripten，另立项）

### 3.3 与改编模式文档关系

| 文档 | 用途 | 开发状态 |
|------|------|----------|
| `platformer-supertux-integration-plan.md` | Kotlin bundle、CDN、901–910 改编 | **已实施**，冻结 feature，仅 bugfix |
| **本文档** | Fork 经典模式 1:1 | **Phase 0 起开发** |

---

## 四、目标架构

### 4.1 仓库布局（规划）

```
d:/soft/
├── reference-assets/supertux/     # 浅克隆 upstream，只读对照
├── engine/supertux-fork/          # FunLife fork（git remote → 自有 GitHub）
│   ├── src/                       # 继承 upstream + patches/
│   ├── data/                      # 可 symlink 或 submodule 至 upstream data
│   ├── mk/android/                # SDL Activity 工程
│   └── patches/
│       ├── 001-funlife-brand.patch
│       ├── 002-configurable-player-sprite.patch
│       └── 003-jni-funlife-bridge.patch
├── app/                           # FunLife 主 App
│   └── .../SuperTuxClassicActivity.kt   # 新建
└── docs/
    ├── supertux-fork-strategy.md  # 本文档
    └── platformer-supertux-integration-plan.md
```

### 4.2 运行时架构

```
┌─────────────────── FunLife Compose Shell ───────────────────┐
│  PlatformerLevelSelectUi                                     │
│    ├─ 「改编南极」→ PlatformerScreen (Kotlin, level 901–910) │
│    └─ 「经典南极」→ SuperTuxClassicActivity                  │
│           │ intent: level=world1/welcome_antarctica.stl      │
│           │         save_dir=...                             │
│           ▼                                                  │
│  ┌─────────────────────────────────────────────┐             │
│  │ SDLActivity → libsupertux2.so → SDL_main    │             │
│  │   PHYSFS: assets/data/ 或 CDN 解压目录       │             │
│  └─────────────────────────────────────────────┘             │
│           │ onExit: score, time, completed → FunLife        │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 数据交付

| 方式 | 说明 | 阶段 |
|------|------|------|
| **APK assets** | `assetpack` 或 expansion；首包 World1 | Phase 2 |
| **CDN 增量** | 与 `assets.yishi.site` 同 manifest 机制，下载 `data/world2/` | Phase 7+ |
| **版本 pin** | fork 内 `source_pin.json` 记录 upstream commit | Phase 0 |

---

## 五、合规与授权（法务必读）

| 组件 | 许可 | FunLife 义务 |
|------|------|--------------|
| SuperTux **引擎**（C++） | **GPL v2+** | 分发 fork 二进制须满足 GPL（提供对应源码、修改说明） |
| **data/** 多数素材 | GPL v2+ / **CC-BY-SA 3.0** 等 | Credits / ATTRIBUTION；SA 要求衍生素材同许可 |
| FunLife 自有 UI / JNI | 项目自有许可 | 与 GPL 引擎 **动态链接**（`libsupertux2.so`）通常可行，**须法务确认** |

**交付物：**

- App 内 **「开源许可 / Credits」** 页（链到 fork 源码 tarball 或 GitHub）  
- `engine/supertux-fork/NOTICE` 列出修改文件  
- 不在商店描述中暗示「官方 SuperTux 应用」（品牌 Mod）

---

## 六、角色 Mod 策略（FunLife IP 接入经典模式）

### 6.1 事实：Player 硬编码于 C++

upstream `Player` 构造函数加载：

```cpp
MovingSprite({0, 0}, "images/creatures/tux/tux.sprite", ...);
```

**Fork 必须 patch：** 启动时从配置文件 / JNI 读取 `player_sprite`（默认仍为 `tux.sprite`）。

### 6.2 `.sprite` 契约（节选）

| SuperTux action 前缀 | 用途 | 坤坤/IP 策略 |
|----------------------|------|--------------|
| `small-stand` / `small-walk` / `small-run` | 地面 | **P0 必做** |
| `small-jump` / `small-fall` | 空中 | **P0**；无 fall 则 **映射 jump** |
| `small-duck` / `small-crawl` | 蹲伏 | P1 补帧或 **映射 walk** |
| `small-swim` / `small-slide` | 水/滑 | P1 或 **删关卡内水体** |
| `grow` / `shrink` / `fire` | 能力 | P2 或限制关卡无火球 |
| `gameover` / `win` | 结算 | P0 |

### 6.3 三档实施（与商业项目一致）

| 档位 | 做法 | 适用阶段 |
|------|------|----------|
| **A 补动画** | AI/美术补全 `.sprite` 动作 | 品牌上线前 |
| **B 动作映射** | Fall→Jump、Slide→Walk | **Phase 5 MVP** |
| **C 删玩法** | 选无游泳/无蹲关 | 快速试玩 |

### 6.4 与 FunLife catalog 关系

| 角色来源 | 经典模式 | 改编模式 |
|----------|----------|----------|
| 坤坤 CHICK_PRO_MAX | 需 `ikun.sprite` + patch | ✅ 已支持 |
| platformer_characters | 需逐角色转 `.sprite` | ✅ catalog |
| Tux | ✅ 默认 | ✅ v3 bundle |

**Phase 5 前经典模式默认仅 Tux**；坤坤作为 Phase 5 里程碑，不阻塞 Phase 2 Android 跑通。

---

## 七、分期交付（Phase 0–9）

> **门禁：** 每 Phase 必须验收通过，才进入下一 Phase 开发。

### Phase 0 — 仓库与合规基线（1 周）

| 任务 | 验收标准 |
|------|----------|
| 创建 `engine/supertux-fork`，remote 指向 FunLife 组织 | 可 `git push` |
| 记录 upstream pin commit（与 `reference-assets/supertux` 一致或更新） | `source_pin.json` |
| 添加 `NOTICE`、GPL 说明、`patches/README.md` | 法务 review 通过 |
| CI：仅编译 PC Linux x64（或 Windows） | GitHub Actions green |

**退出标准：** fork 可构建；文档齐全；**不修改 gameplay**。

---

### Phase 1 — PC 运行 SuperTux（1–2 周）

| 任务 | 验收标准 |
|------|----------|
| 按 upstream `INSTALL.md` 编译 | 可执行文件启动 |
| 加载 `data/levels/world1/welcome_antarctica.stl` | 与 upstream 同关可通关 |
| 录屏对比：tile、敌人、coin 行为 | QA 签字「PC 基线 OK」 |

**退出标准：** PC 上 **901 对应关** 1:1 可玩（Tux）。

---

### Phase 2 — Android 运行（2–4 周，高风险）

| 任务 | 验收标准 |
|------|----------|
| 配置 NDK + vcpkg（见 `mk/android/README.md`） | 文档化版本号 |
| `./tools/bootstrap-android-project.sh` + gradle assembleDebug | 产出 APK |
| 真机 USB 安装，进入 welcome_antarctica | 可玩 ≥5 分钟无崩溃 |
| `data/` 打包策略（asset pack / 内置） | 首包 ≤ 目标体积（产品定，建议 <150MB） |

**风险：** 官方 README 标注 **"Good luck"**；需预留 NDK/vcpkg 排障 buffer。

**退出标准：** **不依赖 FunLife App**，独立 SuperTux APK 可玩南极第一关。

---

### Phase 3 — 品牌 Mod（1 周）

| 任务 | 验收标准 |
|------|----------|
| 替换应用名、Icon、splash | 商店素材就绪 |
| 启动标题 / 关于页 FunLife 品牌 | 无 SuperTux 默认商标误用 |
| Credits 保留上游作者 | 合规 |

---

### Phase 4 — UI 替换 / Compose 壳（2 周）

| 方案 | 说明 |
|------|------|
| **A 轻壳（推荐）** | FunLife Compose 选关 → 启动 `SuperTuxClassicActivity`，游戏内 UI 仍用 SuperTux |
| **B 深改** | fork 内改 C++ 菜单 | 工作量大 |

**验收：** 从 FunLife 选关进入经典模式，返回键回到 FunLife 选关页。

---

### Phase 5 — 可配置角色 sprite（2–3 周）

| 任务 | 验收标准 |
|------|----------|
| patch `Player` 读 `player_sprite` 路径 | JNI 传入或 assets 配置 |
| 默认 `tux.sprite` 回归通过 | Phase 1 用例仍过 |
| 可选 `ikun.sprite` v1（映射档 B） | walk/jump/fall 可玩 |
| 文档：`docs/supertux-player-sprite-spec.md` | 动作对照表 |

---

### Phase 6 — FunLife 账号与进度（2–3 周）

| 任务 | 验收标准 |
|------|----------|
| JNI：`onLevelComplete(levelId, time, coins)` | Kotlin 收到回调 |
| 解锁 901–910 与 `PlatformerUnlockProgress` 对齐或独立 key | 产品设计 |
| 存档目录隔离 | 不覆盖 FunLife 主存档 |

---

### Phase 7 — 联网与 CDN（2–4 周）

| 任务 | 验收标准 |
|------|----------|
| 可选：CDN 下载 `data/world2/` | manifest + SHA-256 |
| 排行榜 / 好友（若产品要） | API 设计评审 |

---

### Phase 8 — 扩展关卡与内容（持续）

| 任务 | 验收标准 |
|------|----------|
| World1 全关 901–920 映射表 | 每关 PC/Android  smoke |
| 自研 `.stl` 进 fork `data/levels/custom/` | 编辑器导出 |

---

### Phase 9 — 与 FunLife 内容汇合（可选，长期）

| 任务 | 说明 |
|------|------|
| 评估 1–74 是否永久保留 Kotlin 引擎 | **默认：保留** |
| 若统一引擎：仅当产品强制，单独立项 | 不在 ST-FORK 范围 |

---

## 八、FunLife App 集成设计（Phase 4+）

### 8.1 新建组件（规划）

| 组件 | 职责 |
|------|------|
| `SuperTuxClassicActivity` | 继承或嵌入 `SDLActivity` |
| `SuperTuxClassicLauncher` | intent 参数、data 路径、关卡 stl |
| `SuperTuxClassicBridge` | JNI 回调 → 解锁 / 排行榜 |
| 选关 UI 入口 | `PlatformerLevelSelectUi` 增加「经典模式」Tab |

### 8.2 Intent 契约（草案）

```kotlin
// 启动经典模式
SuperTuxClassicLauncher.start(
    context = context,
    levelStl = "levels/world1/welcome_antarctica.stl",
    playerSprite = "images/creatures/tux/tux.sprite", // Phase 5 可改 ikun
    saveSlot = "funlife_user_${userId}",
)
```

### 8.3 与改编模式共存

- **同一 levelId 901** 可有两个入口：「改编」「经典」  
- 改编入口继续用 `platformer_supertux` bundle v3+  
- 经典入口 **不** 下载该 bundle，使用 fork 内 `data/`

---

## 九、构建与环境（Android Checklist）

### 9.1 工具链（参考 upstream `mk/android/README.md`）

| 工具 | 版本要求 |
|------|----------|
| Android NDK | 与 upstream CI 对齐（README 示例 r29） |
| vcpkg | Android triplet：`arm64-android` 等 |
| CMake | upstream 指定 |
| Gradle | `mk/android/gradle/wrapper` |

### 9.2 环境变量 / `local.properties`

```properties
vcpkg_root=/path/to/vcpkg
ndk_home=/path/to/ndk/<version>
```

### 9.3 常用命令（Phase 2 验收）

```bash
# PC（Phase 1）
cd engine/supertux-fork && mkdir build && cd build
cmake .. && cmake --build .

# Android（Phase 2）
cd engine/supertux-fork
./tools/bootstrap-android-project.sh <SDL_VERSION>
cd mk/android && ./gradlew assembleDebug
./deploy-debug-adb.sh
```

### 9.4 CI 策略

| 阶段 | CI |
|------|-----|
| Phase 0–1 | Linux PC build + smoke |
| Phase 2+ | Android arm64-v8a debug APK artifact |
| 每 PR | 不得破坏 `welcome_antarctica` 可启动 |

---

## 十、风险登记册

| ID | 风险 | 概率 | 影响 | 缓解 |
|----|------|------|------|------|
| F1 | Android 编译失败 / NDK 地狱 | 高 | 高 | Phase 2 独立 buffer；参考 upstream nightly APK |
| F2 | GPL 合规疏漏 | 低 | 高 | 法务 review；源码链接 |
| F3 | 双引擎维护成本 | 中 | 中 | 经典/改编入口分离；改编冻结 |
| F4 | 角色 Mod 动作不足 | 中 | 中 | Phase 5 映射档；默认 Tux |
| F5 | APK 体积过大 | 中 | 中 | asset pack；World 分包 CDN |
| F6 | upstream 合并冲突 | 中 | 中 | patch 目录；定期 rebase 策略 |
| F7 | 团队 C++ 能力缺口 | 中 | 高 | Phase 0 培训；外部顾问 |

---

## 十一、工作量估算（人日）

| Phase | 内容 | 估算 |
|-------|------|------|
| 0 | Fork + 合规 + PC CI | 5–8 |
| 1 | PC 跑通 + QA 基线 | 5–10 |
| 2 | Android 首包 | **15–25** |
| 3 | 品牌 | 3–5 |
| 4 | FunLife Activity 壳 | 5–8 |
| 5 | 角色 sprite 可配置 + ikun v1 | 10–15 |
| 6 | 账号/解锁 JNI | 8–12 |
| 7 | CDN / 排行榜 | 10–20 |
| **至 Phase 4 可对外 Beta** | | **33–56** |
| **至 Phase 5 坤坤可选** | | **+10–15** |

---

## 十二、开放决策（产品 / 法务确认后再开发）

| # | 决策 | 选项 | 建议 |
|---|------|------|------|
| D1 | 经典模式命名 | 「SuperTux 经典」/「南极 · 官方引擎」 | 避免商标混淆，用后者 |
| D2 | Phase 2 首包关卡 | 仅 welcome / World1 10 关 | 仅 welcome，最小验证 |
| D3 | 默认角色 | Tux / 坤坤 | Phase 2–4 仅 Tux |
| D4 | GPL 源码提供方式 | App 内链接 / 随 APK assets | GitHub public fork |
| D5 | 改编模式去留 | 保留 / 经典上线后隐藏 | **保留** 作低网速 fallback |
| D6 | levelId 901 双入口 | 是否允许同 ID 两种玩法 | 允许，UI 区分 |

---

## 十三、开发启动门禁（Definition of Ready）

**在写第一行 fork 代码前，必须满足：**

- [x] 产品确认 **双轨策略**（§1.2）— **2026-06-26 用户确认：Fork 主线**  
- [ ] 法务确认 **GPL / CC-BY-SA** 分发方式（§五）  
- [ ] 运维确认 **Android CI** 机器与 NDK 版本（§九）  
- [x] 指定 **Phase 0 负责人**（C++ / 构建）— 仓库脚手架已就绪  
- [x] 改编模式文档标注 **冻结**（见关联文档修订）  
- [x] 本文件 **v1.0 评审通过**（产品路线见 `supertux-funlife-product-roadmap.md`）

---

## 十四、附录

### A. 901–910 与 World1 源文件映射（经典模式）

| levelId | 源 STL | 改编模式（Kotlin）状态 |
|---------|--------|----------------------|
| 901 | `world1/welcome_antarctica.stl` | bundle v3 |
| 902 | `world1/journey_begins.stl` | 同上 |
| 903 | `world1/fork_in_the_road.stl` | 同上 |
| 904 | `world1/frosted_fields.stl` | 同上 |
| 905 | `world1/bouncy_mountainside.stl` | 同上 |
| 906 | `world1/stone_cold.stl` | 同上 |
| 907 | `world1/into_stars.stl` | 同上 |
| 908 | `world1/path_in_clouds.stl` | 同上 |
| 909 | `world1/more_snowballs.stl` | 同上 |
| 910 | `world1/night_chill.stl` | 同上 |

经典模式 **直接使用上列 STL 路径**，无需转换。

### B. 参考链接

| 资源 | URL |
|------|-----|
| SuperTux GitHub | https://github.com/SuperTux/supertux |
| Android README | `reference-assets/supertux/mk/android/README.md` |
| Level Format Wiki | https://github.com/SuperTux/supertux/wiki/Level-Format |
| 改编模式规划 | `./platformer-supertux-integration-plan.md` |

### C. 后续子文档（Phase 0 起陆续创建）

| 文档 | 创建时机 | 状态 |
|------|----------|------|
| `docs/supertux-fork-build-guide.md` | Phase 0 | **已创建** |
| `docs/supertux-player-sprite-spec.md` | Phase 5 | 待创建 |
| `docs/supertux-funlife-jni-api.md` | Phase 6 | 待创建 |

---

## 十五、修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-06-26 | 首版：Fork 战略、Phase 0–9、角色 Mod、双轨产品、开发门禁 |
