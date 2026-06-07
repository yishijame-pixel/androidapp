/**
 * FunLife Draw & Guess — 企业级笔画 WebSocket 中继
 *
 * 职责（热路径）：
 *   - 房间内 stroke_chunk / clear 低延迟广播
 *   - PB JWT 鉴权 + 房间成员校验
 *   - 断线重连：环形缓冲 + strokeId 画布快照
 *   - 房间齐步走：ready 握手指 / room_go 统一起跑
 *
 * 冷路径仍走 PocketBase：猜词、计分、笔画持久化账本
 */

const http = require("http");
const { WebSocketServer } = require("ws");
const { authenticateJoin } = require("./pbAuth");
const binary = require("./binary");

const PORT = Number(process.env.PORT || 8790);
const PB_BASE_URL = process.env.PB_BASE_URL || "http://127.0.0.1:8090";
const RELAY_KEY = process.env.DRAW_WS_RELAY_KEY || "";
const RING_SIZE = Number(process.env.RING_SIZE || 400);
const MAX_MSG_PER_SEC = Number(process.env.MAX_MSG_PER_SEC || 240);
const PB_AUTH_TIMEOUT_MS = Number(process.env.PB_AUTH_TIMEOUT_MS || 4000);
const REPLAY_TAIL = Number(process.env.DRAW_WS_REPLAY_TAIL || 320);
const ROOM_GO_TIMEOUT_MS = Number(process.env.DRAW_WS_ROOM_GO_MS || 8000);
const LOG_CHUNKS = process.env.DRAW_WS_LOG_CHUNKS === "1";

/** @type {Map<string, Set<import('ws').WebSocket>>} */
const roomPeers = new Map();
/** @type {Map<string, object[]>} */
const roomRing = new Map();
/** @type {Map<string, Map<string, object>>} strokeId -> stroke snapshot */
const roomSnapshots = new Map();
/** @type {Map<string, {ready:Set<string>, goTs:number|null, timer:ReturnType<typeof setTimeout>|null}>} */
const roomSyncByKey = new Map();
/** @type {Map<string, {ts:number, round:number}>} */
const roomLastGo = new Map();
/** @type {WeakMap<import('ws').WebSocket, {userId:string, roomId:string, roomStatus:string, rate:object}>} */
const peerMeta = new WeakMap();

function replyJson(ws, obj) {
  if (ws.readyState !== ws.OPEN) return;
  ws.send(JSON.stringify(obj));
}

function syncKey(roomId, round) {
  return roomId + ":" + (round || 1);
}

function getRoomSync(roomId, round) {
  const key = syncKey(roomId, round);
  if (!roomSyncByKey.has(key)) {
    roomSyncByKey.set(key, { ready: new Set(), goTs: null, timer: null });
  }
  return roomSyncByKey.get(key);
}

function peerCount(roomId) {
  const peers = roomPeers.get(roomId);
  return peers ? peers.size : 0;
}

function mergePoints(existing, incoming) {
  if (!incoming || incoming.length === 0) return existing || [];
  if (!existing || existing.length === 0) return incoming.slice();
  const ex = existing[existing.length - 1];
  const ix = incoming[0];
  const dropFirst =
    ex && ix &&
    Math.abs(ex[0] - ix[0]) < 0.00005 &&
    Math.abs(ex[1] - ix[1]) < 0.00005;
  return existing.concat(dropFirst ? incoming.slice(1) : incoming);
}

function mergeSnapshot(roomId, msg) {
  if (msg.t === "clear") {
    roomSnapshots.delete(roomId);
    return;
  }
  if (msg.t !== "stroke_chunk" && msg.t !== "stroke_end") return;
  const strokeId = msg.strokeId;
  if (!strokeId) return;
  let map = roomSnapshots.get(roomId);
  if (!map) {
    map = new Map();
    roomSnapshots.set(roomId, map);
  }
  const prev = map.get(strokeId);
  const finalized = msg.t === "stroke_end";
  const points = finalized && msg.points && msg.points.length > 0
    ? msg.points
    : mergePoints(prev ? prev.points : [], msg.points || []);
  map.set(strokeId, {
    t: finalized ? "stroke_end" : "stroke_chunk",
    strokeId,
    chunk: msg.chunk || 0,
    round: msg.round || 1,
    color: msg.color || (prev && prev.color) || "#222222",
    width: msg.width != null ? msg.width : (prev && prev.width) || 4,
    from: msg.from || (prev && prev.from) || "",
    points,
    seq: msg.seq != null ? msg.seq : (prev && prev.seq),
  });
}

function snapshotEvents(roomId) {
  const map = roomSnapshots.get(roomId);
  if (!map || map.size === 0) return [];
  return Array.from(map.values());
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

function broadcastBinary(roomId, payload, exceptWs) {
  const peers = roomPeers.get(roomId);
  if (!peers) return;
  for (const peer of peers) {
    if (peer !== exceptWs && peer.readyState === peer.OPEN) {
      peer.send(payload);
    }
  }
}

function broadcastJson(roomId, msg, exceptWs) {
  const peers = roomPeers.get(roomId);
  if (!peers) return;
  const raw = JSON.stringify(msg);
  for (const peer of peers) {
    if (peer !== exceptWs && peer.readyState === peer.OPEN) {
      peer.send(raw);
    }
  }
}

function broadcast(roomId, msg, exceptWs) {
  pushRing(roomId, msg);
  broadcastJson(roomId, msg, exceptWs);
}

function broadcastRoomState(roomId, round) {
  const sync = getRoomSync(roomId, round);
  const body = {
    t: "room_state",
    v: 1,
    room: roomId,
    round: round || 1,
    peerCount: peerCount(roomId),
    expectedPeers: 2,
    readyCount: sync.ready.size,
  };
  broadcastJson(roomId, body, null);
}

function fireRoomGo(roomId, round) {
  const sync = getRoomSync(roomId, round);
  if (sync.goTs != null) return;
  sync.goTs = Date.now();
  if (sync.timer) {
    clearTimeout(sync.timer);
    sync.timer = null;
  }
  roomLastGo.set(roomId, { ts: sync.goTs, round: round || 1 });
  broadcastJson(roomId, {
    t: "room_go",
    v: 1,
    room: roomId,
    round: round || 1,
    serverTs: sync.goTs,
    peerCount: peerCount(roomId),
    readyCount: sync.ready.size,
  }, null);
}

function tryFireRoomGo(roomId, round) {
  const sync = getRoomSync(roomId, round);
  if (sync.goTs != null) return;
  const peers = peerCount(roomId);
  if (sync.ready.size >= 2 && peers >= 2) {
    fireRoomGo(roomId, round);
  }
}

function scheduleRoomGoTimer(roomId, round) {
  const sync = getRoomSync(roomId, round);
  if (sync.goTs != null || sync.timer) return;
  sync.timer = setTimeout(() => fireRoomGo(roomId, round), ROOM_GO_TIMEOUT_MS);
}

function markReady(roomId, round, userId) {
  const sync = getRoomSync(roomId, round);
  sync.ready.add(userId);
  tryFireRoomGo(roomId, round);
  if (sync.goTs == null) scheduleRoomGoTimer(roomId, round);
}

function clearRoomIfEmpty(roomId) {
  const peers = roomPeers.get(roomId);
  if (peers && peers.size > 0) return;
  roomPeers.delete(roomId);
  roomRing.delete(roomId);
  roomSnapshots.delete(roomId);
  roomLastGo.delete(roomId);
  for (const key of roomSyncByKey.keys()) {
    if (key.startsWith(roomId + ":")) {
      const sync = roomSyncByKey.get(key);
      if (sync && sync.timer) clearTimeout(sync.timer);
      roomSyncByKey.delete(key);
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
  const roomId = meta.roomId;
  const set = roomPeers.get(roomId);
  if (set) {
    set.delete(ws);
  }
  peerMeta.delete(ws);
  clearRoomIfEmpty(roomId);
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

function normalizePath(pathname) {
  if (pathname === "/draw-ws" || pathname === "/draw-ws/") return "/";
  if (pathname.startsWith("/draw-ws/")) return pathname.slice("/draw-ws".length);
  return pathname;
}

const WS_PATHS = new Set(["/ws", "/draw-ws/ws"]);

const server = http.createServer((req, res) => {
  const url = parseUrl(req);
  const path = url ? normalizePath(url.pathname) : "";
  if (req.method === "GET" && path === "/health") {
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({
      ok: true,
      rooms: roomPeers.size,
      service: "funlife-draw-ws",
      version: 2,
      pathPrefix: "/draw-ws",
    }));
    return;
  }
  res.writeHead(404);
  res.end();
});

const wss = new WebSocketServer({ noServer: true });

server.on("upgrade", (req, socket, head) => {
  const url = parseUrl(req);
  if (!url || !WS_PATHS.has(url.pathname)) {
    socket.destroy();
    return;
  }
  wss.handleUpgrade(req, socket, head, (ws) => {
    wss.emit("connection", ws, req);
  });
});

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
        roomStatus: room.status || "",
        rate: { windowStart: Date.now(), count: 0 },
      });
      addPeer(roomId, ws);
      joined = true;
      const pc = peerCount(roomId);
      const defaultRound = 1;
      const sync = getRoomSync(roomId, defaultRound);
      replyJson(ws, {
        t: "joined",
        v: 1,
        room: roomId,
        userId,
        status: room.status,
        peerCount: pc,
        expectedPeers: 2,
        readyCount: sync.ready.size,
      });
      const snap = snapshotEvents(roomId);
      if (snap.length > 0) {
        replyJson(ws, { t: "snapshot", v: 1, room: roomId, events: snap });
      }
      const ring = roomRing.get(roomId) || [];
      const tail = ring.slice(-REPLAY_TAIL);
      if (tail.length > 0) {
        replyJson(ws, { t: "replay", v: 1, room: roomId, events: tail });
      }
      const playing = (room.status || "").toLowerCase() === "playing";
      const lastGo = roomLastGo.get(roomId);
      if (playing && lastGo) {
        replyJson(ws, {
          t: "room_go",
          v: 1,
          room: roomId,
          round: lastGo.round,
          serverTs: lastGo.ts,
          peerCount: pc,
          readyCount: sync.ready.size,
        });
      }
      broadcastRoomState(roomId, defaultRound);
    })
    .catch((err) => {
      console.warn("[draw_ws] auth failed room=" + roomId + " reason=" + err.message);
      ws.close(4003, "auth_failed");
    });

  ws.on("message", (data, isBinary) => {
    if (!joined) return;
    const meta = peerMeta.get(ws);
    if (!meta) return;
    if (!allowRate(ws)) {
      replyJson(ws, { t: "error", v: 1, code: "rate_limit" });
      return;
    }
    let msg;
    try {
      if (isBinary || binary.isBinary(data)) {
        msg = binary.decodeMessage(data);
        if (!msg) {
          replyJson(ws, { t: "error", v: 1, code: "bad_binary" });
          return;
        }
      } else {
        msg = JSON.parse(data.toString("utf8"));
      }
    } catch {
      replyJson(ws, { t: "error", v: 1, code: "bad_json" });
      return;
    }
    if (!msg || typeof msg !== "object") return;
    const t = msg.t;
    if (t === "ping") {
      if (isBinary || binary.isBinary(data)) {
        ws.send(Buffer.from([0xfd, 0x47, 1, binary.TYPE_PING]));
      } else {
        replyJson(ws, { t: "pong", v: 1, ts: Date.now() });
      }
      return;
    }
    if (t === "ready") {
      const round = msg.round || 1;
      if ((meta.roomStatus || "").toLowerCase() !== "playing") {
        replyJson(ws, {
          t: "room_state",
          v: 1,
          room: meta.roomId,
          round,
          peerCount: peerCount(meta.roomId),
          expectedPeers: 2,
          readyCount: getRoomSync(meta.roomId, round).ready.size,
          waiting: true,
        });
        return;
      }
      markReady(meta.roomId, round, meta.userId);
      broadcastRoomState(meta.roomId, round);
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
      mergeSnapshot(meta.roomId, out);
      if (LOG_CHUNKS && t === "stroke_chunk") {
        console.log("[draw_ws] chunk room=" + meta.roomId + " stroke=" + out.strokeId);
      }
      if (isBinary || binary.isBinary(data)) {
        pushRing(meta.roomId, out);
        const raw = Buffer.isBuffer(data) ? data : Buffer.from(data);
        broadcastBinary(meta.roomId, binary.appendRelayFrom(raw, meta.userId), ws);
      } else {
        broadcast(meta.roomId, out, ws);
      }
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
