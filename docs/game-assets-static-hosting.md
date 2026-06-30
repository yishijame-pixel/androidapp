# FunLife 游戏静态资源托管 — 企业级设计文档

> **版本**: 1.0  
> **日期**: 2026-06-20  
> **适用范围**: 吃豆人皮肤/音效、横版冒险角色包、登录/商店装饰等 **非敏感** 游戏资源  
> **部署环境**: Windows + Docker Desktop · Cloudflare Named Tunnel · `assets.yishi.site`

---

## 1. 文档目的与范围

### 1.1 背景

原方案将游戏资源 zip 存放在 **腾讯云 CloudBase COS**，通过 **`asset_bundle` 云函数** 签发 manifest 与临时下载链接。CloudBase 免费套餐资源点用尽后，云函数不可用，App 无法获取 manifest，出现「游戏资源更新失败」。

本方案将 **游戏资源分发** 从 CloudBase **解耦**，迁移至自托管静态站点，与 **PocketBase 社交层**（`pb.yishi.site`）并列，互不影响。

### 1.2 范围内

| 包含 | 不包含 |
|------|--------|
| zip 资源包托管与 manifest 分发 | VIP 账号、支付、AI 配额（仍走 CloudBase） |
| App 端下载、校验、缓存 | PocketBase 用户数据、对战状态 |
| 隧道、Nginx、Cloudflare 边缘防护 | 应用内用户生成内容（UGC） |

### 1.3 设计原则

1. **最小权限**：静态站点只读；不暴露管理接口。  
2. **纵深防御**：Cloudflare → 隧道 → Nginx 限速 → App SHA-256 → 解压 Zip Slip 防护。  
3. **可观测**：健康检查、发布校验脚本、回滚流程。  
4. **与社交隔离**：资源域名与 PocketBase API 分离，单点故障不扩散。

---

## 2. 目标架构

```
┌─────────────┐     HTTPS GET      ┌──────────────────┐
│ Android App │ ─────────────────► │ Cloudflare Edge  │
└─────────────┘   manifest.json    │ (WAF / CDN / TLS)│
       │           bundles/*.zip    └────────┬─────────┘
       │                                     │ Tunnel (出站)
       │                                     ▼
       │                          ┌──────────────────────┐
       │                          │ funlife-cloudflared    │
       │                          └────────┬─────────────┘
       │                                   │ assets.yishi.site
       │                                   ▼
       │                          ┌──────────────────────┐
       │                          │ funlife-assets-static │
       │                          │ nginx:alpine :80      │
       │                          │ volume: assets_public │
       │                          └──────────────────────┘
       │
       └── 本地 resource_cache/（校验通过后持久化）

并行独立：
  pb.yishi.site → pocketbase / draw-ws / pac-maze-ws（不受资源站影响）
  VIP_BACKEND_URL → CloudBase 云函数（账号/AI，与资源 zip 无关）
```

### 2.1 域名与职责

| 域名 | 职责 | 数据分类 |
|------|------|----------|
| `assets.yishi.site` | 游戏资源 manifest + zip | **公开**（可公开下载） |
| `pb.yishi.site` | 社交 API / WebSocket | **用户数据**（鉴权） |
| `*.app.tcloudbase.com` | VIP / AI 代理 | **敏感**（HMAC + Pin） |

### 2.2 目录结构（宿主机）

```
pocketbase/assets_public/
├── manifest.json          # 版本清单 + 直链 + SHA-256
└── bundles/
    ├── pac_maze_skins.zip
    ├── pac_maze_sfx.zip
    ├── platformer_characters.zip
    └── …
```

---

## 3. 威胁模型（STRIDE）

| 威胁 | 说明 | 缓解措施 |
|------|------|----------|
| **S** 欺骗 | 伪造 manifest 指向恶意 zip | HTTPS + 可选 Cert Pin；manifest/bundle SHA-256 |
| **T** 篡改 | MITM 替换 zip 内容 | TLS 1.2+；Pin；下载后 SHA-256 校验 |
| **R** 抵赖 | 发布方否认发过某版本 | manifest `version`/`updatedAt` + Git 发布记录 |
| **I** 信息泄露 | zip 被爬取、盗链 | 公开资源可接受；Cloudflare 限速；监控异常流量 |
| **D** 拒绝服务 | 大流量打满带宽/隧道 | CF Rate Limit；Nginx `limit_req`；仅 GET |
| **E** 权限提升 | 通过资源站入侵 PocketBase | **网络隔离**（独立容器/域名）；隧道分路由 |

---

## 4. 恶意攻击与防护

### 4.1 DDoS / 带宽耗尽

**风险**：攻击者大量请求 `bundles/*.zip`（单包可达数十 MB），耗尽 Cloudflare 隧道或家庭/机房上行带宽。

**防护**：

| 层级 | 措施 |
|------|------|
| Cloudflare | 开启 **Under Attack Mode**（应急）；**Rate Limiting**（如每 IP 10 req/min 对 `/bundles/`）；**缓存** manifest（5min）与 zip（immutable） |
| Nginx | `limit_req_zone` 30r/s；`limit_conn` 每 IP 4 连接；仅允许 GET/HEAD |
| 运维 | 监控隧道出口流量；异常峰值告警；必要时临时关闭 `assets` 子域路由 |

### 4.2 盗链与爬虫

**风险**：第三方 App/网站直接引用 zip URL，增加带宽成本。

**防护**：

- 资源为 **公开游戏素材**，盗链通常 **可接受**（非付费内容）。  
- 若需限制：Cloudflare **Hotlink Protection** 或检查 `User-Agent`（注意 **OkHttp 默认 UA 与浏览器不同**，勿误拦 App）。  
- **不推荐** 对 App 下载路径做 Referer 校验（Android 通常不带 Referer）。

### 4.3 Manifest 篡改（MITM）

**风险**：恶意 Wi‑Fi 返回假 manifest，指向恶意 zip。

**防护**：

1. **强制 HTTPS**（Cloudflare 全站 SSL）。  
2. App 配置 **`ASSET_MANIFEST_PIN`**（Cloudflare 源站证书 SHA-256）。  
3. manifest 中每个 bundle 的 **`sha256`**；App 下载后校验，不匹配则拒绝解压。  
4. 解压后 **marker 文件校验**（现有 `PAC_MAZE_SKINS_MARKERS` 等逻辑）。

### 4.4 恶意 Zip（Zip Slip / Zip Bomb）

**风险**：恶意 zip 含 `../` 路径或极高压缩比，占满磁盘/CPU。

**防护**（App 端已实现/增强）：

| 控制 | 限制 |
|------|------|
| Zip Slip | 解压路径必须在 staging 目录 canonical 子树内 |
| Zip Bomb | 单包解压总量上限 **512 MB**；条目数上限 **20,000** |
| 格式 | 仅接受 `.zip`；解压后跑 `isBundleReady` 业务校验 |

### 4.5 供应链篡改

**风险**：发布机被入侵，上传恶意 zip。

**防护**：

- 发布脚本 **`generate-assets-manifest.ps1`** 自动计算 SHA-256 写入 manifest。  
- manifest 与 zip **同机原子更新**（先上传 zip，再更新 manifest）。  
- 仓库内 **`manifest.json.example`** 作结构参考；**生产 manifest 不提交 zip**。  
- 可选：CI 对 zip 做病毒扫描 / 文件类型白名单（png/ogg/json/txt）。

### 4.6 隧道凭证泄露

**风险**：`cloudflared/*.json` 泄露，攻击者接入内网。

**防护**：

- 凭证 **已 gitignore**；权限 `600`。  
- 隧道 **ingress 白名单**：仅 `assets.yishi.site`、`pb.yishi.site` 等明确 hostname。  
- **勿** 将隧道 catch-all 指到管理面板。  
- 泄露后：**立即 rotate tunnel**（`cloudflared tunnel delete` + 重建）。

### 4.7 对 PocketBase 的横向移动

**风险**：通过 assets 容器突破到 pocketbase 网络。

**防护**：

- assets 容器 **只读挂载** volume；**无 shell 工具**（alpine nginx 最小镜像）。  
- Docker network `funlife-net` 内 assets **不暴露** 8090；cloudflared 按 hostname 分流。  
- PocketBase 管理后台 **不通过隧道公开**（或 IP 白名单）。

---

## 5. 数据安全与分类

### 5.1 数据分类

| 级别 | 内容 | 存储位置 | 加密 |
|------|------|----------|------|
| L0 公开 | 游戏皮肤 png、音效 ogg | `assets_public/` | 传输 TLS |
| L1 内部 | manifest version、SHA-256 | 同上 | 传输 TLS |
| L2 用户 | 好友、聊天记录、对战 | PocketBase SQLite | 传输 TLS + PB 规则 |
| L3 敏感 | VIP HMAC、AI Key、FCM | CloudBase / secrets/ | 不进 git；App Pin |

**游戏资源 zip 不含用户隐私**，泄露影响为 **知识产权/带宽**，非 GDPR 级个人数据。

### 5.2 密钥与配置

| 配置项 | 位置 | 说明 |
|--------|------|------|
| `ASSET_MANIFEST_URL` | `local.properties` → BuildConfig | 静态 manifest HTTPS 地址 |
| `ASSET_MANIFEST_PIN` | 同上 | Cloudflare 证书 pin（可选） |
| `VIP_BACKEND_URL` | 同上 | 与资源 manifest **分离** |
| cloudflared credentials | `docker/cloudflared/*.json` | 不进 git |

### 5.3 客户端缓存

- 路径：`filesDir/resource_cache/`  
- 内容：解压后的 png/ogg/json  
- **不含** 账号 token（token 在 EncryptedSharedPreferences / PB SDK）  
- 卸载 App 即清除；无跨用户共享风险（单用户设备）

---

## 6. 可用性与灾备

### 6.1 SLA 目标（建议）

| 指标 | 目标 |
|------|------|
| manifest 可用性 | 99.5%（依赖 Cloudflare + 本机 Docker） |
| 单 bundle 下载成功率 | 99%（含弱网重试） |
| RTO（资源站故障） | < 4h（回滚 manifest 或切 CloudBase 备用） |
| RPO（资源版本） | 0（manifest + zip 在 git release / 本地备份） |

### 6.2 降级策略

```
1. 静态 manifest 失败 → 回退 CloudBase asset_bundle（VIP_BACKEND_URL 仍可用时）
2. manifest 失败 + 本地缓存有效 → 离线继续玩（ResourceStore 已有逻辑）
3. 全部失败 → 首页横幅提示；核心玩法依赖已缓存 bundle
```

### 6.3 备份

- **每周**：`assets_public/bundles/` 复制到冷存储（外置盘 / GitHub Release）。  
- **每次发布**：保留上一版 zip + manifest（`manifest.json.bak.v{N}`）。  
- PocketBase `pb_data/` **独立备份**（与资源无关，但必须做）。

---

## 7. 发布与变更管理

### 7.1 发布流程

```text
1. 本地 build（如 build_pac_maze_skins.ps1）
2. 复制 zip → pocketbase/assets_public/bundles/
3. .\pocketbase\tools\generate-assets-manifest.ps1
4. 校验：manifest 中 sha256 与文件一致
5. docker compose restart assets-static（通常不需要，volume 热更新）
6. curl https://assets.yishi.site/manifest.json
7. 提升 App 内 PAC_MAZE_SKINS_BUNDLE_VERSION（若 bundle 内容要求）
8. 发版 App 或依赖 manifest version 触发用户更新
```

### 7.2 回滚

1. 恢复旧 zip 与旧 `manifest.json`（降低 `version`）。  
2. App 比对 `bundle_content_*` pref 与 manifest version，自动重新下载。  
3. 若 App 已强制更高 bundle_version，需 **同步发 App 热修** 或保持 manifest 中 zip 向后兼容。

### 7.3 版本语义

| 字段 | 含义 |
|------|------|
| `manifest.version` | 整包清单版本（整数递增） |
| `pac_maze_skins/bundle_version.txt` | 皮肤包内容版本（App 硬编码最低要求） |
| `bundles[].sha256` | 该 zip 内容指纹 |

---

## 8. 监控与告警

### 8.1 健康检查

| 检查项 | 方法 | 频率 |
|--------|------|------|
| 容器存活 | Docker healthcheck `wget manifest.json` | 30s |
| 公网可达 | `curl -sf https://assets.yishi.site/manifest.json` | 5min（外部 cron） |
| zip 完整性 | 发布脚本 SHA-256 | 每次发布 |
| 隧道 | `cloudflared` 容器 Up | Docker |

### 8.2 建议告警

- manifest HTTP ≠ 200 连续 3 次  
- 隧道容器 Restart 次数 > 3/小时  
- Cloudflare 带宽 > 日阈值 80%  
- App 端 `ResourceStore` VALIDATION_FAILED 率上升（需客户端埋点）

### 8.3 日志

- Nginx access log：IP、URL、状态码、字节数（默认 stdout，Docker logs 收集）  
- **不记录** manifest 请求体；**不** 在资源站打 App 用户 ID

---

## 9. 合规与隐私

- 游戏资源为 **预置美术/音效**，无个人数据。  
- 下载行为 **不绑定** 用户账号（纯 HTTPS GET）。  
- 若未来做 **个性化资源推荐**，需另建 API 并更新隐私政策。  
- 中国区 ICP：若 `assets.yishi.site` 面向公众，确认域名备案要求（与主域 `yishi.site` 策略一致）。

---

## 10. 运维手册（Quick Reference）

### 10.1 首次部署

```powershell
cd d:\soft\pocketbase
mkdir assets_public\bundles -Force
# 放入 zip 后：
.\tools\generate-assets-manifest.ps1
cloudflared tunnel route dns funlife-pb assets.yishi.site
docker compose up -d assets-static
docker compose --profile tunnel restart cloudflared
```

### 10.2 日常更新资源

```powershell
.\tools\upload-assets-static.ps1 -Bundle pac_maze_skins
curl -s https://assets.yishi.site/manifest.json | jq .
```

### 10.3 App 配置（local.properties）

```properties
ASSET_MANIFEST_URL=https://assets.yishi.site/manifest.json
# 可选：Cloudflare 源证书 pin（与 VIP_BACKEND_PIN 格式相同）
ASSET_MANIFEST_PIN=sha256/xxxxxxxx=
```

### 10.4 故障排查

| 现象 | 排查 |
|------|------|
| manifest 502 | `docker ps` 看 assets-static / cloudflared |
| zip 404 | 文件名是否与 manifest `url` 一致 |
| App 校验失败 | 对比 manifest `sha256` 与本地文件 |
| 下载慢 | Cloudflare 缓存是否命中；考虑启用 CF CDN 缓存 zip |
| PocketBase 正常但资源失败 | **预期**：两系统独立，查 assets 栈 |

---

## 11. 相关文件索引

| 文件 | 说明 |
|------|------|
| `pocketbase/docker-compose.yml` | `assets-static` 服务 |
| `pocketbase/docker/nginx/assets-nginx.conf` | Nginx 安全基线 |
| `pocketbase/docker/cloudflared/config.example.yml` | 隧道 ingress 模板 |
| `pocketbase/assets_public/manifest.json.example` | manifest 结构示例 |
| `pocketbase/tools/generate-assets-manifest.ps1` | 生成 manifest + SHA-256 |
| `pocketbase/tools/upload-assets-static.ps1` | 发布 zip 到 assets_public |
| `app/.../ResourceStore.kt` | 静态 manifest 优先 + 校验 |
| `app/.../SecureHttp.kt` | 双域名 Cert Pin |

---

## 12. 修订记录

| 版本 | 日期 | 作者 | 说明 |
|------|------|------|------|
| 1.0 | 2026-06-20 | FunLife | 初版：自托管静态资源，脱离 CloudBase 云函数 |
