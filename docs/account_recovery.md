# 企业级账号恢复方案

清本机数据（或卸载重装）后，用户仍可用 **原用户名 + 密码 + 原设备** 从云端恢复登录身份与可同步资产。

## 架构

```
App 登录
  │
  ├─ 本地 SQLite 验密成功 → 正常登录
  │
  └─ 本地无账号
       │
       POST /account_recover { username, passwordProof, deviceId }
       │
       ├─ 云端 vip_users 验密 + 设备绑定校验
       ├─ 拉取 vip_coin_snapshots 钱包快照
       ├─ 签发 device_token
       └─ 审计写入 vip_recover_log
       │
       App 重建本地 User 行 + 恢复金币/积分 + 保存 token → 登录成功
```

## 安全设计

| 项 | 说明 |
|---|---|
| passwordProof | `SHA256("FunLifeAuth\|username\|password")`，不上传明文密码 |
| 防枚举 | 用户不存在 / 密码错误统一返回 `CREDENTIALS_INVALID` |
| 设备绑定 | 异设备返回 `DEVICE_CONFLICT`，需 VIP 迁移或在原设备恢复 |
| 限流 | 按 device / user / IP 分别限流 |
| 审计 | 每次请求写入 `vip_recover_log` |

## 恢复范围

| 可恢复 | 暂不可恢复（需 Phase 2 加密云备份） |
|---|---|
| 登录身份（本地 User） | 习惯、心情、目标、日记 |
| 昵称 | Pac-Maze 进度、书籍、宠物 |
| 金币 / 积分（最近快照） | 背包、成就、社交本地缓存 |
| device_token | 全部 Room 个人数据 |

## 部署

```powershell
cd backend
.\deploy-account-recover.ps1
```

部署后在 **CloudBase 控制台 → account_recover → HTTP 触发器** 确认路径为 `/account_recover`。

首次部署还需确保集合存在（含 `vip_recover_log`）：

```powershell
cd backend/tools
node init_collections.js
```

## 测试

### 一键（推荐）

```powershell
# 本地单元测试：后端 9 项 + Android 6 项
powershell -File scripts/run_account_recovery_all.ps1

# 含云端 E2E（需已部署）
powershell -File scripts/run_account_recovery_all.ps1 -IncludeE2E
```

### 分项

| 脚本 | 覆盖 |
|---|---|
| `backend/functions/account_recover/account_recover.test.js` | 核心逻辑 9 用例 |
| `app/.../AccountRecoveryLogicTest.kt` | passwordProof 3 用例 |
| `app/.../AccountRecoveryRepositoryTest.kt` | 本地重建 + 钱包恢复 3 用例 |
| `backend/tools/test_account_recover.js` | 云端 E2E 7 步 |

## 用户操作（linkg 等同设备清数据场景）

1. 安装含本功能的 App 版本
2. 确认后端已部署 `/account_recover`
3. 在同一设备上用 **原用户名 + 原密码** 登录
4. 成功后 Toast 提示「已从云端恢复账号」
