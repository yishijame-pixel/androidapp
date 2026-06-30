# SuperTux Fork 构建指南（Phase 0–2）

> **关联：** [`supertux-fork-strategy.md`](./supertux-fork-strategy.md) · [`supertux-funlife-product-roadmap.md`](./supertux-funlife-product-roadmap.md) · `engine/supertux-fork/`

## 0. 同步 upstream（必做）

```powershell
powershell -File backend/tools/bootstrap_supertux_fork.ps1
```

将 `reference-assets/supertux` 镜像到 `engine/supertux-fork/`，保留 `patches/`、`NOTICE`、`source_pin.json`。

## 前置

| 工具 | 说明 |
|------|------|
| Git | fork remote 指向 FunLife 组织 |
| CMake | 与 upstream CI 对齐 |
| vcpkg | PC + Android triplets |
| Android NDK | 见 upstream `mk/android/README.md`（示例 r29） |

## Phase 0 — 仓库基线

- [x] `bootstrap_supertux_fork.ps1` 同步 upstream  
- [x] `source_pin.json` 记录 commit  
- [x] Linux CI：`.github/workflows/supertux-fork-linux.yml`

## Phase 1 — PC 冒烟

```bash
cd engine/supertux-fork/build
./supertux2 --datadir ../data levels/world1/welcome_antarctica.stl
```

## Phase 2 — Android（NDK + vcpkg → libsupertux2.so）

**CI：** `.github/workflows/supertux-fork-android.yml`（arm64-v8a debug APK + `.so` artifact）

### 本地前置

| 变量 | 示例 |
|------|------|
| `VCPKG_ROOT` | `C:/dev/vcpkg` |
| `ANDROID_NDK_HOME` | `…/ndk/29.0.14206865` |

`mk/android/local.properties`：

```properties
vcpkg_root=/path/to/vcpkg
ndk_home=/path/to/ndk/29.0.14206865
```

### 命令

```bash
bash backend/tools/clone_supertux_upstream.sh
powershell -File backend/tools/bootstrap_supertux_fork.ps1
cd engine/supertux-fork
./tools/bootstrap-android-project.sh 2.32.10
cd mk/android && ./gradlew assembleDebug -Pcpuarch=arm64-v8a
```

产物：`app/build/outputs/apk/debug/*.apk`，native 库 `libsupertux2.so`。

## FunLife App 集成（Phase 4）

- `SuperTuxClassicActivity` — SDL 壳（当前为占位，待 `libsupertux2.so` 就绪）
- Intent：`levelStl`, `playerSprite`, `saveSlot`
- 详见战略文档 §八

## 故障排查

| 现象 | 处理 |
|------|------|
| vcpkg Android triplet 失败 | 对照 upstream nightly workflow |
| data/ 资源缺失 | 确保 git LFS / 完整 data 子树 |
| APK 体积过大 | asset pack 分包 World |
