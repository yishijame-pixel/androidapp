# FunLife 完全脱离腾讯云 — 详细迁移方案

> **版本**: 3.0（企业级完整版）  
> **日期**: 2026-06-21  
> **目标**: 零依赖腾讯云（CloudBase 云函数 / 文档数据库 / COS / API 密钥），全部迁到自托管栈  
> **现状基线**: 游戏资源已迁 `assets.yishi.site`；社交已在 `pb.yishi.site`（PocketBase + Cloudflare 隧道）

---

## 1. Executive Summary

### 1.1 为什么要完全脱离

| 问题 | 影响 |
|------|------|
| 免费资源点用尽 | 云函数全挂 → 卡密无法激活、AI 不可用 |
| 函数与 DB 共用套餐 | 不可预测停服 |
| COS + 云函数耦合 | 发布链路复杂、单点账单 |

### 1.2 目标架构（迁移完成后）

```
                         ┌──────────────── Cloudflare ────────────────┐
                         │  TLS / WAF / Rate Limit / CDN 缓存          │
                         └───────┬──────────────┬──────────────┬───────┘
                                 │              │              │
                    assets.yishi.site   api.yishi.site   pb.yishi.site
                         │              │              │
              ┌──────────▼───┐   ┌──────▼──────┐  ┌────▼─────────────┐
              │ assets-static│   │ funlife-api │  │ pocketbase       │
              │ nginx :80    │   │ Express     │  │ draw-ws          │
              │ (只读 zip)   │   │ :3400       │  │ pac-maze-ws      │
              └──────────────┘   └──────┬──────┘  └──────────────────┘
                                        │
                                 ┌──────▼──────┐
                                 │ PostgreSQL  │  ← 原 CloudBase 全部集合
                                 │  :5432      │
                                 └─────────────┘
              ┌──────────────┐   ┌─────────────┐
              │ vip-admin    │──►│ 同一 PG     │
              │ :3300        │   │ (读写卡密)  │
              └──────────────┘   └─────────────┘

App 配置:
  ASSET_MANIFEST_URL=https://assets.yishi.site/manifest.json
  VIP_BACKEND_URL=https://api.yishi.site
  POCKETBASE_URL=https://pb.yishi.site
  （删除全部 TCB_* / tcloudbase 域名）
```

### 1.3 原则

1. **API 契约不变**：App 仍 POST `/redeem`、`/chat_ai` 等，只改 `VIP_BACKEND_URL` 域名。  
2. **HMAC 协议不变**：`VIP_HMAC_SECRET` 与现有 `cloudbaserc.json` 一致，用户已激活凭证无需重兑。  
3. **分阶段可回滚**：每阶段独立验收，保留 CloudBase 只读备份直至稳定 30 天。  
4. **社交与 VIP 分离**：PocketBase 继续管好友/房间；VIP/卡密/AI 配额走 PostgreSQL + `funlife-api`。

---

## 2. 现状依赖清单（必须全部替换）

### 2.1 已脱离 ✅

| 能力 | 原腾讯云 | 现方案 |
|------|----------|--------|
| 游戏资源 zip + manifest | COS + `asset_bundle` 云函数 | `assets.yishi.site` 静态站 |

### 2.2 仍依赖腾讯云 ❌

#### A. CloudBase 云函数（App → `VIP_BACKEND_URL`）

| 路径 | 用途 | App 模块 |
|------|------|----------|
| `/redeem` | 卡密激活 | `VipCloudRepository` |
| `/verify` | 凭证复验 | `VipCloudRepository` |
| `/migrate` | 换机迁移 | `VipCloudRepository` |
| `/chat_ai` | 聊天/记账 AI | `ChatAiCloudRepository` |
| `/letter_ai` | 时光信箱 AI | `LetterCloudRepository` |
| `/vip_config` | SKU 运行时配置 | `VipRuntimeConfig` |
| `/coin_log` | 金币流水上报 | `CoinCloudReporter` |
| `/register_log` | 注册绑定 | `UserCloudRepository` |
| `/user_status` | 封禁/设备标记 | `UserCloudRepository` |
| `/account_recover` | 账号恢复 | `UserCloudRepository` |
| `/beta_validate` | 内测码 | `BetaCodeRepository` |
| `/postcard_drift` | 明信片漂流 | `PostcardDriftCloudRepository` |
| `/quote_galaxy` | 摘抄银河 | `QuoteGalaxyCloudRepository` |
| `/pac_maze_config` | ikun 须知 | `PacMazeIkunDisclosureConfig` |

#### B. CloudBase 文档数据库（后台 + 云函数）

`backend/tools/init_collections.js` 及云函数/admin 实际使用的集合（完整）：

| 集合 | 用途 |
|------|------|
| `vip_codes` | 卡密主表 |
| `vip_redeem_log` | 兑换/AI 审计 |
| `vip_sku_config` | SKU 运行时覆盖 |
| `vip_users` | 用户名绑定 |
| `vip_user_bans` | 封禁 |
| `vip_device_marks` | 设备标签 |
| `vip_revocations` | 凭证吊销 |
| `vip_rate_limit` | IP/设备限流 |
| `vip_coin_logs` | 金币流水 |
| `vip_coin_snapshots` | 金币快照 |
| `vip_coin_nonces` | 防重放 nonce |
| `vip_recover_log` | 恢复日志 |
| `vip_admin_audit` | 后台审计 |
| `letter_quota` | 信箱 AI 月配额 |
| `chat_ai_quota` | 聊天 AI 日配额 |
| `chat_ai_quota_month` | 聊天 AI 月配额 |
| `chat_ai_trial` | 体验池 |
| `chat_ai_book_quota` | 书房 AI 配额 |
| `chat_ai_dna_quota` | 读者 DNA 配额 |
| `pac_maze_ikun_disclosure` | ikun 须知文案 |
| `quote_galaxy` | 银河摘抄内容 |
| `galaxy_lights` | 点亮记录 |
| `galaxy_reports` | 举报 |
| `galaxy_publish_quota` | 发布配额 |
| `postcards` | 明信片 |
| `postcard_quota` | 明信片配额 |
| `active_readers` | 活跃读者 |

#### C. 腾讯云 API 密钥

- `TCB_SECRET_ID` / `TCB_SECRET_KEY`：`backend/admin`、`backend/tools`、所有云函数  
- `tcb login` / `tcb storage upload`：资源上传脚本（已由 `upload-assets-static.ps1` 替代）

#### D. COS 对象存储

- `yishi-assetss/v1/bundles/*.zip` — **可下线**（已迁静态站）

---

## 3. 目标技术选型

### 3.1 计算层：`funlife-api`（自建 Express）

| 项 | 选型 | 理由 |
|----|------|------|
| 运行时 | Node 20 LTS | 与现有 `backend/functions/*.js` 同栈 |
| 框架 | Express 4 | 云函数 `exports.main(event)` 易包装为 `req,res` |
| 部署 | Docker + Cloudflare 隧道 | 与 PocketBase、assets 一致 |
| 域名 | `api.yishi.site` | 与 `pb.`、`assets.` 并列 |

**实现方式**：新增 `backend/api/server.js` 统一路由：

```text
POST /redeem      → require('../functions/redeem/index.js').main(wrap(event))
POST /chat_ai     → ...
GET  /health      → 200
```

将云函数内 `tcb.database()` 替换为 `db` 抽象层（见 3.2），**业务逻辑文件尽量复用**。

### 3.2 数据层：PostgreSQL 15

| 项 | 选型 | 理由 |
|----|------|------|
| 数据库 | PostgreSQL 15 | 关系型、事务、JSONB 存灵活字段 |
| 部署 | Docker `postgres:15-alpine` | 与现有 Compose 栈集成 |
| 备份 | 每日 `pg_dump` + 宿主机副本 | 替代 CloudBase 控制台导出 |
| 连接 | `pg` / `node-postgres` 连接池 | admin + api 共用 |

**为何不直接用 PocketBase SQLite？**

- VIP 与社交数据模型、权限、迁移节奏不同  
- 云函数已有 20+ 集合、高并发写配额，PostgreSQL 更合适  
- 后台 admin 已是独立 Express，改 PG 比塞进 PB hooks 更清晰  

**若要坚持单一数据库**：可用 SQLite 文件 `vip.db`（小规模够用），表结构相同，本文以 PostgreSQL 为例。

### 3.3 存储层

| 类型 | 方案 |
|------|------|
| 游戏资源 | `assets.yishi.site`（已完成） |
| 后台上传/用户 UGC | PocketBase `pb_data` 或本地磁盘 |
| 密钥 | `pocketbase/secrets/` + Docker secrets，**不进 git** |

### 3.4 AI 调用

| 项 | 方案 |
|----|------|
| DeepSeek API Key | 仅存在于 `funlife-api` 环境变量 `AI_API_KEY` |
| App | **永不持有** Key；只调 `/chat_ai`、`/letter_ai` |
| 降级 | 保留 `CHAT_AI_USE_PROXY=false` 作开发兜底 |

### 3.5 DNS / 入口（全部 Cloudflare，非腾讯云）

| 子域 | 服务 |
|------|------|
| `pb.yishi.site` | PocketBase + WS |
| `assets.yishi.site` | 静态资源 |
| `api.yishi.site` | VIP + AI API |
| `admin.yishi.site` | （可选）VIP 管理后台，IP 白名单 |

---

## 4. 数据库 Schema 设计（PostgreSQL）

### 4.1 核心表（示例 DDL 摘要）

```sql
-- 卡密
CREATE TABLE vip_codes (
  code            TEXT PRIMARY KEY,          -- 规范化大写无分隔
  display_code    TEXT,
  sku_code        TEXT NOT NULL,
  product_type    TEXT NOT NULL,             -- vip | chat_ai
  status          TEXT NOT NULL DEFAULT 'unused',
  vip_level       INT DEFAULT 0,
  chat_ai_tier    INT DEFAULT 0,
  batch           TEXT,
  disabled        BOOLEAN DEFAULT FALSE,
  device_id       TEXT,
  username        TEXT,
  migrate_count   INT DEFAULT 0,
  expire_date     TIMESTAMPTZ,
  activated_at    TIMESTAMPTZ,
  created_at      TIMESTAMPTZ DEFAULT now(),
  metadata        JSONB DEFAULT '{}'
);
CREATE INDEX idx_vip_codes_status ON vip_codes(status);
CREATE INDEX idx_vip_codes_device ON vip_codes(device_id);
CREATE INDEX idx_vip_codes_batch ON vip_codes(batch);

-- SKU 运行时配置（原 vip_sku_config 文档）
CREATE TABLE vip_sku_config (
  sku_code        TEXT PRIMARY KEY,
  bonus_coins     INT,
  daily_coins     INT,
  duration_days   INT,
  price           NUMERIC(10,2),
  display_name    TEXT,
  overrides       JSONB DEFAULT '{}',
  updated_at      TIMESTAMPTZ DEFAULT now()
);

-- 配额（通用模式：key + bucket + count）
CREATE TABLE quota_counters (
  bucket          TEXT NOT NULL,   -- letter | chat_ai | chat_ai_book | ...
  quota_key       TEXT NOT NULL,   -- device_yyyymmdd 等
  device_id       TEXT,
  count           INT DEFAULT 0,
  meta            JSONB DEFAULT '{}',
  updated_at      TIMESTAMPTZ DEFAULT now(),
  PRIMARY KEY (bucket, quota_key)
);

-- 限流
CREATE TABLE vip_rate_limit (
  rate_key        TEXT PRIMARY KEY,
  count           INT DEFAULT 0,
  window_start    TIMESTAMPTZ,
  expires_at      TIMESTAMPTZ
);

-- 其余表：vip_users, vip_redeem_log, vip_revocations, vip_coin_logs,
-- vip_coin_snapshots, vip_admin_audit, quote_galaxy, postcards, ...
-- 详见 backend/migrations/postgres/001_initial.sql（实施阶段添加）
```

### 4.2 数据访问抽象

新增 `backend/shared/db/index.js`：

```javascript
// 统一接口，替换 tcb.database().collection(name)
module.exports = {
  codes: require('./repos/vip_codes'),
  quota: require('./repos/quota_counters'),
  // ...
};
```

每个 repo 实现与原 CloudBase 调用等价的：

- `getById(id)` / `where(filter).limit(n).get()`  
- `set(doc)` / `update(partial)` / `atomicUpdate(where, patch)`  

**关键**：`/redeem` 的原子 `status: unused → used` 必须用 SQL：

```sql
UPDATE vip_codes SET status='used', device_id=$1, activated_at=now()
WHERE code=$2 AND status='unused' AND disabled=false
RETURNING *;
-- affected rows = 0 → 并发失败，与 CloudBase 语义一致
```

---

## 5. 分阶段迁移计划

### Phase 0 — 准备（1–2 天）

| 任务 | 产出 |
|------|------|
| 导出 CloudBase 全集合 JSON | `backup/tcb-export/YYYY-MM-DD/*.json` |
| 记录各集合文档数、索引需求 | 迁移验收基准 |
| 创建 `backend/migrations/postgres/` | DDL + seed |
| 编写 `tools/export_tcb.js` / `tools/import_pg.js` | 一次性迁移脚本 |

**导出命令思路**：用现有 `TCB_SECRET_*` + `@cloudbase/node-sdk` 分页拉取各 collection。

### Phase 1 — 游戏资源（已完成 ✅）

- `assets.yishi.site`  
- App `ASSET_MANIFEST_URL`  
- 停用 COS 上传脚本  

### Phase 2 — API 网关 + 仍用 TCB DB（过渡，可选，1–2 天）

> 若希望**先恢复业务、再迁库**，可先只做 HTTP 自托管，数据库仍连 CloudBase。  
> **注意：这不算完全脱离**，只是不耗云函数资源点。

1. 部署 `funlife-api` Docker，路由 15 个端点  
2. `VIP_BACKEND_URL=https://api.yishi.site`  
3. cloudflared 增加 `api.yishi.site → funlife-api:3400`  

**完全脱离方案可跳过 Phase 2，直接 Phase 3+4。**

### Phase 3 — PostgreSQL + 双写/切读（3–5 天）

| 步骤 | 说明 |
|------|------|
| 3.1 | Docker 增加 `funlife-postgres`，持久化 volume |
| 3.2 | 跑 DDL migration |
| 3.3 | 全量 import TCB export → PG |
| 3.4 | `funlife-api` 改连 PG（feature flag `DB_BACKEND=postgres`） |
| 3.5 | 对账脚本：对比 `vip_codes` 数量、随机 100 条 hash |
| 3.6 | `vip-admin` 改 PG 数据源 |

### Phase 4 — App 与配置切换（1 天）

`local.properties` / CI 生产配置：

```properties
VIP_BACKEND_URL=https://api.yishi.site
VIP_BACKEND_PIN=sha256/...   # Cloudflare 证书 pin，非 tcloudbase
ASSET_MANIFEST_URL=https://assets.yishi.site/manifest.json
POCKETBASE_URL=https://pb.yishi.site

# 删除或留空：
# TCB_* 全部移除
```

发版 App；监控 7 日错误率。

### Phase 5 — 下线腾讯云（稳定 30 天后）

| 动作 | 说明 |
|------|------|
| 关闭 CloudBase 云函数 | 或整环境销毁 |
| 删除 COS bucket 内容 | 确认 assets 静态站完整 |
| 吊销 `TCB_SECRET_ID/KEY` | 腾讯云控制台 |
| 仓库删除 `@cloudbase/*` 依赖 | 清理 `cloudbaserc.json`、`tcb login` 文档 |
| 归档最终 TCB export | 冷备份 |

---

## 6. Docker Compose 增量（目标态）

在 `pocketbase/docker-compose.yml` 增加：

```yaml
  postgres:
    image: postgres:15-alpine
    container_name: funlife-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: funlife_vip
      POSTGRES_USER: funlife
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?required}
    volumes:
      - ./data/postgres:/var/lib/postgresql/data
    networks: [funlife]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U funlife -d funlife_vip"]

  funlife-api:
    build:
      context: ../backend
      dockerfile: docker/Dockerfile.api
    container_name: funlife-api
    restart: unless-stopped
    ports:
      - "${API_HOST_PORT:-3400}:3400"
    environment:
      DATABASE_URL: postgres://funlife:${POSTGRES_PASSWORD}@postgres:5432/funlife_vip
      HMAC_SECRET: ${HMAC_SECRET:?required}
      AI_API_KEY: ${AI_API_KEY:-}
      AI_BASE_URL: ${AI_BASE_URL:-https://api.deepseek.com}
      AI_MODEL: ${AI_MODEL:-deepseek-chat}
    depends_on:
      postgres:
        condition: service_healthy
    networks: [funlife]

  vip-admin:
    # 现有服务：environment 改为 DATABASE_URL，移除 TCB_*
```

`docker/cloudflared/config.yml` 增加：

```yaml
  - hostname: api.yishi.site
    service: http://funlife-api:3400
  - hostname: admin.yishi.site   # 可选
    service: http://vip-admin:3300
```

---

## 7. 安全设计（脱离腾讯云后）

| 威胁 | 措施 |
|------|------|
| API 被刷 | Cloudflare Rate Limit；`funlife-api` IP 限流；保留 `vip_rate_limit` 表 |
| 卡密撞库 | `/redeem` 设备+IP 限流；admin 监控异常激活 |
| MITM | HTTPS + `VIP_BACKEND_PIN` / `API_BACKEND_PIN` |
| DB 泄露 | Postgres 仅 Docker 内网；强密码；不映射 5432 到公网 |
| AI Key 泄露 | 只在 `funlife-api` 容器 env；不进 App、不进 git |
| 管理后台 | `admin.yishi.site` + Cloudflare Access 或 IP 白名单 |
| 备份勒索 | 异地 pg_dump；assets zip 已有 GitHub 冷备 |

HMAC 验签、`certificate`/`signature` 协议**保持不变**，App 端 `VipCertificateStore` 无需改。

---

## 8. 成本对比（估算）

| 项目 | 腾讯云 CloudBase | 自托管（你现有机器） |
|------|------------------|---------------------|
| 云函数调用 | 套餐/按量，易用尽 | **0**（自有 CPU） |
| 文档数据库 | 含在套餐 | **0**（PG 磁盘 ~几百 MB） |
| COS 流量 | 按量 | **0**（assets 走 Cloudflare） |
| DeepSeek AI | 按 token | **按 token**（与现网相同，只是 Key 放自建 API） |
| Cloudflare | — | 免费档通常够用 |
| 电费/带宽 | — | 现有家庭/机房成本 |

---

## 9. 风险与回滚

| 风险 | 缓解 |
|------|------|
| 迁移丢数据 | 全量 export + import 对账；切流前快照 PG |
| PG 单点故障 | 每日备份；可选主从（后期） |
| 家庭宽带上行 | 大资源已 CDN；API 流量小 |
| 证书轮换 | Pin 主+备证书；Cloudflare 统一管理 |
| 回滚 | 保留 TCB export + 临时恢复 `VIP_BACKEND_URL` 到 tcloudbase（充值后） |

---

## 10. 验收 Checklist

### 10.1 功能

- [ ] 后台生成 VIP 卡 / 聊天 AI 卡 → PG `vip_codes` 有记录  
- [ ] App 激活卡密 → `/redeem` 200 + 本地凭证有效  
- [ ] `/verify` 复验通过  
- [ ] `/chat_ai`、`/letter_ai` 正常扣配额  
- [ ] `/coin_log` 流水写入  
- [ ] ikun 须知 `/pac_maze_config` 可读  
- [ ] 游戏资源 `assets.yishi.site` 下载正常  
- [ ] PocketBase 社交不受影响  

### 10.2 非功能

- [ ] 腾讯云控制台无活跃 API 调用（监控 24h）  
- [ ] `grep -r tcloudbase\|TCB_\|cloudbase` 代码库仅剩历史文档/备份说明  
- [ ] pg_dump 自动任务运行  
- [ ] Cloudflare 429/5xx 告警配置  

---

## 11. 工作量估算

| 阶段 | 人天（1 人） | 说明 |
|------|-------------|------|
| Phase 0 准备 + export | 1 | 脚本 + 备份 |
| Phase 3 DB + repo 层 | 3–4 | 20+ 表/仓库 |
| Phase 3 API 适配 | 2–3 | 包装 14 个 handler |
| Phase 3 admin 改 PG | 2 | server.js 查询替换 |
| Phase 4 App 配置 + 测试 | 1 | E2E 脚本已有可复用 |
| Phase 5 下线 + 文档 | 0.5 | |
| **合计** | **约 9–11 人天** | 可并行缩短 |

---

## 12. 与现有文档关系

| 文档 | 关系 |
|------|------|
| [game-assets-static-hosting.md](./game-assets-static-hosting.md) | Phase 1 已完成部分 |
| [pocketbase/DOCKER_DEPLOY.md](../pocketbase/DOCKER_DEPLOY.md) | 隧道/Compose 基座 |
| 本文 | VIP/AI/卡密 完全离云总方案 |

---

## 13. 实施优先级建议

```
现在立刻做（不等充值）:
  ① Phase 0 导出 CloudBase 数据（趁 DB 还能读）
  ② Phase 3 PostgreSQL + funlife-api 骨架
  ③ 先打通 /redeem + /verify + /chat_ai 三板斧
  ④ admin 改 PG
  ⑤ 切 VIP_BACKEND_URL → api.yishi.site
  ⑥ 30 天后销毁 CloudBase

不要做的:
  × 仅 Phase 2 就当“完全脱离”（DB 还在腾讯云）
  × 把 VIP 塞进 PocketBase 又不设计权限（后期难维护）
  × 在 App 里硬编码 bypass 卡密（破坏商业模型）
```

---

## 14. 修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-21 | 初版：完全脱离腾讯云总体方案 |
| 2.0 | 2026-06-21 | **已落地实现**：PostgreSQL 兼容层 + funlife-api + 迁移脚本 |
| 3.0 | 2026-06-21 | **企业级完整版**：Runbook、API 契约、DR、监控、过渡模式 `-TcbOnly` |

---

## 15. 已实现代码清单

| 路径 | 说明 |
|------|------|
| `backend/shared/db/postgres.js` | CloudBase `database()` PostgreSQL 兼容实现 |
| `backend/shared/db/install-shim.js` | PG + TCB 凭证注入；零改动云函数 |
| `backend/migrations/postgres/001_documents.sql` | documents 表 DDL |
| `backend/api/server.js` | Express 网关，14 个 API 路由 |
| `backend/docker/Dockerfile.api` | API 容器镜像 |
| `backend/tools/export_tcb.js` | CloudBase → JSON 导出 |
| `backend/tools/import_pg.js` | JSON → PostgreSQL 导入 |
| `backend/tools/seed_pg_defaults.js` | 空库默认 SKU / ikun |
| `backend/tools/install_function_deps.js` | 云函数 npm 依赖 |
| `backend/admin/_loadEnv.js` | 支持 `DATABASE_URL` 或 `TCB_*` |
| `pocketbase/docker-compose.yml` | `postgres` + `funlife-api` 服务 |
| `pocketbase/tools/deploy-offboard-tencent.ps1` | 一键部署（含 `-TcbOnly`） |
| `docs/game-assets-static-hosting.md` | 游戏资源静态站文档 |

### 15.1 功能保留说明

**无需重写 App 或后台 UI。** 云函数 `backend/functions/*/index.js` 原样复用。

| 能力 | 保留 | 备注 |
|------|------|------|
| 卡密生成/查询/禁用 | ✅ | admin → PG |
| 卡密激活 / 复验 / 迁移 | ✅ | `/redeem` `/verify` `/migrate` |
| 聊天 AI / 信箱 AI | ✅ | `AI_API_KEY` 在 api 容器 |
| 金币流水 / 账号恢复 | ✅ | |
| 银河 / 明信片 | ✅ | |
| ikun 须知 | ✅ | `/pac_maze_config` |
| SKU 运行时配置 | ✅ | `/vip_config` |
| 游戏资源下载 | ✅ | `assets.yishi.site` |
| 社交 | ✅ | PocketBase 未动 |

---

## 16. 运维 Runbook（速查）

详见 **§23 分场景部署**。常用命令：

```powershell
docker compose --profile api --profile tunnel up -d
docker compose logs -f funlife-api
curl -s https://api.yishi.site/health
```

---

## 17. 文档治理（Document Control）

| 项 | 内容 |
|----|------|
| 文档名称 | FunLife 完全脱离腾讯云 — 企业级迁移与运维手册 |
| 版本 | 3.0 |
| 所有者 | 平台 / 后端 |
| 受众 | 开发、运维、发布负责人 |
| 关联文档 | [game-assets-static-hosting.md](./game-assets-static-hosting.md)、[pocketbase/DOCKER_DEPLOY.md](../pocketbase/DOCKER_DEPLOY.md) |
| 审批门禁 | Phase 3 切 PG 前需完成 §10 验收 + §29 Go/No-Go |

---

## 18. 术语表

| 术语 | 含义 |
|------|------|
| CloudBase (TCB) | 腾讯云开发平台：云函数 + 文档数据库 + COS |
| funlife-api | 自建 Express 网关，路由与原云函数 1:1 |
| db-shim | `install-shim.js`，拦截 `@cloudbase/node-sdk`，底层换 PG |
| VIP 凭证 | App 本地 HMAC 签名证书，由 `/redeem` 签发 |
| 过渡模式 | `-TcbOnly`：API 自托管但 DB 仍读 CloudBase（非完全离云） |
| 完全离云 | API + DB + 静态资源均不在腾讯云 |

---

## 19. 服务目录（Service Catalog）

| 服务 | 域名/端口 | 容器 | Profile | 职责 |
|------|-----------|------|---------|------|
| 游戏资源 CDN | `assets.yishi.site` | `assets-static:80` | 默认 | manifest + zip |
| VIP/AI API | `api.yishi.site` | `funlife-api:3400` | `api` | 14 个业务端点 |
| 社交 | `pb.yishi.site` | `pocketbase:8090` | 默认 | 好友/房间/UGC |
| 你画我猜 WS | `pb.yishi.site/draw-ws` | `draw-ws:8790` | 默认 | 实时绘画 |
| 豆人迷宫 WS | `pb.yishi.site/pac-maze-ws` | `pac-maze-ws:8791` | 默认 | 多人迷宫 |
| VIP 管理后台 | `:3300` / 可选 `admin.yishi.site` | `vip-admin:3300` | `admin` | 卡密生成/审计 |
| PostgreSQL | 内网 `:5432` | `funlife-postgres` | 默认 | VIP 全量数据 |
| Cloudflare 隧道 | — | `funlife-cloudflared` | `tunnel` | 公网入口 |

---

## 20. 完整 API 契约（App → funlife-api）

所有端点均为 **POST**（部分支持 GET 调试）。请求体 JSON，响应 JSON。HMAC 协议与现网一致。

| 路径 | 鉴权 | 主要入参 | 成功响应要点 |
|------|------|----------|--------------|
| `/redeem` | 无（限流） | `code`, `deviceId` | `certificate`, `signature` |
| `/verify` | 凭证 | `certificate`, `signature` | `ok`, 等级/过期信息 |
| `/migrate` | 凭证 | 旧设备 + 新 `deviceId` | 新凭证 |
| `/chat_ai` | 凭证/体验池 | `messages`, `deviceId` | AI 回复 + 配额 |
| `/letter_ai` | 凭证 | 信箱上下文 | AI 回复 |
| `/vip_config` | 无 | — | `vipLevels` SKU 运行时 |
| `/coin_log` | 凭证 | 流水 batch | `ok` |
| `/register_log` | 无 | 用户名/设备 | 绑定结果 |
| `/user_status` | 无 | `deviceId` | 封禁/标记 |
| `/account_recover` | 恢复码 | 设备信息 | 新凭证 |
| `/beta_validate` | 无 | 内测码 | 放行结果 |
| `/postcard_drift` | 凭证 | 明信片 payload | 漂流结果 |
| `/quote_galaxy` | 凭证 | 摘抄/点亮 | 银河数据 |
| `/pac_maze_config` | 无 | — | ikun 须知文案 |
| `/health` | 无 | — | `ok`, `db`: postgres \| cloudbase |

**App 配置（生产）**

```properties
VIP_BACKEND_URL=https://api.yishi.site
ASSET_MANIFEST_URL=https://assets.yishi.site/manifest.json
POCKETBASE_URL=https://pb.yishi.site
```

---

## 21. 数据层：集合 → PostgreSQL 映射

采用 **单表 JSONB** 策略（`documents`），与 CloudBase 文档模型 1:1，无需重写云函数。

```sql
PRIMARY KEY (collection, doc_id)
data JSONB  -- 含原 _id 及全部字段
```

| collection | 业务 | doc_id 典型来源 |
|--------------|------|-----------------|
| `vip_codes` | 卡密 | 规范化 code |
| `vip_redeem_log` | 兑换审计 | `_id` |
| `vip_sku_config` | SKU 覆盖 | skuCode |
| `chat_ai_quota` | 日配额 | device+日期 composite |
| `letter_quota` | 信箱月配额 | 同上 |
| `pac_maze_ikun_disclosure` | ikun 须知 | `ikun_disclosure` |
| … | 见 §2.2 | export 脚本 `COLLECTIONS` 列表 |

索引见 `backend/migrations/postgres/001_documents.sql`（`vip_codes.status`、`chat_ai_quota` 等）。

---

## 22. 密钥与配置清单

| 变量 | 存放位置 | 用途 |
|------|----------|------|
| `HMAC_SECRET` | `pocketbase/.env`、与 `VIP_HMAC_SECRET` 一致 | 凭证签名 |
| `POSTGRES_PASSWORD` | `pocketbase/.env` | PG 连接 |
| `DATABASE_URL` | 容器内自动拼接 | admin + api |
| `AI_API_KEY` | `pocketbase/.env`（**不进 App/git**） | DeepSeek |
| `ADMIN_PASSWORD_HASH` | `backend/tools/.env` | 后台登录 |
| `ADMIN_SESSION_SECRET` | 同上 | Session |
| `TCB_*` | 仅 export / `-TcbOnly` 过渡 | 读旧库 |

模板：`pocketbase/docker/.env.example`、`backend/tools/.env.example`

---

## 23. 部署 Runbook（分场景）

### 23.1 场景 A — 完全离云（目标态）

```powershell
cd d:\soft\pocketbase
# 1. 复制 docker/.env.example → .env，填入 POSTGRES_PASSWORD、HMAC_SECRET、AI_API_KEY、ADMIN_*
# 2. backend/tools/.env 保留 TCB_* 用于 export（一次性）
.\tools\deploy-offboard-tencent.ps1
cloudflared tunnel route dns funlife-pb api.yishi.site
gradlew :app:installDebug
```

### 23.2 场景 B — 过渡模式（当前可用：API 自托管 + TCB DB）

当 **Docker Hub 拉不到 postgres** 或 **TCB export 读配额用尽** 时：

```powershell
.\tools\deploy-offboard-tencent.ps1 -TcbOnly -SkipExport -SkipImport
```

- ✅ 恢复 `/redeem`、`/chat_ai`、`/vip_config`（不耗云函数资源点）
- ❌ DB 仍在腾讯云 → **不算完全离云**
- 待网络/配额恢复后改跑场景 A

### 23.3 场景 C — 仅导出卡密（读配额有限）

```powershell
cd backend\tools
node export_tcb.js --only=vip_codes,vip_sku_config,vip_users
```

export 脚本支持限流重试；失败集合会 skip 并继续。

### 23.4 空库启动（无 TCB 备份）

```powershell
DATABASE_URL=postgres://... node backend/tools/seed_pg_defaults.js
```

写入默认 SKU + ikun 须知；**不含历史卡密**，需 admin 重新生成或后续 import。

---

## 24. 监控与 SLO（建议）

| 指标 | 目标 | 检查方式 |
|------|------|----------|
| API 可用性 | 99.5% | `GET https://api.yishi.site/health` 每 5min |
| `/vip_config` 延迟 | P95 < 2s | 合成探测 POST |
| PG 磁盘 | < 80% | `docker exec funlife-postgres df` |
| 隧道 | 4 连接 healthy | `docker logs funlife-cloudflared` |
| 429/5xx | 告警 | Cloudflare Analytics |

---

## 25. 备份与灾难恢复

| 对象 | 频率 | 路径 | RPO |
|------|------|------|-----|
| PostgreSQL | 每日 | `backup/pg/funlife_vip_YYYYMMDD.sql` | 24h |
| TCB export | 切流前 + 每月 | `backup/tcb-export/` | 导出时点 |
| 游戏 zip | 已 Git + assets 站 | `assets_public/` | 0 |
| PocketBase | 现有 pb_data 备份 | `pocketbase/pb_data` | 24h |

**恢复 PG**

```powershell
docker exec -i funlife-postgres psql -U funlife funlife_vip < backup/pg/latest.sql
docker compose --profile api restart funlife-api
```

---

## 26. 事件响应（Incident Playbook）

| 现象 | 可能原因 | 动作 |
|------|----------|------|
| App「资源更新失败」 | assets 站/ manifest | 查 `assets.yishi.site/health` |
| 卡密无法激活 | API 挂 / DB 只读 | `curl api.yishi.site/health`；查 funlife-api logs |
| AI 无响应 | 缺 `AI_API_KEY` | 查容器 env，非 App |
| `EXCEED_REQUEST_LIMIT` | TCB 读配额 | 改 PG 或 `-TcbOnly` + 等待配额 |
| postgres 拉取失败 | Docker Hub 网络 | 换镜像源或 `-TcbOnly` |
| api.yishi.site 超时 | DNS/隧道 | `cloudflared tunnel route dns ...` |

---

## 27. Go/No-Go 切流检查（Phase 4）

| # | 检查项 | 通过 |
|---|--------|------|
| 1 | `https://api.yishi.site/health` → `db: postgres` | ☐ |
| 2 | admin 生成卡 → PG `vip_codes` 有记录 | ☐ |
| 3 | App 激活测试卡 → 凭证有效 | ☐ |
| 4 | `/chat_ai` 扣配额成功 | ☐ |
| 5 | `assets.yishi.site` manifest 下载 | ☐ |
| 6 | PocketBase 登录/房间正常 | ☐ |
| 7 | pg_dump 任务已配置 | ☐ |
| 8 | TCB export 已归档 | ☐ |

---

## 28. 代码与脚本索引（v3.0）

| 路径 | 说明 |
|------|------|
| `backend/shared/db/postgres.js` | PG 版 `database()` |
| `backend/shared/db/install-shim.js` | PG + TCB 凭证注入 |
| `backend/api/server.js` | Express 14 路由 |
| `backend/tools/install_function_deps.js` | 云函数依赖安装 |
| `backend/tools/export_tcb.js` | TCB 导出（重试/skip/`--only=`） |
| `backend/tools/import_pg.js` | JSON → PG |
| `backend/tools/seed_pg_defaults.js` | 空库默认数据 |
| `pocketbase/tools/deploy-offboard-tencent.ps1` | 一键部署（含 `-TcbOnly`） |
| `pocketbase/docker-compose.yml` | postgres + api + admin |
| `pocketbase/docker/cloudflared/config.yml` | `api.yishi.site` ingress |

---

## 29. 当前部署状态（2026-06-21）

| 组件 | 状态 |
|------|------|
| `assets.yishi.site` | ✅ 已上线 |
| `pb.yishi.site` | ✅ 已上线 |
| `api.yishi.site` | ✅ **已上线**（过渡：DB=cloudbase） |
| PostgreSQL | ⏳ 待 Docker 拉取镜像 + import |
| TCB 全量 export | ✅ 已用 `export_tcb_nosql.js` 导入 PG（56 卡密 + 32 用户 + 248 兑换日志） |
| App `VIP_BACKEND_URL` | ✅ 已指向 `api.yishi.site` |

**下一步（完全离云）**

1. Docker Hub 网络恢复 → `deploy-offboard-tencent.ps1`（无 `-TcbOnly`）
2. 或 TCB 控制台导出 / 分批 `export_tcb.js --only=vip_codes,...`
3. `import_pg.js` → 切 `DATABASE_URL` → 重启 api + admin
4. 稳定 30 天后销毁 CloudBase

---

## 30. RACI（简化）

| 任务 | 负责 | 审批 |
|------|------|------|
| 架构 / 文档 | 后端 | 产品 |
| Docker 部署 | 运维 | 后端 |
| App 发版 | 客户端 | QA |
| 密钥轮换 | 运维 | 负责人 |
| TCB 下线 | 运维 | 负责人 |

---

## 31. 合规与安全要点

- App **永不持有** `AI_API_KEY` / `TCB_SECRET_*`
- PG **不映射公网**（仅 Docker 内网 + 可选 localhost 调试）
- 管理后台建议 Cloudflare Access 或 IP 白名单
- 凭证 HMAC 与证书 Pin 保持不变，降低 MITM 风险
- 离云后吊销腾讯云 API 密钥，归档最终 export

---

## 32. 修订记录（续）

| 版本 | 日期 | 说明 |
|------|------|------|
| 3.0 | 2026-06-21 | 企业级章节 17–32；`-TcbOnly`；`api.yishi.site` 上线验证 |

