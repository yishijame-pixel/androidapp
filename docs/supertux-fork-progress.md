# SuperTux Fork 进度看板（ST-FORK）

> **最后更新：** 2026-06-30（Android CI run #6 失败，已修 bootstrap 路径）  
> **仓库：** [yishijame-pixel/androidapp](https://github.com/yishijame-pixel/androidapp)  
> **一键看 CI：** [GitHub Actions 总览](https://github.com/yishijame-pixel/androidapp/actions)

---

## 总览（一眼看懂）

```
产品决策 ✅  Fork 经典模式 = 主线
Push GitHub ✅  main @ 6cd204d
Phase 1 PC   🔄  supertux-fork-linux 重跑中（~7 min）
Phase 2 APK  ❌  run #6 失败（17 min）→ 已修 monorepo bootstrap 路径
改编 bundle  ✅  platformer-supertux-validate 已通过
App 集成     ⏳  SuperTuxClassicActivity 仍为占位
```

| 阶段 | 内容 | 状态 | 说明 |
|------|------|------|------|
| **0** | 脚手架、文档、NOTICE | ✅ 完成 | `engine/supertux-fork/` 元数据 |
| **1** | PC 编译 `supertux2` | ✅ **CI 绿** | [Linux run #28417068861](https://github.com/yishijame-pixel/androidapp/actions/runs/28417068861) |
| **2** | Android `libsupertux2.so` | 🔄 **CI 进行中** | 当前步骤：`Install vcpkg ports` · [Run #28417748230](https://github.com/yishijame-pixel/androidapp/actions/runs/28417748230) |
| **2b** | 改编 bundle 校验 | ✅ **已通过** | [Validate run #28417748232](https://github.com/yishijame-pixel/androidapp/actions/runs/28417748232) |
| **3** | Logo / 品牌 Mod | ⏳ 未开始 | |
| **4** | FunLife 选关 → 引擎 | ⏳ 占位 Activity | `SuperTuxClassicActivity.kt` |
| **5** | 可配置角色 sprite | ⏳ 未开始 | |
| **6–7** | 账号 / 排行榜 / 商城 | ⏳ 未开始 | 见 [产品路线图](./supertux-funlife-product-roadmap.md) |

---

## GitHub Actions 三条流水线

| Workflow | 作用 | 最新结果 | 链接 |
|----------|------|----------|------|
| **supertux-fork-linux** | PC 编 SuperTux | 🔄 in_progress | [Run #28417748222](https://github.com/yishijame-pixel/androidapp/actions/runs/28417748222) |
| **supertux-fork-android** | NDK + vcpkg + APK | 🔄 in_progress | [Run #28417748230](https://github.com/yishijame-pixel/androidapp/actions/runs/28417748230) |
| **platformer-supertux-validate** | 改编 bundle 107 关 | ✅ success | [Run #28417748232](https://github.com/yishijame-pixel/androidapp/actions/runs/28417748232) |

### 最新 commit

| 项 | 值 |
|----|-----|
| `main` HEAD | `6cd204d` — fix(ci): Android NDK via setup-android v2 and sdkmanager |
| upstream pin | `94996de`（见 `engine/supertux-fork/source_pin.json`） |

---

## Android CI 要多久？有进度条吗？

**GitHub 没有百分比进度条**，只有「步骤清单」：已完成打 ✅，进行中转圈 ⏳，失败打 ❌。

打开 [Android run #28417748230](https://github.com/yishijame-pixel/androidapp/actions/runs/28417748230) → 点 **build-apk** → 左侧步骤列表即「进度条」。

### 步骤与典型耗时（首次无 vcpkg 缓存）

| # | 步骤 | 耗时 | 状态 |
|---|------|------|------|
| 1–3 | checkout + bootstrap upstream | ~2 min | ✅ |
| 4–7 | JDK + Android SDK + NDK | ~3 min | ✅（本次已修过） |
| 8 | Cache vcpkg | ~10 s | ✅ |
| **9** | **Install vcpkg ports (15 个库)** | **15–25 min** | **⏳ 当前** |
| 10 | Bootstrap SDL Android | ~1 min | 待 |
| 11 | Gradle assembleDebug (CMake+native) | **10–20 min** | 待 |
| 12–13 | Upload artifacts | ~30 s | 待 |

**预计总时长：** 首次约 **30–45 分钟**；下次有 vcpkg 缓存约 **15–20 分钟**。

**当前估算：** 已在第 9 步，大约还需 **20–35 分钟**（取决于 GitHub runner 负载）。

---

## 双轨产品（当前定位）

| 模式 | 引擎 | 关卡 | 进度 |
|------|------|------|------|
| **经典 Fork（主线）** | SuperTux C++ | 原生 `.stl` + 脚本/Boss | Phase 1 ✅ · Phase 2 修 CI 中 |
| **改编 Kotlin（备用）** | FunLife | bundle v4 · 107 关 | 已进 APK assets · 非 1:1 |

---

## 本地你可以自己查

### 1. 看 GitHub CI（推荐）

浏览器打开：**https://github.com/yishijame-pixel/androidapp/actions**

左侧筛选：

- `supertux-fork-linux`
- `supertux-fork-android`
- `platformer-supertux-validate`

### 2. Push（需代理时）

本机 GitHub 需走代理 `127.0.0.1:7897`：

```powershell
$env:HTTPS_PROXY="http://127.0.0.1:7897"
$env:HTTP_PROXY="http://127.0.0.1:7897"
cd d:\soft
git push origin main
```

或：

```powershell
powershell -File backend/tools/push_supertux_fork_ci.ps1
```

### 3. 本地同步 upstream（不提交 318MB 树）

```powershell
$env:HTTPS_PROXY="http://127.0.0.1:7897"
powershell -File backend/tools/clone_supertux_upstream.ps1
powershell -File backend/tools/bootstrap_supertux_fork.ps1
```

### 4. 改编 bundle 重新导入

```powershell
python backend/tools/import_supertux_platformer.py
python backend/tools/validate_platformer_supertux.py
```

---

## 下一步（开发队列）

1. **Android CI** — setup-android@v2 + sdkmanager 显式装 NDK（已 push，待绿）
2. **重跑 validate** — workflow path 已修复
3. **Android CI 绿** — 下载 artifact：`supertux-fork-apk-arm64-debug`、`libsupertux2-arm64-v8a`
4. **Phase 4** — 真 SDL 壳 + FunLife 选关进引擎

---

## 相关文档

| 文档 | 用途 |
|------|------|
| [supertux-fork-strategy.md](./supertux-fork-strategy.md) | 企业战略 Phase 0–9 |
| [supertux-funlife-product-roadmap.md](./supertux-funlife-product-roadmap.md) | 角色/UI/商城/排行榜 |
| [supertux-fork-build-guide.md](./supertux-fork-build-guide.md) | 构建命令 |
| [platformer-supertux-integration-plan.md](./platformer-supertux-integration-plan.md) | 改编模式 bundle |

---

## 修订记录

| 日期 | 说明 |
|------|------|
| 2026-06-30 | 首版进度看板；Linux CI 绿；Android NDK 步骤失败 |
