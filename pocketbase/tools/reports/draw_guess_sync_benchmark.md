# 你画我猜同步延迟对比报告

- 生成时间: 2026-06-07T01:15:23.195Z
- PocketBase: `https://pb.yishi.site`
- draw_ws: `wss://draw.yishi.site/ws`
- 样本数: 10

## 架构对比

| 版本 | 绘画热路径 | 归档 | 猜词方收笔方式 |
|------|------------|------|----------------|
| **v1** | 每笔 POST `game_moves` | 同左 | PB SSE ~800ms+ |
| **v2** | WS `stroke_chunk` ~16ms 节流 | 抬手 `stroke_end` → PB 一次 | WS 广播 + ledger 去重 |

## 延迟实测 (p50 / p95)

| 指标 | v1 基线 | v2 实测 p50 | v2 实测 p95 | 提升 |
|------|---------|-------------|-------------|------|
| PB POST draw_stroke | 520ms | 504ms | 506ms | 3% |
| WS chunk 猜词方收包 | 900ms | 506ms | 511ms | 44% |
| WS 双端 joined | 4000ms | 2613ms | 2613ms | 35% |
| 有效绘画延迟 (估) | 920ms | 506ms | 511ms | 45% |

## 解读

- **绘画热路径**：v2 WS chunk p50 **506ms**，相对 v1 有效延迟基线 **900ms** 约快 **44%**。
- **PB 归档**：抬手后仍走 POST（p50 **504ms**），与 v1 同量级，但绘画中不再每笔 POST，减少双写漂移。
- **进局 WS**：双端 joined **2613ms**（App 硬上限 4s）。
- **笔画稳定性**：v2 绘画中仅 WS 分片，ledger 同 strokeId 让位于 live 层，PB 归档后 drop live，避免「画完笔画移动」。

## 复现命令

```powershell
cd pocketbase
node tools/test_draw_guess_sync_benchmark.js --base-url https://pb.yishi.site --ws-url wss://draw.yishi.site/ws
```