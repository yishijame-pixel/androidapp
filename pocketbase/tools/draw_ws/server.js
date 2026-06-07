/**
 * FunLife Draw & Guess — 企业级笔画 WebSocket 中继
 *
 * 职责（热路径）：
 *   - 房间内 stroke_chunk / clear 低延迟广播
 *   - PB JWT 鉴权 + 房间成员校验
 *   - 断线重连：环形缓冲回放最近 N 条
 *
 * 不负责（冷路径，仍走 PocketBase）：
 *   - 用户/好友、房间创建、猜词、计分、阶段切换
 *   - 笔画持久化账本（客户端 stroke_end 后 POST game_moves）
 *
 * 环境变量：
 *   PORT              默认 8790
 *   PB_BASE_URL       PocketBase API 根，如 https://pb.yishi.site
 *   DRAW_WS_RELAY_KEY 可选，连接时 Header Authorization: Bearer <key>
 *   RING_SIZE         每房间缓冲条数，默认 400
 *   MAX_MSG_PER_SEC   每连接限速，默认 40
 *   PB_AUTH_TIMEOUT_MS 默认 5000
 */

const http = require("http");
const { WebSocketServer } = require("ws");
const { authenticateJoin } = require("./pbAuth");

const PORT = Number(process.env.PORT || 8790);
const PB_BASE_URL = process.env.PB_BASE_URL || "http://127.0.0.1:8090";
const RELAY_KEY = process.env.DRAW_WS_RELAY_KEY || "";
const RING_SIZE = Number(process.env.RING_SIZE || 400);
const MAX_MSG_PER_SEC = Number(process.env.MAX_MSG_PER_SEC || 120);
const PB_AUTH_TIMEOUT_MS = Number(process.env.PB_AUTH_TIMEOUT_MS || 5000);

/** @type {Map<string, Set<import('ws').WebSocket>>} */
const roomPeers = new Map();
/** @type {Map<string, object[]>} */
const roomRing = new Map();
/** @type {WeakMap<import('ws').WebSocket, {userId:string, roomId:string, rate:object}>} */
const peerMeta = new WeakMap();

function replyJson(ws, obj) {
  if (ws.readyState !== ws.OPEN) return;
  ws.send(JSON.stringify(obj));
}

function pushRing(roomId, msg) {
  let ring = roomRing.get(roomId);
  if (!ring) {
    ring = [];
    roomRing.set(roomId, ring);
  }
  ring.push(msg);
  if (ring.length > RING_SIZE) ring.splice(0, ring.length - RING_SIZE);
}

function broadcast(roomId, msg, exceptWs) {
  pushRing(roomId, msg);
  const peers = roomPeers.get(roomId);
  if (!peers) return;
  const raw = JSON.stringify(msg);
  for (const peer of peers) {
    if (peer !== exceptWs && peer.readyState === peer.OPEN) {
      peer.send(raw);
    }
  }
}

function addPeer(roomId, ws) {
  let set = roomPeers.get(roomId);
  if (!set) {
    set = new Set();
    roomPeers.set(roomId, set);
  }
  set.add(ws);
}

function removePeer(ws) {
  const meta = peerMeta.get(ws);
  if (!meta) return;
  const set = roomPeers.get(meta.roomId);
  if (set) {
    set.delete(ws);
    if (set.size === 0) roomPeers.delete(meta.roomId);
  }
  peerMeta.delete(ws);
}

function allowRate(ws) {
  const meta = peerMeta.get(ws);
  if (!meta) return false;
  const now = Date.now();
  if (now - meta.rate.windowStart >= 1000) {
    meta.rate.windowStart = now;
    meta.rate.count = 0;
  }
  meta.rate.count += 1;
  return meta.rate.count <= MAX_MSG_PER_SEC;
}

function parseUrl(req) {
  try {
    return new URL(req.url, "http://localhost");
  } catch {
    return null;
  }
}

const server = http.createServer((req, res) => {
  if (req.method === "GET" && req.url === "/health") {
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({
      ok: true,
      rooms: roomPeers.size,
      service: "funlife-draw-ws",
      version: 1,
    }));
    return;
  }
  res.writeHead(404);
  res.end();
});

const wss = new WebSocketServer({ server, path: "/ws" });

wss.on("connection", (ws, req) => {
  const url = parseUrl(req);
  if (!url) {
    ws.close(4000, "bad_url");
    return;
  }
  if (RELAY_KEY) {
    const auth = req.headers.authorization || "";
    if (auth !== "Bearer " + RELAY_KEY) {
      ws.close(4001, "unauthorized");
      return;
    }
  }

  const token = (url.searchParams.get("token") || "").trim();
  const roomId = (url.searchParams.get("room") || "").trim();
  if (!token || !roomId) {
    ws.close(4002, "missing_token_or_room");
    return;
  }

  let joined = false;

  authenticateJoin(PB_BASE_URL, token, roomId, PB_AUTH_TIMEOUT_MS)
    .then(({ userId, room }) => {
      peerMeta.set(ws, {
        userId,
        roomId,
        rate: { windowStart: Date.now(), count: 0 },
      });
      addPeer(roomId, ws);
      joined = true;
      replyJson(ws, {
        t: "joined",
        v: 1,
        room: roomId,
        userId,
        status: room.status,
      });
      const ring = roomRing.get(roomId) || [];
      const tail = ring.slice(-80);
      if (tail.length > 0) {
        replyJson(ws, { t: "replay", v: 1, room: roomId, events: tail });
      }
    })
    .catch((err) => {
      console.warn("[draw_ws] auth failed room=" + roomId + " reason=" + err.message);
      ws.close(4003, "auth_failed");
    });

  ws.on("message", (data) => {
    if (!joined) return;
    const meta = peerMeta.get(ws);
    if (!meta) return;
    if (!allowRate(ws)) {
      replyJson(ws, { t: "error", v: 1, code: "rate_limit" });
      return;
    }
    let msg;
    try {
      msg = JSON.parse(data.toString("utf8"));
    } catch {
      replyJson(ws, { t: "error", v: 1, code: "bad_json" });
      return;
    }
    if (!msg || typeof msg !== "object") return;
    const t = msg.t;
    if (t === "ping") {
      replyJson(ws, { t: "pong", v: 1, ts: Date.now() });
      return;
    }
    if (msg.room && msg.room !== meta.roomId) {
      replyJson(ws, { t: "error", v: 1, code: "wrong_room" });
      return;
    }
    if (t === "stroke_chunk" || t === "stroke_end" || t === "clear") {
      const out = Object.assign({}, msg, {
        v: 1,
        room: meta.roomId,
        from: meta.userId,
        serverTs: Date.now(),
      });
      var peers = roomPeers.get(meta.roomId);
      var fanout = peers ? peers.size - 1 : 0;
      if (t === "stroke_chunk") {
        console.log("[draw_ws] chunk room=" + meta.roomId + " from=" + meta.userId + " fanout=" + fanout);
      }
      broadcast(meta.roomId, out, ws);
      return;
    }
    replyJson(ws, { t: "error", v: 1, code: "unknown_type" });
  });

  ws.on("close", () => removePeer(ws));
  ws.on("error", () => removePeer(ws));
});

server.listen(PORT, () => {
  console.log("[draw_ws] listening on :" + PORT + " pb=" + PB_BASE_URL);
});
