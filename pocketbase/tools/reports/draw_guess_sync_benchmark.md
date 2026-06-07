# 你画我猜同步延迟对比报告

- 生成时间: 2026-06-07T16:15:00.356Z
- PocketBase: `http://127.0.0.1:8090`
- draw_ws: `wss://pb.yishi.site/draw-ws/ws`
- 样本数: 5

## 架构对比

| 版本 | 绘画热路径 | 归档 | 猜词方收笔方式 |
|------|------------|------|----------------|
| **v1** | 每笔 POST `game_moves` | 同左 | PB SSE ~800ms+ |
| **v2** | WS `stroke_chunk` ~16ms 节流 | 抬手 `stroke_end` → PB 一次 | WS 广播 + ledger 去重 |

## 延迟实测 (p50 / p95)

| 指标 | v1 基线 | v2 实测 p50 | v2 实测 p95 | 提升 |
|------|---------|-------------|-------------|------|
| PB POST draw_stroke | 520ms | 2ms | 2ms | 100% |
| WS chunk 猜词方收包 | 900ms | 492ms | 751ms | 45% |
| WS 双端 joined | 4000ms | 4134ms | 4134ms | -3% |
| 有效绘画延迟 (估) | 920ms | 492ms | 751ms | 47% |

## 解读

- **绘画热路径**：v2 WS chunk p50 **492ms**，相对 v1 有效延迟基线 **900ms** 约快 **45%**。
- **PB 归档**：抬手后仍走 POST（p50 **2ms**），与 v1 同量级，但绘画中不再每笔 POST，减少双写漂移。
- **进局 WS**：双端 joined **4134ms**（App 硬上限 4s）。
- **笔画稳定性**：v2 绘画中仅 WS 分片，ledger 同 strokeId 让位于 live 层，PB 归档后 drop live，避免「画完笔画移动」。

## 复现命令

```powershell
cd pocketbase
node tools/test_draw_guess_sync_benchmark.js --base-url http://127.0.0.1:8090 --ws-url wss://pb.yishi.site/draw-ws/ws
```