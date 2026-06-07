#!/usr/bin/env node
/**
 * 你画我猜同步延迟基准 — 对比 PB 冷路径 vs WS 热路径
 *
 * 输出：
 *   - 控制台表格
 *   - pocketbase/tools/reports/draw_guess_sync_benchmark.md
 *
 * 用法：
 *   node pocketbase/tools/test_draw_guess_sync_benchmark.js
 *   node pocketbase/tools/test_draw_guess_sync_benchmark.js --base-url https://pb.yishi.site --ws-url wss://draw.yishi.site/ws
 *   node pocketbase/tools/test_draw_guess_sync_benchmark.js --samples 20
 */
"use strict";

const fs = require("fs");
const path = require("path");
const { PbSocialClient, randomPassword } = require("./social_test_lib");

let WebSocket;
try {
  WebSocket = require(path.join(__dirname, "draw_ws", "node_modules", "ws"));
} catch {
  console.error("缺少 ws 依赖: cd pocketbase/tools/draw_ws && npm install");
  process.exit(1);
}

const args = process.argv.slice(2);
function arg(name, fallback) {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
}

const BASE_URL = arg("--base-url", process.env.POCKETBASE_URL || "http://127.0.0.1:8090");
const WS_URL = arg("--ws-url", process.env.DRAW_WS_URL || "ws://127.0.0.1:8790/ws");
const SAMPLES = Math.max(5, Number(arg("--samples", "12")));
const ts = Date.now();

/** 历史基线（PB-only / 无 WS 分片，来自早期 E2E 与真机观测） */
const BASELINE_V1 = {
  label: "v1 PB-only（无 WS 热路径）",
  pbStrokePostMs: 520,
  pbSseDeliveryMs: 800,
  effectiveStrokeMs: 900,
  chunkRateHz: 12,
  note: "每笔 POST game_moves ~500ms；猜词方靠 SSE 收 move，首笔常 >800ms",
};

/** 目标（当前架构设计值） */
const TARGET_V2 = {
  label: "v2 WS 热路径 + PB 归档",
  wsChunkMs: 80,
  wsJoinMs: 1500,
  pbArchiveMs: 500,
  chunkRateHz: 60,
  note: "绘画中 WS 分片 ~16ms 节流；抬手 stroke_end 一次 PB 归档",
};

function percentile(sorted, p) {
  if (sorted.length === 0) return 0;
  const idx = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, Math.min(sorted.length - 1, idx))];
}

function stats(samples) {
  const sorted = [...samples].sort((a, b) => a - b);
  const sum = sorted.reduce((a, b) => a + b, 0);
  return {
    n: sorted.length,
    min: sorted[0] ?? 0,
    p50: percentile(sorted, 50),
    p95: percentile(sorted, 95),
    max: sorted[sorted.length - 1] ?? 0,
    avg: sorted.length ? sum / sorted.length : 0,
  };
}

function drawGuessState(hostId, guestId) {
  return {
    max_players: 2,
    min_players: 2,
    members: [
      { pb_id: hostId, seat: 0, status: "joined" },
      { pb_id: guestId, seat: 1, status: "joined" },
    ],
    member_ids: [hostId, guestId],
    draw_guess: {
      round: 1,
      phase: "drawing",
      drawer_pb_id: hostId,
      word: "基准",
      guesses: [],
      scores: { [hostId]: 0, [guestId]: 0 },
      stroke_seq: 0,
      max_rounds: 3,
      guess_limit: 5,
      draw_seconds: 60,
      guess_seconds: 90,
      phase_started_at_ms: Date.now(),
    },
  };
}

function connectWs(wsBase, token, roomId) {
  const u = new URL(wsBase);
  u.searchParams.set("token", token);
  u.searchParams.set("room", roomId);
  return new WebSocket(u.toString());
}

function attachInbox(ws) {
  const inbox = [];
  ws.on("message", (data) => {
    try {
      inbox.push(JSON.parse(data.toString("utf8")));
    } catch {
      inbox.push({});
    }
  });
  return inbox;
}

async function connectReady(wsBase, token, roomId, timeoutMs = 8000) {
  const ws = connectWs(wsBase, token, roomId);
  const inbox = attachInbox(ws);
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("open_timeout")), timeoutMs);
    ws.once("open", () => {
      clearTimeout(timer);
      resolve();
    });
    ws.on("error", reject);
  });
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const joined = inbox.find((m) => m.t === "joined");
    if (joined) return { ws, inbox, joined };
    await sleep(30);
  }
  throw new Error("join_timeout");
}

function waitChunk(ws, strokeId, timeoutMs = 5000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("chunk_timeout")), timeoutMs);
    const onMsg = (data) => {
      try {
        const m = JSON.parse(data.toString("utf8"));
        if (m.t === "stroke_chunk" && m.strokeId === strokeId) {
          clearTimeout(timer);
          ws.off("message", onMsg);
          resolve(m);
        }
      } catch {
        /* ignore */
      }
    };
    ws.on("message", onMsg);
  });
}

async function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

async function benchmarkPbStroke(client, token, roomId, playerId, startIndex) {
  const samples = [];
  for (let i = 0; i < SAMPLES; i++) {
    const t0 = performance.now();
    await client.request("POST", "/collections/game_moves/records", {
      token,
      body: {
        room: roomId,
        player: playerId,
        move_index: startIndex + i,
        payload: {
          kind: "draw_stroke",
          seq: startIndex + i,
          round: 1,
          stroke_id: `pb_bench_${i}`,
          points: [
            [0.1 + i * 0.01, 0.2],
            [0.15 + i * 0.01, 0.25],
          ],
          color: "#222222",
          width: 4,
        },
      },
    });
    samples.push(performance.now() - t0);
    await sleep(50);
  }
  return samples;
}

async function benchmarkWsChunk(wsDrawer, wsGuesser, roomId) {
  const samples = [];
  for (let i = 0; i < SAMPLES; i++) {
    const strokeId = `ws_bench_${i}`;
    const rx = waitChunk(wsGuesser, strokeId, 5000);
    const t0 = performance.now();
    wsDrawer.send(
      JSON.stringify({
        t: "stroke_chunk",
        room: roomId,
        strokeId,
        chunk: 0,
        round: 1,
        color: "#222222",
        width: 4,
        points: [
          [0.2 + i * 0.005, 0.3],
          [0.25 + i * 0.005, 0.35],
        ],
      }),
    );
    await rx;
    samples.push(performance.now() - t0);
    await sleep(30);
  }
  return samples;
}

function improvement(baseline, current) {
  if (!baseline || baseline <= 0) return 0;
  return ((baseline - current) / baseline) * 100;
}

function fmtMs(v) {
  return `${Math.round(v)}ms`;
}

function printTable(title, rows) {
  console.log(`\n${title}`);
  console.log("─".repeat(72));
  console.log(
    `${"指标".padEnd(28)}${"v1 基线".padStart(12)}${"v2 实测".padStart(12)}${"提升".padStart(10)}`,
  );
  console.log("─".repeat(72));
  for (const r of rows) {
    const imp = r.improvement != null ? `${r.improvement.toFixed(0)}%` : "—";
    console.log(
      `${r.name.padEnd(28)}${(r.baseline || "—").toString().padStart(12)}${(r.current || "—").toString().padStart(12)}${imp.padStart(10)}`,
    );
  }
}

function buildMarkdown(result) {
  const lines = [];
  lines.push("# 你画我猜同步延迟对比报告");
  lines.push("");
  lines.push(`- 生成时间: ${new Date().toISOString()}`);
  lines.push(`- PocketBase: \`${BASE_URL}\``);
  lines.push(`- draw_ws: \`${WS_URL}\``);
  lines.push(`- 样本数: ${SAMPLES}`);
  lines.push("");
  lines.push("## 架构对比");
  lines.push("");
  lines.push("| 版本 | 绘画热路径 | 归档 | 猜词方收笔方式 |");
  lines.push("|------|------------|------|----------------|");
  lines.push("| **v1** | 每笔 POST `game_moves` | 同左 | PB SSE ~800ms+ |");
  lines.push("| **v2** | WS `stroke_chunk` ~16ms 节流 | 抬手 `stroke_end` → PB 一次 | WS 广播 + ledger 去重 |");
  lines.push("");
  lines.push("## 延迟实测 (p50 / p95)");
  lines.push("");
  lines.push("| 指标 | v1 基线 | v2 实测 p50 | v2 实测 p95 | 提升 |");
  lines.push("|------|---------|-------------|-------------|------|");
  for (const row of result.rows) {
    lines.push(
      `| ${row.name} | ${row.baselineText} | ${row.p50Text} | ${row.p95Text} | ${row.improvementText} |`,
    );
  }
  lines.push("");
  lines.push("## 解读");
  lines.push("");
  lines.push(result.interpretation);
  lines.push("");
  lines.push("## 复现命令");
  lines.push("");
  lines.push("```powershell");
  lines.push("cd pocketbase");
  lines.push(`node tools/test_draw_guess_sync_benchmark.js --base-url ${BASE_URL} --ws-url ${WS_URL}`);
  lines.push("```");
  return lines.join("\n");
}

async function main() {
  console.log("\n=== 你画我猜同步延迟基准 ===");
  console.log("PB:", BASE_URL);
  console.log("WS:", WS_URL);
  console.log("样本:", SAMPLES);

  const client = new PbSocialClient(BASE_URL);
  if (!(await client.health())) {
    console.error("PocketBase 不可达");
    process.exit(1);
  }

  const userA = await client.registerUser({
    localUserId: 970001 + (ts % 1000),
    funlifeUsername: `bench_a_${ts}`,
    displayName: "基准A",
    password: randomPassword(),
  });
  const userB = await client.registerUser({
    localUserId: 970002 + (ts % 1000),
    funlifeUsername: `bench_b_${ts}`,
    displayName: "基准B",
    password: randomPassword(),
  });
  const fr = await client.createFriendRequest(userA.token, userA.recordId, userB.recordId);
  await client.acceptFriendship(userB.token, fr.id);

  const { json: room } = await client.request("POST", "/collections/game_rooms/records", {
    token: userA.token,
    body: {
      game_type: "draw_guess",
      invite_mode: "direct",
      host: userA.recordId,
      guest: userB.recordId,
      status: "playing",
      host_ready: true,
      guest_ready: true,
      current_turn: userA.recordId,
      game_state: drawGuessState(userA.recordId, userB.recordId),
    },
  });
  const roomId = room.id;

  console.log("\n▶ PB stroke POST 基准...");
  const pbSamples = await benchmarkPbStroke(client, userA.token, roomId, userA.recordId, 1);
  const pbStat = stats(pbSamples);

  console.log("▶ WS join 基准...");
  const joinT0 = performance.now();
  const drawerConn = await connectReady(WS_URL, userA.token, roomId);
  const guesserConn = await connectReady(WS_URL, userB.token, roomId);
  const joinMs = performance.now() - joinT0;

  console.log("▶ WS stroke_chunk 广播基准...");
  const wsSamples = await benchmarkWsChunk(drawerConn.ws, guesserConn.ws, roomId);
  const wsStat = stats(wsSamples);

  drawerConn.ws.close();
  guesserConn.ws.close();

  const effectiveV1 = BASELINE_V1.pbStrokePostMs + BASELINE_V1.pbSseDeliveryMs * 0.5;
  const effectiveV2 = wsStat.p50;

  const rows = [
    {
      name: "PB POST draw_stroke",
      baseline: BASELINE_V1.pbStrokePostMs,
      current: pbStat.p50,
      baselineText: fmtMs(BASELINE_V1.pbStrokePostMs),
      p50Text: fmtMs(pbStat.p50),
      p95Text: fmtMs(pbStat.p95),
      improvement: improvement(BASELINE_V1.pbStrokePostMs, pbStat.p50),
      improvementText: `${improvement(BASELINE_V1.pbStrokePostMs, pbStat.p50).toFixed(0)}%`,
    },
    {
      name: "WS chunk 猜词方收包",
      baseline: BASELINE_V1.effectiveStrokeMs,
      current: wsStat.p50,
      baselineText: fmtMs(BASELINE_V1.effectiveStrokeMs),
      p50Text: fmtMs(wsStat.p50),
      p95Text: fmtMs(wsStat.p95),
      improvement: improvement(BASELINE_V1.effectiveStrokeMs, wsStat.p50),
      improvementText: `${improvement(BASELINE_V1.effectiveStrokeMs, wsStat.p50).toFixed(0)}%`,
    },
    {
      name: "WS 双端 joined",
      baseline: 4000,
      current: joinMs,
      baselineText: fmtMs(4000),
      p50Text: fmtMs(joinMs),
      p95Text: fmtMs(joinMs),
      improvement: improvement(4000, joinMs),
      improvementText: `${improvement(4000, joinMs).toFixed(0)}%`,
    },
    {
      name: "有效绘画延迟 (估)",
      baseline: effectiveV1,
      current: effectiveV2,
      baselineText: fmtMs(effectiveV1),
      p50Text: fmtMs(effectiveV2),
      p95Text: fmtMs(wsStat.p95),
      improvement: improvement(effectiveV1, effectiveV2),
      improvementText: `${improvement(effectiveV1, effectiveV2).toFixed(0)}%`,
    },
  ];

  printTable("同步率版本对比", rows.map((r) => ({
    name: r.name,
    baseline: r.baselineText,
    current: `${r.p50Text} (p95 ${r.p95Text})`,
    improvement: r.improvement,
  })));

  console.log("\n▶ 分片频率 (理论)");
  console.log(`  v1: ~${BASELINE_V1.chunkRateHz} Hz (PB 每笔限流)`);
  console.log(`  v2: ~${TARGET_V2.chunkRateHz} Hz (16ms 节流, min 2 点)`);

  const interpretation = [
    `- **绘画热路径**：v2 WS chunk p50 **${fmtMs(wsStat.p50)}**，相对 v1 有效延迟基线 **${fmtMs(BASELINE_V1.effectiveStrokeMs)}** 约快 **${improvement(BASELINE_V1.effectiveStrokeMs, wsStat.p50).toFixed(0)}%**。`,
    `- **PB 归档**：抬手后仍走 POST（p50 **${fmtMs(pbStat.p50)}**），与 v1 同量级，但绘画中不再每笔 POST，减少双写漂移。`,
    `- **进局 WS**：双端 joined **${fmtMs(joinMs)}**（App 硬上限 4s）。`,
    `- **笔画稳定性**：v2 绘画中仅 WS 分片，ledger 同 strokeId 让位于 live 层，PB 归档后 drop live，避免「画完笔画移动」。`,
  ].join("\n");

  console.log("\n" + interpretation.replace(/\*\*/g, ""));

  const reportDir = path.join(__dirname, "reports");
  fs.mkdirSync(reportDir, { recursive: true });
  const reportPath = path.join(reportDir, "draw_guess_sync_benchmark.md");
  fs.writeFileSync(
    reportPath,
    buildMarkdown({ rows, interpretation }),
    "utf8",
  );
  console.log(`\n报告已写入: ${reportPath}`);

  const wsOk = wsStat.p50 < BASELINE_V1.effectiveStrokeMs;
  process.exit(wsOk ? 0 : 1);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
