# FunLife VIP 卡密后端

腾讯云 CloudBase 上的极简卡密兑换系统，配套 Android 客户端。

## 架构

```
你的 App                   CloudBase 云函数              vip_codes 集合
  │  POST /redeem           │                              │
  │  { code, deviceId } ───→│                              │
  │                         │  查码 → 原子标记 used ──────→│
  │                         │  签发 HMAC 凭证              │
  │  { cert, signature } ←──│                              │
  │                                                        │
  │  本地存凭证 + 验签 → 解锁 VIP                          │
```

## 目录

```
backend/
├── cloudbaserc.json          ← 云函数部署配置（修改 envId）
├── shared/
│   └── sku.js                ← 产品 SKU 定义（改价改档在这）
├── functions/
│   ├── redeem/               ← 兑换接口
│   ├── migrate/              ← 设备迁移接口
│   └── verify/               ← 凭证复验接口（防破解）
└── tools/                    ← 本地管理工具
    ├── generate_codes.js     ← 批量生成卡密
    ├── admin_query.js        ← 查询销售情况
    └── admin_disable.js      ← 禁用/启用卡密
```

## 部署步骤

### Step 1. 生成 HMAC 密钥

这是签发凭证的密钥，**不能泄露**。在 PowerShell 运行：

```powershell
node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"
```

把输出的长串记下来（约 96 个字符）。

### Step 2. 修改 cloudbaserc.json

打开 `backend/cloudbaserc.json`，把 3 个函数中的 `HMAC_SECRET` 字段值替换为上一步生成的密钥。

> ⚠️ 3 个函数必须用**同一个密钥**，否则签出来的凭证别的函数验不过。

### Step 3. 部署云函数

```powershell
cd backend
tcb fn deploy redeem
tcb fn deploy migrate
tcb fn deploy verify
```

每个函数大约 30-60 秒部署完成。部署完后到 CloudBase 控制台 → 云函数 → 看到这 3 个函数即成功。

### Step 4. 开启 HTTP 访问（可选，方便客户端直调）

CloudBase 控制台 → 云函数 → 点函数名 → 「触发器」→ 「新建 HTTP 触发器」
- 路径：`/redeem` `/migrate` `/verify`
- 这样 App 可以直接 HTTPS 访问，无需 SDK

### Step 5. 配置工具脚本

```powershell
cd backend/tools
copy .env.example .env
# 编辑 .env，填入 TCB_SECRET_ID 和 TCB_SECRET_KEY
npm install
```

### Step 6. 生成第一批卡密测试

```powershell
node generate_codes.js VIP_NORMAL 5 test_batch
```

执行后：
- 云端 vip_codes 集合多 5 条记录
- 当前目录生成 `codes_export_test_batch_xxx.csv`

打开 CSV 看到的就是要发给客户的卡密。

### Step 7. 查询销售情况

```powershell
node admin_query.js                       # 总览
node admin_query.js batch test_batch      # 按批次
node admin_query.js code FL-XXXX-XXXX-XXXX # 查单个
```

### Step 8. 退款处理

用户退款 → 你在工具里禁用对应卡密：

```powershell
node admin_disable.js FL-XXXX-XXXX-XXXX
```
- 未兑换 → 直接作废
- 已兑换 → 该用户的 App 下次 verify 时凭证失效，VIP 自动消失

## 业务流程

### 售卖 → 兑换 → 服务

1. **你**：`node generate_codes.js VIP_NORMAL 50` 生成 50 个普通 VIP 卡密
2. **你**：把生成的 CSV 上传到淘宝店/网盘等渠道
3. **客户**：通过淘宝/朋友圈付款给你 ¥39.9
4. **你**：从 CSV 复制一个未发过的卡密发给客户
5. **客户**：在 App 里输入卡密 → 调用 redeem 云函数 → 拿到签名凭证 → 本地激活 VIP

### 客户换手机

1. **客户**：新手机装 App，在 VIP 页面点「迁移VIP」
2. **客户**：输入原卡密 → 调用 migrate 云函数
3. **云端**：把卡密的 `usedByDevice` 改为新设备，新设备得到新凭证
4. **旧手机**：下次启动 verify 失败，VIP 自动失效

### 防破解（定期复验）

1. App 每 7 天联网时自动调 verify
2. 云端比对本地凭证 → 重新签发凭证（续期 1 年）
3. 破解者改本地数据库 → 下次 verify 时签名错 → 失败

## 安全要点

- ✅ HMAC_SECRET 只在云端，APK 内不存在
- ✅ 客户端只验签不签发
- ✅ 凭证含 deviceId，跨设备无效
- ✅ 凭证 1 年过期，强制定期联网
- ✅ vip_codes 集合权限 ADMINONLY，客户端无法直接读
- ✅ 数据库原子更新防并发抢码
- ✅ migrateCount 限制 3 次防滥用

## 常见运营场景

| 场景 | 做法 |
|---|---|
| 改价格 | 改 `shared/sku.js` + 3 个函数下的 `sku.js`，重新部署 |
| 新增 SKU | 同上，加新条目；要给老卡密兼容 |
| 用户找不到卡密 | `node admin_query.js code <卡密>` 看状态 |
| 用户重装 App | redeem 接口对该设备幂等，重新兑换会还原 |
| 用户换手机 | 引导用 migrate 流程，3 次内自助 |
| 退款 | `node admin_disable.js <卡密>` |
| 恶意刷迁移 | 已限制 3 次；超过让用户走客服 |
