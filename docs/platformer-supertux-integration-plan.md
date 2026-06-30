# SuperTux × 横版冒险（Platformer）企业级集成规划

> **版本**: 1.3  
> **日期**: 2026-06-26  
> **状态**: **改编模式 · 全量关卡已实施（107 关 · bundle v4）** — 非 1:1 复刻  
> **适用范围**: FunLife Android · 横版冒险 · SuperTux **World1/2/Bonus/Redmond 全关卡**
> **上游项目**: [SuperTux/supertux](https://github.com/SuperTux/supertux)（GPL v2+ 引擎 + data/ 双授权素材）

> **重要：** 若目标是 **100% 复刻** SuperTux 关卡与玩法（脚本/Boss/sector），请走 **Fork 经典模式**（**FunLife 已确认为主线**），见  
> **[`docs/supertux-fork-strategy.md`](./supertux-fork-strategy.md)** · **[`docs/supertux-funlife-product-roadmap.md`](./supertux-funlife-product-roadmap.md)**  
> **本文档** 仅描述 **Kotlin 引擎 + 离线 bundle** 的「改编」路线，作 **fallback**，非 1:1 主线。

---

## 1. 文档目的

本文档定义将 **SuperTux 游戏资源**（关卡、贴图、音效、音乐、部分玩法对象）集成进 FunLife **横版冒险 Kotlin 引擎** 的方案（**改编模式**），包括：

- 可集成内容的 ** exhaustive 清单**
- 与现有 **ResourceStore / content_catalog / 片段关卡** 架构的对齐方式
- **离线转换管线**、云端 bundle、运行时加载的分层设计
- **授权合规**、QA、CI、风险与分期交付计划

**不在范围内：**

- **100% 复刻 SuperTux** → 见 [`supertux-fork-strategy.md`](./supertux-fork-strategy.md)  
- 嵌入 SuperTux C++ 引擎、运行 Squirrel 脚本  
- Pac-Maze 俯视迷宫复用 SuperTux 关卡

### 1.1 双轨产品定位

| 模式 | 文档 | 引擎 | fidelity |
|------|------|------|----------|
| **改编南极** 901–931 等 | 本文档 | FunLife Kotlin | 近似，可玩 |
| **经典南极** 901+ | `supertux-fork-strategy.md` | SuperTux fork | 100% |

---

## 2. 执行摘要

| 维度 | 结论 |
|------|------|
| **目标玩法** | 横版冒险（Platformer）— 与 SuperTux 同为重力 + 跳跃 + tile 碰撞 |
| **集成方式** | **离线转换 + 云端资源包**；运行时只读 FunLife 格式，不解析 `.stl` |
| **素材可用性** | 贴图 / 音效 / BGM **可用**（CC-BY-SA / GPL v2+ 为主，逐文件核对） |
| **关卡可用性** | `.stl` **需转换**；长关用滚动片段；脚本与 sector 逻辑 **不移植** |
| **当前缺口** | ~~Platformer 无音效系统~~；~~无 SuperTux tileset~~；~~无 STL 导入工具~~ → **Phase 1–2 已落地** |
| **推荐首期** | 全量 **107 关**（901–1018）+ 横版音效 |
| **1:1 复刻** | **不在本文档** → Fork 战略 Phase 0 起 |
| **预估工期** | MVP 2–3 周 · 完整南极 World1 包 6–8 周 · 多 World 12+ 周 |

---

## 3. 设计原则（企业级）

1. **格式归一**：运行时只认 `content_catalog.json`、`tileset_manifest.json`、`sfx_manifest.json`、烘焙 `rows`/TMX，不认 SuperTux 原生格式。  
2. **Bundle 解耦**：SuperTux 内容独立 zip（`platformer_supertux_*`），不塞进 `platformer_characters`，避免角色包体积膨胀。  
3. **可选下载**：南极章节可设为「扩展包」，主线未通关也可试玩第一关。  
4. **合规可追溯**：每个 bundle 含 `LICENSE.txt`、`ATTRIBUTION.json`，与 `data/AUTHORS` 映射。  
5. **CI 门禁**：转换后关卡必须通过 `PlatformerLevelValidator` + 连通性脚本。  
6. **渐进降级**：资源缺失时回退 `buildEmergencyWorld`，不闪退。  
7. **不破坏主线**：新章节使用 **新 levelId 段**（建议 75–94），不改动现有 1–74 关脚本。

---

## 4. FunLife 现状对齐

### 4.1 已有能力（可直接复用）

| 模块 | 路径 | 用途 |
|------|------|------|
| 关卡构建 | `PlatformerLevels.buildWorld()` | 片段脚本 / TMX / rows 三路径 |
| 长关滚动 | `PlatformerCampaignScrollRunner` + `PlatformerSegmentLevelFactory` | SuperTux 宽关卡切片拼接 |
| TMX 房间 | `PlatformerTmxRoomPainter`（STORY_ROOM） | 嵌入 28 格宽房间 |
| 外部 tileset | `PlatformerPackTilesetLoader` | desert/winter/forest 等 128px 包 |
| 内容目录 | `PlatformerContentCatalog` + `content_catalog.json` | 角色/敌人/地砖/章节元数据 |
| 云端资源 | `ResourceStore` + `assets.yishi.site` manifest | zip 下载、SHA-256、缓存 |
| 音效参考 | `PacMazeSfx` + `dist/pac_maze_sfx/sfx_manifest.json` | Platformer 音效包可照此建模 |
| 关卡校验 | `PlatformerLevelValidator` | CI 连通性 / 预算 / 脚本完整性 |

### 4.2 当前缺口

| 缺口 | 影响 |
|------|------|
| Platformer 无 `SoundPool`/BGM | 即使导入 SuperTux 音效也无法播放 |
| 无 `PlatformerTilesetPack.SUPERTUX` | 无法渲染冰雪地砖 |
| 无 STL→FunLife 转换工具 | 无法批量导入关卡 |
| `GameResourceBundles.gameBootOrder` 未含 SuperTux 包 | 需扩展 boot 或按需下载 |
| 敌人视觉仅 catalog 远程动画 | SuperTux 敌人需映射到现有 type 或新 catalog 条目 |

### 4.3 关卡 ID 规划

| 段 | ID 范围 | 用途 | 状态 |
|----|---------|------|------|
| 主线 | 1–52 | `PlatformerCampaignLevelCatalog` | 已有 |
| 故事 TMX | 17–22 | STORY_ROOM 嵌入 | 已有（Marian & Robin） |
| 群英 | 63–74 | `PlatformerHeroLevelCatalog` | 已有 |
| 高空 | 75–84（Sky） | `PlatformerSkyLevelCatalog` | 已有（需核对实际起始 ID） |
| **SuperTux 南极** | **建议 901–920** | 独立扩展章节，避免与 Sky 冲突 | **待建** |

> **注**：`PlatformerLevels` 中 Sky 起始为 `PlatformerSkyLengthSpec.SKY_LEVEL_START`，集成前需在代码中确认空闲 ID 段；本文档建议用 **900+ 扩展 ID** 与主线彻底隔离。

---

## 5. SuperTux 可集成资产总清单

### 5.1 关卡与世界（Level & World）

| 类别 | 源路径 | 格式 | 数量级 | 集成优先级 | FunLife 产出 |
|------|--------|------|--------|------------|--------------|
| 主线关卡 World1 南极 | `data/levels/world1/*.stl` | S-expression v2 | ~40+ 可玩关 | **P0** | 烘焙 rows + segment 脚本 |
| World2 森林 | `data/levels/world2/*.stl` | 同上 | ~30+ | P1 | 同上 |
| World3 城堡/地下 | `data/levels/world3/*.stl` | 同上 | ~30+ | P2 | 同上 |
| World4 / bonus | `data/levels/bonus1/` 等 | 同上 | 若干 | P3 | 同上 |
| 世界地图 | `data/levels/world1/worldmap.stwm` | 世界地图 tile | 1/世界 | **P3（UI 用）** | 选关地图 PNG 或简化节点图 |
| 关卡脚本 | `*.nut` + `init-script` | Squirrel | 每关 0–n | **不集成** | 丢弃或手工重写关键节点 |
| Sector 多区域 | `.stl` 内多 `(sector ...)` | 逻辑分区 | 部分关 | P2 | 仅导入 `main` sector；其余忽略 |

**推荐首批转换关卡（World1）**

| 源文件 | 理由 | 目标 levelId |
|--------|------|--------------|
| `intro.stl` | 最小，验证管线 | 901 |
| `welcome_antarctica.stl` | 教学向 | 902 |
| `journey_begins.stl` | 中等长度 | 903 |
| `fork_in_the_road.stl` | 分叉路线 | 904 |
| `frosted_fields.stl` | 标准战斗 | 905 |
| `castle_of_nolok.stl` | Boss 关（简化） | 920 |

---

### 5.2 贴图与 Tileset（Graphics / Tiles）

| 类别 | 源路径 | 格式 | 集成优先级 | FunLife 产出 |
|------|--------|------|------------|--------------|
| 主 tile 定义 | `data/images/tiles.strf` | S-expression + PNG 引用 | **P0** | `platformer_supertux/tilesets/antarctic/` |
| 自动贴图集 | `data/images/autotiles.satc` | SuperTux autotile | P0 | 烘焙为 32×32 或 128×128 单格 PNG 序列 |
| 背景层 | `data/images/background/*.jpg/png` | 位图 | **P0** | `backgrounds/antarctic_*.webp` |
| 前景装饰 | tilemap z-pos 100 | tile id | P1 | DECO 层或 `PlatformerCell.DECO` |
| 对象精灵 | `data/images/objects/**` | PNG 动画 | P1 | 敌人/道具 catalog 或静态 object 层 |
| 粒子/天气 | rain/snow/comets in `.stl` | 粒子系统 | P3 | 用 Compose 粒子简化模拟 |
| UI / 字体 | SuperTux 菜单资源 | — | **不集成** | 继续用 FunLife UI |

**Tile 物理属性（strf `attributes`）映射**

| SuperTux 属性 | FunLife `PlatformerCell` | MVP 策略 |
|---------------|--------------------------|----------|
| solid | `#` SOLID | 直接映射 |
| unisolid（单向板） | `-` PLATFORM | P1：查 strf；MVP 当 SOLID |
| ice | 保留 SOLID + 摩擦系数 | P2：扩展 `PlatformerPhysics` |
| water | AIR + 区域 hazard | P3 |
| slope | — | **不支持**；转换时拉平或近似阶梯 |

---

### 5.3 音效 SFX（`data/sounds/`，约 89 文件）

完整清单（按用途分类，便于 `sfx_manifest.json` 映射）：

#### 5.3.1 玩家动作（P0）

| SuperTux 文件 | 建议事件 ID | 说明 |
|---------------|-------------|------|
| `jump.wav` | `player_jump` | 普通跳 |
| `bigjump.wav` | `player_big_jump` | 大跳/弹簧 |
| `hop.ogg` | `player_hop` | 小跳变体 |
| `fall.wav` / `retro_fall.wav` | `player_fall` | 空中 |
| `thud.ogg` | `player_land` | 落地 |
| `hurt.wav` | `player_hurt` | 受伤 |
| `kill.wav` | `player_die` | 死亡 |
| `skid.wav` | `player_skid` | 急停（可选） |

#### 5.3.2 交互与收集（P0）

| SuperTux 文件 | 建议事件 ID |
|---------------|-------------|
| `coin.wav` / `coin2.ogg` | `pickup_gem` |
| `coins_cleared.ogg` | `level_all_gems` |
| `lifeup.wav` | `pickup_life` |
| `grow.ogg` / `upgrade.wav` | `power_up` |
| `switch.ogg` / `turnkey.ogg` | `switch_toggle` |
| `door.wav` | `door_open` |
| `trampoline.wav` | `spring_bounce` |
| `warp.wav` | `portal` |

#### 5.3.3 战斗与敌人（P1）

| SuperTux 文件 | 建议事件 ID |
|---------------|-------------|
| `stomp.wav` / `kick.wav` | `enemy_stomp` |
| `squish.wav` | `enemy_squish` |
| `shoot.wav` / `dartfire.wav` | `player_shoot` |
| `darthit.wav` | `projectile_hit` |
| `explosion.wav` | `explosion` |
| `mr_tree.ogg` / `mr_treehit.ogg` | `enemy_special` |
| `ghoul_stunned.ogg` | `enemy_stun` |

#### 5.3.4 环境（P1–P2）

| SuperTux 文件 | 建议事件 ID |
|---------------|-------------|
| `splash.ogg` / `splash.wav` | `water_splash` |
| `brick.wav` | `block_break` |
| `iceblock_bump.wav` / `icecrash.ogg` | `ice_hit` |
| `lava.wav` / `sizzle.ogg` | `lava_burn` |
| `rain.wav` | `ambience_rain` |
| `waterfall.wav` | `ambience_waterfall` |

#### 5.3.5 UI / 流程（P0）

| SuperTux 文件 | 建议事件 ID |
|---------------|-------------|
| `welldone.ogg` / `tada.ogg` / `excellent.wav` | `level_clear` |
| `savebell2.wav` | `checkpoint` |
| `locked.ogg` | `locked` |
| `invincible_start.ogg` | `invincible_start` |

#### 5.3.6 暂不集成（P3 或丢弃）

`phone.wav`, `empty.wav`, `convert_to_mono.sh`, `normalize.sh`, 部分 crystallo-* 主题音（非南极章）、`evil_nolok_jingle.ogg`（Boss 专用可 P2 加入）

---

### 5.4 背景音乐 BGM（`data/music/`）

| 主题目录 | 典型用途 | 优先级 | FunLife 映射 |
|----------|----------|--------|--------------|
| `antarctic/` | World1 关卡 BGM | **P0** | `bgm_supertux_antarctic_*` loop |
| `forest/` | World2 | P1 | 章节 bgm |
| `castle/` | World3 | P2 | 章节 bgm |
| `tropical/` | 特殊关 | P3 | — |
| `retro/` | 复古关 | P3 | — |
| `misc/` | 菜单/过场 | P2 | 选关/胜利 |

**实现**：参照 `PacMazeSfx` 的 BGM 循环逻辑，新建 `PlatformerSfx` + `MediaPlayer`/`ExoPlayer` 单轨 BGM + `SoundPool` 短音效。

---

### 5.5 敌人与对象（Badguys & Objects）

SuperTux `.stl` 内对象 → FunLife 映射表（MVP 用 **行为等价**，视觉可用现有 catalog 敌人或占位）：

| SuperTux badguy | 行为 | FunLife 映射 | 视觉策略 |
|-----------------|------|--------------|----------|
| `snowball` | 巡逻 | `PlatformerEnemyType.SLIME` | 现有 slime 或新 `supertux_snowball` 精灵 |
| `snail` | 巡逻慢 | `SNAIL` | 远程 catalog |
| `skullyhop` / `spiky` | 巡逻 | `SKULL` / `MUSHROOM` | catalog |
| `flyingsnowball` / `zeekling` | 飞行 | `BAT` + `FLY` | catalog |
| `mrbomb` / `mrbomb` | 爆炸 | `PlatformerTrapType` 或敌人 | P2 |
| `stalactite` / `spike` | 静态伤害 | `PlatformerCell.SPIKE` / trap | tile 层 |
| `coin` / `bonus` | 收集 | `PlatformerGem` | 宝石逻辑 |
| `spawnpoint` | 出生 | `@` SPAWN / player init | 必须 |
| `door` / `secretarea` | 脚本门 | **忽略** | P3 手工放 GOAL |
| `yeti` (boss) | Boss AI | 专用 Boss 段 | P2 |

---

### 5.6 明确不集成项

| 项 | 原因 |
|----|------|
| SuperTux 引擎源码（C++） | 与 Kotlin/Compose 栈割裂 |
| Squirrel 脚本（`*.nut`, `init-script`） | 无解释器；逻辑需重写 |
| 多 Sector 传送 | 无 sector 运行时 |
| 精确斜坡/单向板物理 | 需扩展 `PlatformerPhysics` |
| Autoscroll 相机路径 | 需新 camera 模式 |
| 水下游泳、爬墙（Tux 特有状态） | 角色能力模型不同 |
| SuperTux 主菜单 / 世界地图交互 | FunLife 自有 Hub |

---

## 6. 目标架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        SuperTux upstream                         │
│  data/levels/*.stl  data/images/*  data/sounds/*  data/music/* │
└────────────────────────────┬────────────────────────────────────┘
                             │  clone + pin commit
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              backend/tools/ (离线转换管线)                         │
│  import_supertux_tileset.py   → dist/platformer_supertux/tilesets│
│  import_supertux_levels.py    → dist/platformer_supertux/levels  │
│  import_supertux_sfx.py       → dist/platformer_sfx/curated/st/  │
│  validate_supertux_bundle.py  → CI 门禁                          │
│  pack_platformer_supertux.ps1 → zip + manifest 条目              │
└────────────────────────────┬────────────────────────────────────┘
                             │  upload assets.yishi.site
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  App resource_cache/                                             │
│  platformer_supertux/   platformer_sfx/   platformer_characters/ │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  Runtime (Kotlin)                                                │
│  PlatformerSuperTuxCatalog · PlatformerSfx · SUPERTUX tileset   │
│  PlatformerLevels.buildWorld · Segment STORY_ROOM · ScrollRunner │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. 资源包（Bundle）设计

### 7.1 新增 Bundle 清单

| Bundle ID | 内容 | 预估体积 | Boot 策略 |
|-----------|------|----------|-----------|
| `platformer_supertux` | tileset + backgrounds + 关卡 JSON/TMX + ATTRIBUTION | 15–40 MB | **按需**：进入南极章下载 |
| `platformer_sfx` | 横版 SFX+BGM manifest（含 SuperTux 子集 + 可保留 Kenney 补充） | 5–15 MB | 与 `pac_maze_sfx` 分离或合并 vol2 |
| （可选）`platformer_supertux_world2` | 森林扩展 | +20 MB | 按需 |

### 7.2 目录结构（`dist/platformer_supertux/`）

```
platformer_supertux/
├── bundle_version.txt          # 整数递增
├── ATTRIBUTION.json            # 逐文件授权映射
├── LICENSE.txt                 # CC-BY-SA 3.0 + GPL v2+ 声明
├── content_catalog.json        # 章节/关卡/ tileset 索引
├── tilesets/
│   └── antarctic/
│       ├── tileset_manifest.json
│       ├── tiles/01.png …
│       └── background.webp
├── levels/
│   ├── 901_intro/
│   │   ├── level_manifest.json # 元数据、spawn、goal、源 stl 路径
│   │   ├── rows.txt            # 烘焙字符矩阵（可选）
│   │   └── segments.json       # 片段脚本（长关）
│   └── …
└── source_pin.json             # SuperTux git commit hash
```

### 7.3 `content_catalog.json` 扩展示例

```json
{
  "schemaVersion": 2,
  "bundleVersion": 1,
  "chapters": [{
    "id": "supertux_antarctic",
    "title": "南极探险",
    "subtitle": "SuperTux World 1 · 改编",
    "levelRange": [901, 920],
    "tilesetId": "supertux_antarctic",
    "theme": "PACK_SUPERTUX",
    "bgmEvent": "bgm_supertux_antarctic",
    "unlock": { "type": "default", "value": 0 },
    "attribution": "SuperTux Team · CC-BY-SA / GPL v2+"
  }],
  "levels": [
    { "id": 901, "sourceStl": "world1/intro.stl", "title": "企鹅入门" }
  ]
}
```

### 7.4 manifest 与 ResourceStore 改动要点

- `ResourceStore.BUNDLE_IDS` 增加 `platformer_supertux`  
- `GameResourceBundles` 增加 **可选** `ensureSuperTuxBundle()`，不在全局 boot 强制（避免首包过大）  
- `isBundleContentCurrent`：marker 文件 `platformer_supertux/content_catalog.json`  
- `PacMazeResourceUpdateNotifier`：进入南极 Hub 时 `ensureBundleWithProgress`

---

## 8. 离线转换管线（Tooling）

### 8.1 工具清单

| 工具 | 输入 | 输出 | 阶段 |
|------|------|------|------|
| `import_supertux_tileset.py` | `tiles.strf` + PNG | tileset 目录 + manifest | Phase 1 |
| `import_supertux_level.py` | `.stl` | rows / segments / spawn / enemies JSON | Phase 1 |
| `import_supertux_sfx.py` | `data/sounds/*` | `platformer_sfx` curated + manifest 条目 | Phase 2 |
| `import_supertux_music.py` | `data/music/antarctic/*` | ogg + manifest | Phase 2 |
| `validate_platformer_supertux.py` | dist 目录 | 报告（连通性、授权、缺失 tile） | Phase 1 |
| `pack_platformer_supertux.ps1` | dist | zip + 更新 `assets_public/manifest.json` | Phase 1 |

### 8.2 STL 解析规则（`import_supertux_level.py`）

1. 解析 S-expression（括号 + 字符串 + atom）  
2. 取 `(sector (name "main") ...)` 或第一个 sector  
3. 合并所有 `(tilemap (solid #t) ...)` 为碰撞层（tile id ≠ 0 → solid）  
4. 读取 `(spawnpoint (name "main") (x Px) (y Py))` → 格坐标（SuperTux 1 tile ≈ 32px，与 `PLATFORMER_TILE_PX` 对齐）  
5. 扫描 badguys → `PlatformerEnemySpawn` JSON  
6. 宽地图：按 `SEGMENT_W=28` 切片 → `SegmentSpec` 列表 + 可选 STORY_ROOM TMX  
7. 输出 `PlatformerLevelValidator` 可消费的 `PlatformerLevelDef` 烘焙 rows  

### 8.3 坐标与尺度

| 项 | SuperTux | FunLife |
|----|----------|---------|
| Tile 像素 | 32×32（标准） | `PLATFORMER_TILE_PX = 32` |
| 关卡高度 | 可变 | 烘焙为 14 行视口 + 滚动 |
| 玩家高 | ~1.75 tile | `PlatformerPhysics.playerH()` 已对齐 |

---

## 9. 运行时改动清单

### 9.1 新增 / 修改 Kotlin 模块

| 文件 | 改动 |
|------|------|
| `PlatformerTypes.kt` | `PlatformerTheme.PACK_SUPERTUX`, `PlatformerTilesetPack.SUPERTUX` |
| `PlatformerPackTilesetLoader.kt` | 加载 `platformer_supertux/tilesets/antarctic` |
| `PlatformerSuperTuxCatalog.kt` | **新建**：901+ 关卡 manifest |
| `PlatformerLevels.kt` | `all` 合并 SuperTux 章（可配置开关） |
| `PlatformerContentCatalog.kt` | 解析 `supertuxChapter` |
| `PlatformerSfx.kt` + `PlatformerAudioManager.kt` | **新建**：照抄 PacMaze 模式 |
| `PlatformerScreen` / ViewModel | 跳跃/收集/受伤/通关触发 SFX |
| `PlatformerRenderer.kt` | SUPERTUX 背景 parallax |
| `ResourceStore.kt` | 新 bundle id + marker |
| `GameResourceBundles.kt` | 可选扩展包枚举 |
| Hub UI | 「南极探险」章节入口 + 下载进度 |

### 9.2 片段脚本策略（长关）

**方案 A（推荐）**：整关转为 `campaignSegmentScript`，类型以 `GAP`/`ENEMY_ROOM`/`STORY_ROOM` 为主，关键房间用转换出的 TMX。

**方案 B**：整关烘焙为超长 `rows` + `PlatformerCampaignScrollRunner`（适合 >200 tile 宽）。

**方案 C**：短关（intro）直接 `rows` + 不滚动。

---

## 10. 授权与合规

### 10.1 默认许可证

- **代码**：GPL v3（不嵌入，仅工具链读取仓库）  
- **data/**：GPL v2+ **且** CC-BY-SA 3.0（双选）  
- **策略**：FunLife 使用 **CC-BY-SA** 路径分发转换后素材；保留 `ATTRIBUTION.json`

### 10.2 必须交付物

1. App 内 **设置 → 开源许可 → SuperTux 致谢**  
2. 每个 zip 内 `LICENSE.txt` + `ATTRIBUTION.json`  
3. 对 CC-BY 3.0 / CC0 单列文件（见 `data/AUTHORS`）  
4. 修改过的 PNG/OGG 在 attribution 中标注「Adapted for FunLife」

### 10.3 法律评审触发条件

- 闭源商业发行  
- 将 SuperTux BGM 与第三方收费音乐混用  
- 对外销售「SuperTux 素材包」独立分发  

---

## 11. QA / CI / 验收

### 11.1 自动化

| 检查 | 命令/位置 |
|------|-----------|
| 关卡 manifest | `:app:testDebugUnitTest` + `PlatformerLevelValidatorTest`（扩展） |
| SuperTux bundle 完整性 | `validate_platformer_supertux.py` |
| 授权文件存在 | CI grep `ATTRIBUTION.json` |
| 体积预算 | zip < 50MB/章 |

### 11.2 手工验收（每关）

- [ ] 出生点合法、不卡墙  
- [ ] 终点/Goal 可达  
- [ ] 主路径低路连通（`PlatformerLevelDesign` 原则）  
- [ ] 敌人可踩/可伤  
- [ ] 宝石计数合理  
- [ ] BGM/SFX 无爆音；BGM 循环无缝（可选）  
- [ ] 资源缺失降级不 crash  

### 11.3 分期验收标准

| 阶段 | 验收 |
|------|------|
| Phase 0 | 工具链解析 `intro.stl` 输出合法 JSON |
| Phase 1 | 901 关可玩通关 + 冰雪 tile 可见 |
| Phase 2 | 903 关 + 12 SFX + 1 BGM |
| Phase 3 | World1 精选 10 关 + Hub 章节 |
| Phase 4 | World2 森林 tileset + 5 关 |

---

## 12. 分期路线图

### Phase 0 — 准备（3–5 天）

- [x] Pin SuperTux commit；法务确认 CC-BY-SA 路径  
- [x] 确认 levelId 段（901–910）  
- [x] 搭建 `backend/tools/import_supertux_platformer.py`  
- [x] 文档评审（本文档 v1.1）

### Phase 1 — 垂直切片 MVP（7–10 天）

- [x] antarctic tileset → `platformer_supertux/tilesets/antarctic`  
- [x] 10 关 STL → 901–910  
- [x] Runtime：`SUPERTUX` tileset + `PlatformerSuperTuxLevelCatalog`  
- [x] Hub 「南极 901–910」筛选 + 独立解锁  
- [x] 单元测试 + `validate_platformer_supertux.py`

### Phase 2 — 音频（5–7 天）

- [x] `PlatformerSfx` + `platformer_sfx/sfx_manifest.json`  
- [x] 16 事件 SFX/BGM（SuperTux 精选）  
- [x] `PlatformerScreen` 全局事件接线（跳跃/落地/宝石/踩踏/射击/死亡/通关）  
- [ ] 可选：与 `pac_maze_sfx` 共用 AudioManager 基类 refactor

### Phase 3 — 南极章节（10–14 天）

- [x] 转换 **107 关**（World1/2/Bonus/Redmond）  
- [x] `platformer_supertux` zip v4 + assets 镜像  
- [x] Hub 多章节筛选 + 按需 ensureBundle  
- [x] `validate_platformer_supertux.py` + push 脚本  
- [x] 云端 `manifest.json` 条目（CDN 发布）  
- [x] Credits 页（设置 → 开源许可）  
- [x] `validate_platformer_supertux.py` 进 CI（`.github/workflows/platformer-supertux-validate.yml`）

### Phase 4 — 扩展（可选）

- [x] World2 森林章 941–978（改编 bundle）  
- [x] Bonus1 981–1010、Redmond 1011–1018  
- [ ] Boss yeti 简化战（改编引擎能力有限）  
- [ ] 世界地图选关 UI  
- [ ] 敌人 SuperTux 精灵 catalog（非占位）

---

## 13. 风险登记册

| ID | 风险 | 概率 | 影响 | 缓解 |
|----|------|------|------|------|
| R1 | STL 含复杂脚本关无法玩 | 高 | 中 | 关卡白名单；跳过关卡 |
| R2 | strf tile 与渲染不一致 | 中 | 高 | 可视化 diff 工具；人工抽检 |
| R3 | 单向板/斜坡物理不符 | 高 | 中 | MVP 当 solid；P2 扩展 physics |
| R4 | 授权文件遗漏 | 低 | 高 | ATTRIBUTION 自动生成 + CI |
| R5 | Bundle 体积超移动网络阈值 | 中 | 中 | 分 world 分包；Wi-Fi 提示 |
| R6 | Platformer 无音频延期 | 中 | 中 | Phase 2 专期；可先 silent MVP |
| R7 | levelId 与 Sky 冲突 | 低 | 高 | 固定 900+ 段 |

---

## 14. 工作量估算（人日）

| 工作包 | 估算 |
|--------|------|
| 转换工具链（tile + level + validate） | 8–12 |
| Runtime tileset + catalog + 1 关 | 5–7 |
| PlatformerSfx 全栈 | 5–8 |
| 10 关转换 + 调优 | 10–15 |
| Hub + ResourceStore + 下载 UX | 4–6 |
| QA + 文档 + 合规 | 3–5 |
| **MVP 合计** | **22–32** |
| World1 全精选 + World2 试点 | +15–25 |

---

## 15. 开放决策（待产品确认）

1. **章节命名**：「南极探险」vs「SuperTux 经典」—— 是否显式使用 SuperTux 商标字样。  
2. **levelId**：901–920 是否与 Sky 段冲突 — 需开发确认 `SKY_LEVEL_START`。  
3. **Boot 是否预下载**：全量用户下载 vs 进入章节再拉。  
4. **角色**：南极章固定「行走小鸡 Pro Max」还是任选 catalog 角色。  
5. **敌人视觉**：占位现有敌人 vs 导入 SuperTux 雪球精灵。  
6. **音效包**：独立 `platformer_sfx` vs 扩展现有 `pac_maze_sfx`（不推荐混 game 字段）。

---

## 16. 附录 A — SuperTux `data/sounds/` 完整文件名

共 **89** 项（含脚本），音频文件 **87**：

`bigjump.wav`, `brick.wav`, `coin.wav`, `coin2.ogg`, `coins_cleared.ogg`, `cracking.wav`, `crystallo-pop.ogg`, `crystallo-shardhit.ogg`, `crystallo-shatter.ogg`, `dartfire.wav`, `darthit.wav`, `door.wav`, `evil_nolok_jingle.ogg`, `excellent.wav`, `explosion.wav`, `fall.wav`, `fire-flower.wav`, `fire.ogg`, `firecracker.ogg`, `fireworks.wav`, `fizz.wav`, `flame.wav`, `flip.wav`, `flop.ogg`, `ghoul_recovering.ogg`, `ghoul_stunned.ogg`, `grow.ogg`, `grow.wav`, `grunts.ogg`, `gulp.wav`, `hop.ogg`, `hurt.wav`, `iceblock_bump.wav`, `icecrash.ogg`, `invincible_start.ogg`, `jump.wav`, `kick.wav`, `kill.wav`, `lava.wav`, `lifeup.wav`, `lightning.wav`, `locked.ogg`, `metal_hit.ogg`, `mr_tree.ogg`, `mr_treehit.ogg`, `phone.wav`, `pop.ogg`, `pshit.ogg`, `rain.wav`, `retro_fall.wav`, `savebell2.wav`, `savebell_low.wav`, `saw.wav`, `shoot.wav`, `sizzle.ogg`, `skid.wav`, `splash.ogg`, `splash.wav`, `splat.wav`, `squish.wav`, `stomp.wav`, `switch.ogg`, `tada.ogg`, `thud.ogg`, `thunder.wav`, `ticking.wav`, `totem.ogg`, `trampoline.wav`, `tree_hit.ogg`, `tree_howling.ogg`, `tree_pinch.ogg`, `tree_suck.ogg`, `turnkey.ogg`, `upgrade.wav`, `warp.wav`, `waterfall.wav`, `welldone.ogg`

---

## 17. 附录 B — 与现有文档交叉引用

| 文档 | 关系 |
|------|------|
| **`docs/supertux-funlife-product-roadmap.md`** | **Fork 主线产品路线（角色/UI/商城/排行榜）** |
| **`docs/supertux-fork-strategy.md`** | **100% 复刻 / Fork 经典模式**（FunLife 已确认主线） |
| `docs/game-assets-static-hosting.md` | 新 zip 上传 manifest 流程 |
| `dist/pac_maze_sfx/sfx_manifest.json` | Platformer sfx manifest 模板 |
| `app/.../PlatformerContentCatalog.kt` | content_catalog 扩展点 |
| `app/.../PlatformerSegmentLibrary.kt` | 长关片段类型 |
| SuperTux Wiki [Level Format](https://github.com/SuperTux/supertux/wiki/Level-Format) | STL 语法参考 |

---

## 18. 修订记录

| 版本 | 日期 | 作者 | 说明 |
|------|------|------|------|
| 1.3 | 2026-06-26 | — | 全量 107 关 bundle v4；Credits；CI |
| 1.2 | 2026-06-26 | — | 标注「改编模式」；指向 Fork 战略；冻结追 1:1 |
| 1.1 | 2026-06-26 | — | 已实施（待验收） |
| 1.0 | 2026-06-26 | AI 规划 | 初稿：全量清单 + 架构 + 分期 |

---

**下一步建议**：评审 §15 开放决策 → 批准 Phase 0 → 提供本地 SuperTux 克隆路径 → 启动 `import_supertux_level.py` 与 901 关垂直切片。
