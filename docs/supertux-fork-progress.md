# SuperTux Fork 进度看板（ST-FORK）

> **最后更新：** 2026-06-30（自动/人工维护）  
> **仓库：** [yishijame-pixel/androidapp](https://github.com/yishijame-pixel/androidapp)  
> **一键看 CI：** [GitHub Actions 总览](https://github.com/yishijame-pixel/androidapp/actions)

---

## 总览（一眼看懂）

```
产品决策 ✅  Fork 经典模式 = 主线
Push GitHub ✅  main @ 1a06ea8
Phase 1 PC   ✅  supertux-fork-linux 已通过
Phase 2 APK  🔄  supertux-fork-android CI 修复已 push
改编 bundle  ⚠️  platformer-supertux-validate 待重跑
App 集成     ⏳  SuperTuxClassicActivity 仍为占位
```

| 阶段 | 内容 | 状态 | 说明 |
|------|------|------|------|
| **0** | 脚手架、文档、NOTICE | ✅ 完成 | `engine/supertux-fork/` 元数据 |
| **1** | PC 编译 `supertux2` | ✅ **CI 绿** | [Linux run #28417068861](https://github.com/yishijame-pixel/androidapp/actions/runs/28417068861) |
| **2** | Android `libsupertux2.so` | 🔄 **CI 修复中** | setup-android v2 + sdkmanager · [Android workflow](https://github.com/yishijame-pixel/androidapp/actions/workflows/supertux-fork-android.yml) |
| **2b** | 改编 bundle 校验 | ⚠️ 待重跑 | [Validate run](https://github.com/yishijame-pixel/androidapp/actions/runs/28417068883) · 旧 BOM 问题已修 |
| **3** | Logo / 品牌 Mod | ⏳ 未开始 | |
| **4** | FunLife 选关 → 引擎 | ⏳ 占位 Activity | `SuperTuxClassicActivity.kt` |
| **5** | 可配置角色 sprite | ⏳ 未开始 | |
| **6–7** | 账号 / 排行榜 / 商城 | ⏳ 未开始 | 见 [产品路线图](./supertux-funlife-product-roadmap.md) |

---

## GitHub Actions 三条流水线

| Workflow | 作用 | 最新结果 | 链接 |
|----------|------|----------|------|
| **supertux-fork-linux** | PC 编 SuperTux | ✅ success | [打开](https://github.com/yishijame-pixel/androidapp/actions/workflows/supertux-fork-linux.yml) |
| **supertux-fork-android** | NDK + vcpkg + APK | ❌ failure | [打开](https://github.com/yishijame-pixel/androidapp/actions/workflows/supertux-fork-android.yml) |
| **platformer-supertux-validate** | 改编 bundle 107 关 | ❌ failure（旧 run） | [打开](https://github.com/yishijame-pixel/androidapp/actions/workflows/platformer-supertux-validate.yml) |

### 最新 commit

| 项 | 值 |
|----|-----|
| `main` HEAD | `1a06ea8` — fix(ci): utf-8-sig for source_pin in all workflows |
| upstream pin | `94996de`（见 `engine/supertux-fork/source_pin.json`） |

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
