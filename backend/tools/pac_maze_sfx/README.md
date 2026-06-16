# 豆人迷宫音效包（云端 COS，不打进 APK）

与 `pet`、`login` 等资源包相同：本地打包 → 上传 CloudBase `yishi-assetss/v1/bundles/pac_maze_sfx.zip` → App 按需下载到 `resource_cache/pac_maze_sfx/`。

## 目录结构（zip 内）

```
pac_maze_sfx/
├── sfx_manifest.json      # 事件 id → 文件路径（PacMazeSfx 对接用）
├── README.md
├── LICENSE.txt
├── curated/               # 默认选用（已重命名，路径稳定）
├── variants/              # 同事件多版本随机播放
│   ├── victory/
│   ├── failure/
│   ├── projectile/
│   └── gameplay/
├── retro/                 # 原始复古 wav + jfxr
└── kenney_impact/         # Kenney Impact Sounds 完整 ogg 库
```

## 构建

```powershell
# 从 d:\download 整合素材到 dist/pac_maze_sfx（默认源路径）
powershell -File backend/tools/build_pac_maze_sfx.ps1

# 自定义下载目录
powershell -File backend/tools/build_pac_maze_sfx.ps1 -DownloadRoot "D:\download"
```

## 上传 COS

```powershell
# 需先 tcb login
powershell -File backend/tools/upload_pac_maze_sfx.ps1
```

上传路径：`yishi-assetss/v1/bundles/pac_maze_sfx.zip`  
解压目标：`resource_cache/pac_maze_sfx/`（与 manifest 中 `targetDir` 一致）

## 事件对照

| 事件 id | 默认文件 | 来源 |
|---------|----------|------|
| `checkpoint` | curated/checkpoint.wav | Pickup_coin 4 |
| `gate_phase` | curated/gate_phase.ogg | Kenney impactBell |
| `level_clear` | curated/level_clear.mp3 | Victory fanfare #1 |
| `game_over` | curated/game_over.mp3 | Cute failure #1 |
| `pellet` | curated/pellet.wav | Pickup_coin 2 |
| `power_pellet` | curated/power_pellet.wav | Pickup_coin 8 |
| `attack` | curated/attack.mp3 | Cute projectile #1 |
| `laser` | curated/laser.wav | Laser_shoot 4 |
| `hurt` | curated/hurt.wav | Hit_hurt 1 |
| `ghost_stun` | curated/ghost_stun.ogg | Kenney impactSoft |
| `bgm_gameplay` | curated/bgm_gameplay.mp3 | Cute arcade #1 |

`Powerup 1.jfxr` 为 jfxr 程序化格式，Android SoundPool 无法直接播放，保留在 `retro/procedural/` 供后续转换。

## App 接入（尚未打进 APK）

后续在 `ResourceStore` 增加 `pac_maze_sfx` bundle，`PacMazeSfx` 从缓存目录加载 `sfx_manifest.json` 即可；当前仍回退 `res/raw` 猜谜音效。
