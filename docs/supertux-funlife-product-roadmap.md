# FunLife × SuperTux Fork 产品路线图

> **状态：** 已确认（2026-06-26）  
> **引擎路线：** SuperTux → Fork → Mod 成 FunLife 子产品  
> **非目标：** 解析 `.stl` 在 Kotlin 里重写 SuperTux  
> **关联：** [`supertux-fork-strategy.md`](./supertux-fork-strategy.md) · [`supertux-fork-build-guide.md`](./supertux-fork-build-guide.md)

---

## 产品架构（定稿）

```
FunLife App（Compose）
├── 账号 / 商城 / 排行榜 / 选关 / CDN
└── SuperTuxClassicActivity
         └── libsupertux2.so + data/
                 └── 原生 .stl + Squirrel + Boss + sector
```

| 层 | 技术 | 谁维护 |
|----|------|--------|
| 壳 | Kotlin Compose | FunLife |
| 引擎 | SuperTux C++ fork | FunLife + upstream merge |
| 关卡 | 官方 `data/levels/*.stl` | upstream + 可选自研 |

**改编 Kotlin bundle（901–1018）** 保留为 **低网速 / 无引擎 fallback**，非 1:1 主线。

---

## Mod 清单（你的需求 → 交付阶段）

| # | 需求 | 实现方式 | Phase | 验收 |
|---|------|----------|-------|------|
| M1 | 替换 Logo / 应用名 | fork patch + Android 资源 | 3 | 启动与商店素材为 FunLife |
| M2 | 替换 UI | Compose 选关/结算；游戏内菜单可保留引擎 | 4 | 从 FunLife 进关、返回选关 |
| M3 | Tux → FunLife 角色 | 可配置 `player.sprite` + `.sprite` 资产 | 5 | ikun/Tux 可切换 |
| M4 | 联网 / 账号 | JNI + FunLife 登录态 | 6 | 存档目录按 userId 隔离 |
| M5 | 排行榜 | `onLevelComplete` JNI → API | 7 | 通关上报时间/金币 |
| M6 | 商城 | **仅 Compose 侧**，引擎不参与 | 7+ | 购买皮肤 → 传 sprite 路径进引擎 |
| M7 | CDN 增量 data | manifest + SHA-256 | 7+ | World2 按需下载 |

---

## 角色 Mod 要点（M3）

1. Patch `Player` 构造函数读配置路径（默认 `tux.sprite`）  
2. 为 FunLife IP 制作 `ikun.sprite`（P0 动作：stand/walk/jump/fall）  
3. 缺失动作使用 **映射档 B**（fall→jump 等）  
4. 商城售卖的「皮肤」= 不同 `.sprite` 路径，经 JNI 传入

---

## 商城与 GPL（法务备忘）

| 模块 | 建议 |
|------|------|
| 引擎 `libsupertux2.so` | GPL 源码公开（GitHub fork） |
| FunLife 商城 / 支付 | 闭源可行，**不**链进 GPL 静态库；仅 JNI 调引擎 |
| 角色皮肤 PNG | 自有版权或 CC；写入 `data/images/creatures/` |
| 商店文案 | 不写「官方 SuperTux App」 |

---

## 当前进度

| Phase | 内容 | 状态 |
|-------|------|------|
| 0 | 脚手架、NOTICE、build-guide | ✅ |
| 1 | upstream 同步 + PC CI 编译 | 🔄 进行中 |
| 2 | Android NDK + APK | ⏳ |
| 3–7 | 上表 M1–M7 | ⏳ |

---

## 开发者命令

```powershell
# 同步 upstream → engine/supertux-fork
powershell -File backend/tools/bootstrap_supertux_fork.ps1

# Linux PC 构建（本地或 CI）
cd engine/supertux-fork
cmake -B build -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build build
```

---

## 修订记录

| 日期 | 说明 |
|------|------|
| 2026-06-26 | 产品确认：Fork 主线；Mod/商城/排行榜路线图 |
