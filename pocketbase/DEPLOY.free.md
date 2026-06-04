# PocketBase 完全免费部署指南（FunLife 社交）

目标：手机**不用连电脑 WiFi** 也能访问社交服务，**长期 0 元**（个人开发 / 小规模好友）。

---

## 腾讯云能用吗？要花钱吗？

| 产品 | 是否适合 PocketBase | 是否「一直免费」 |
|------|---------------------|------------------|
| CloudBase **云函数** | ❌ 不适合（无长连接、无 SQLite 常驻） | 有免费额度，但跑不了 PocketBase |
| CloudBase **轻量 Lighthouse** | ✅ 适合 | ⚠️ 多为**试用/包月**，到期要续费，**不是永久免费** |
| 腾讯云 **CVM** | ✅ 适合 | 同上，按量/包月计费 |

**结论**：腾讯云可以部署，但若你要求**长期 0 元**，优先用下面两种，而不是 Lighthouse。

你现有的 `funlife-prod` 云函数继续管 VIP/AI 即可；**社交 PocketBase 单独部署**，不必塞进云函数。

---

## 真正免费方案（推荐顺序）

### 方案 1：Cloudflare 临时隧道（最快，0 元，5 分钟）

**优点**：不用买服务器、立刻得到 HTTPS。  
**缺点**：电脑要开着跑 PocketBase；临时域名**每次重启会变**（需改 App 配置）。

```powershell
# 终端 1：启动 PocketBase
cd d:\soft\pocketbase
.\start.ps1

# 终端 2：安装 cloudflared 后执行（见 https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/）
cloudflared tunnel --url http://127.0.0.1:8090
```

控制台会输出类似：`https://xxxx.trycloudflare.com`  

1. `local.properties`：
   ```properties
   POCKETBASE_URL=https://xxxx.trycloudflare.com
   ```
2. 重新安装 App：`.\gradlew installDebug`
3. 另开终端初始化 Schema（把地址换成你的）：
   ```powershell
   cd d:\soft\pocketbase
   $env:POCKETBASE_URL = "https://xxxx.trycloudflare.com"
   .\setup-schema.ps1
   ```

### 正式隧道（Named Tunnel，固定 HTTPS 域名）

**FunLife 域名：`yishi.site` → PocketBase 子域：`pb.yishi.site`**

一键脚本（推荐）：

```powershell
cd d:\soft\pocketbase
.\setup-tunnel-yishi.ps1
```

**前提**：`yishi.site` 已接入 Cloudflare（NS 指向 Cloudflare，或在阿里云 DNS 添加 CNAME 到 Cloudflare 隧道）。

**前提（通用）**：有一个域名并已接入 Cloudflare（免费套餐即可）。没有域名只能继续用上面的临时 `trycloudflare.com`。

1. **安装 cloudflared（Windows）**  
   下载：https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/  
   解压后将目录加入 PATH，或在解压目录打开 PowerShell。

2. **登录 Cloudflare**
   ```powershell
   cloudflared tunnel login
   ```
   浏览器授权后，证书会写到 `%USERPROFILE%\.cloudflared\cert.pem`。

3. **创建正式隧道**
   ```powershell
   cloudflared tunnel create funlife-pb
   ```
   记下输出的 **Tunnel UUID** 和 credentials 文件路径（`%USERPROFILE%\.cloudflared\<UUID>.json`）。

4. **编写配置** `%USERPROFILE%\.cloudflared\config.yml`（yishi.site 示例）：
   ```yaml
   tunnel: <你的 Tunnel UUID>
   credentials-file: C:\Users\<你的用户名>\.cloudflared\<UUID>.json
   ingress:
     - hostname: pb.yishi.site
       service: http://127.0.0.1:8090
     - service: http_status:404
   ```
   完整模板见 `pocketbase/cloudflared/config.yishi.site.example.yml`。

5. **绑定 DNS（固定公网地址）**
   ```powershell
   cloudflared tunnel route dns funlife-pb pb.yishi.site
   ```
   会在 Cloudflare 自动创建 CNAME → `*.cfargotunnel.com`。

6. **先启动 PocketBase，再启动隧道**
   ```powershell
   cd d:\soft\pocketbase
   .\start.ps1
   # 另开终端
   cloudflared tunnel run funlife-pb
   ```

7. **验证**  
   浏览器打开 `https://pb.yishi.site/api/health` 应返回 healthy。

8. **App 配置**
   ```properties
   POCKETBASE_URL=https://pb.yishi.site
   ```
   参考 `local.properties.production.example`。
   ```powershell
   cd d:\soft\pocketbase
   $env:POCKETBASE_URL = "https://pb.yishi.site"
   .\setup-schema.ps1
   .\gradlew installDebug
   ```

9. **开机自启（可选）**  
   `cloudflared service install` 后按官方文档把 config 复制到 systemprofile 目录。  
   文档：https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/local-management/as-a-service/windows/

**没有域名怎么办？** 可购买低价域名（通常每年几十元），或继续用临时隧道 / Oracle 免费 VM 直接暴露 IP+HTTPS。

---

### 方案 2：Oracle Cloud 永久免费 VM（最稳的 0 元公网）

**优点**：7×24 在线、固定公网 IP、不依赖家里电脑。  
**缺点**：注册与创建实例稍繁琐（需信用卡验证，**不扣费**选 Always Free 机型）。

1. 注册 [Oracle Cloud Free Tier](https://www.oracle.com/cloud/free/)
2. 创建 **Always Free** ARM 实例（Ubuntu 22.04）
3. 安全组/防火墙放行 **8090**（或 80/443 + Nginx）
4. SSH 上传 Linux 版 `pocketbase` + 可选 `pb_data/`
5. 运行：
   ```bash
   chmod +x pocketbase
   ./pocketbase serve --http=0.0.0.0:8090
   ```
6. 建议用 **Nginx + Let's Encrypt** 上 HTTPS（证书免费）
7. 本机执行：
   ```powershell
   $env:POCKETBASE_URL = "https://你的域名或IP:8090"
   .\setup-schema.ps1
   ```

---

### 方案 3：开发期不部署（0 元，仅本机）

不追求公网时，**不必部署**：

```powershell
cd d:\soft\pocketbase
.\start.ps1
.\allow-firewall.ps1   # 真机 WiFi 访问时
```

`local.properties`：`POCKETBASE_URL=http://电脑局域网IP:8090`  

或 USB：`adb reverse tcp:8090 tcp:8090` + `POCKETBASE_URL=http://127.0.0.1:8090`

---

### 不推荐当作「永久免费」的

| 方案 | 说明 |
|------|------|
| Fly.io / Railway / Render | 有额度，用完或休眠后要付费 |
| 腾讯云 Lighthouse | 试用结束后收费 |
| PikaPods 等托管 | 多为试用 |

---

## App 配置提醒

```properties
# local.properties
POCKETBASE_URL=https://你的公网HTTPS地址
```

- **Debug** 可用 `http://`（仅开发）
- **Release 正式包** 必须 **HTTPS**（`PocketBaseConfig` 已限制）

---

## 免费部署 ≠ 微信杀进程秒推

公网 HTTPS 解决：**连接中、换 WiFi 连不上**。  

要做到接近微信「划掉 App 也能推」，还需：

- Firebase + `google-services.json`
- `pb_hooks` 给 **messages** 加 FCM（当前 Hook  mainly 好友申请）
- 见 `PUSH_SETUP.md`

---

## 数据与安全

- 备份 `pb_data/`（用户、好友、聊天记录）
- 修改 `DEV.local.md` 默认管理员密码
- 勿把 `DEV.local.md`、`.env` 提交到 git
