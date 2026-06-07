# draw_ws 与 PocketBase 同区域部署

将笔画 WebSocket 与 PocketBase 部署在同一 VPS，经**同一条 Cloudflare 隧道 / 同一域名**，可将 WS chunk RTT 从 ~500ms 降到 ~50–100ms。

## 拓扑

```
Android
  ├─ HTTPS  https://pb.yishi.site/api/...     → PocketBase :8090
  └─ WSS     wss://pb.yishi.site/draw-ws/ws   → draw_ws :8790
```

## Nginx 反代

```nginx
location /draw-ws/ {
    proxy_pass http://127.0.0.1:8790/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 86400;
}
```

健康检查：`GET https://pb.yishi.site/draw-ws/health`

## Android

留空 `DRAW_WS_URL`，仅配置 `POCKETBASE_URL=https://pb.yishi.site`，App 自动使用 `wss://pb.yishi.site/draw-ws`。

## 验证

```powershell
cd pocketbase\tools\draw_ws
.\deploy-co-located.ps1 -PbBase https://pb.yishi.site
```
