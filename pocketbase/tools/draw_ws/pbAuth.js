/**
 * 校验 PocketBase JWT，并确认用户属于房间（host / guest）。
 * 优化：本地解码 JWT 取 userId，仅 1 次 GET room（省掉 auth-refresh 往返）。
 */

const http = require("http");
const https = require("https");

const TOKEN_CACHE_TTL_MS = Number(process.env.PB_TOKEN_CACHE_MS || 5 * 60 * 1000);
const ROOM_CACHE_TTL_MS = Number(process.env.PB_ROOM_CACHE_MS || 60 * 1000);

/** @type {Map<string, {userId:string, exp:number}>} */
const tokenCache = new Map();
/** @type {Map<string, {room:object, exp:number}>} */
const roomMemberCache = new Map();

function fetchJson(method, url, headers, body, timeoutMs) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith("https") ? https : http;
    const req = lib.request(url, { method, headers, timeout: timeoutMs }, (res) => {
      const chunks = [];
      res.on("data", (c) => chunks.push(c));
      res.on("end", () => {
        const text = Buffer.concat(chunks).toString("utf8");
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error("http_" + res.statusCode + ":" + text.slice(0, 240)));
          return;
        }
        try {
          resolve(JSON.parse(text));
        } catch (e) {
          reject(new Error("invalid_json"));
        }
      });
    });
    req.on("error", reject);
    req.on("timeout", () => {
      req.destroy();
      reject(new Error("timeout"));
    });
    if (body) req.write(body);
    req.end();
  });
}

function relationId(field) {
  if (!field) return "";
  if (typeof field === "string") return field.trim();
  if (typeof field === "object" && field.id) return String(field.id).trim();
  return "";
}

/** 从 PB JWT payload 解码 userId（不验签；后续 GET room 仍带 token 由 PB 校验） */
function decodePbUserId(token) {
  try {
    const parts = token.split(".");
    if (parts.length < 2) return "";
    const b64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const payload = JSON.parse(Buffer.from(b64, "base64").toString("utf8"));
    return relationId(payload.id) || relationId(payload.record) || "";
  } catch {
    return "";
  }
}

/** PB 无 /records/me，用 auth-refresh 校验 token 并取用户 id（fallback） */
async function verifyPbToken(pbBaseUrl, token, timeoutMs) {
  const cached = tokenCache.get(token);
  if (cached && Date.now() < cached.exp) return cached.userId;

  const base = pbBaseUrl.replace(/\/$/, "");
  const json = await fetchJson(
    "POST",
    base + "/api/collections/users/auth-refresh",
    {
      Authorization: "Bearer " + token,
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    "{}",
    timeoutMs,
  );
  const userId = relationId(json.record) || relationId(json.model) || "";
  if (!userId) throw new Error("no_user_id");
  tokenCache.set(token, { userId, exp: Date.now() + TOKEN_CACHE_TTL_MS });
  return userId;
}

async function resolveUserId(pbBaseUrl, token, timeoutMs) {
  const cached = tokenCache.get(token);
  if (cached && Date.now() < cached.exp) return cached.userId;

  let userId = decodePbUserId(token);
  if (!userId) {
    userId = await verifyPbToken(pbBaseUrl, token, timeoutMs);
  } else {
    tokenCache.set(token, { userId, exp: Date.now() + TOKEN_CACHE_TTL_MS });
  }
  return userId;
}

async function verifyRoomMember(pbBaseUrl, token, roomId, userId, timeoutMs) {
  const cacheKey = roomId + ":" + userId;
  const cached = roomMemberCache.get(cacheKey);
  if (cached && Date.now() < cached.exp) return cached.room;

  const base = pbBaseUrl.replace(/\/$/, "");
  const room = await fetchJson(
    "GET",
    base + "/api/collections/game_rooms/records/" + encodeURIComponent(roomId),
    {
      Authorization: "Bearer " + token,
      Accept: "application/json",
    },
    null,
    timeoutMs,
  );
  const host = relationId(room.host);
  const guest = relationId(room.guest);
  if (userId !== host && userId !== guest) {
    throw new Error("not_room_member host=" + host + " guest=" + guest + " me=" + userId);
  }
  const gameType = (room.game_type || "").trim();
  if (gameType !== "draw_guess") {
    throw new Error("not_draw_guess:" + gameType);
  }
  const summary = { host, guest, status: room.status || "" };
  roomMemberCache.set(cacheKey, { room: summary, exp: Date.now() + ROOM_CACHE_TTL_MS });
  return summary;
}

async function authenticateJoin(pbBaseUrl, token, roomId, timeoutMs) {
  const userId = await resolveUserId(pbBaseUrl, token, timeoutMs);
  const room = await verifyRoomMember(pbBaseUrl, token, roomId, userId, timeoutMs);
  return { userId, room };
}

module.exports = { authenticateJoin, verifyPbToken };
