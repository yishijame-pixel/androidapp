/**
 * 校验 PocketBase JWT，并确认用户属于房间（host / guest）。
 */

const http = require("http");
const https = require("https");

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

/** PB 无 /records/me，用 auth-refresh 校验 token 并取用户 id */
async function verifyPbToken(pbBaseUrl, token, timeoutMs) {
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
  return userId;
}

async function verifyRoomMember(pbBaseUrl, token, roomId, userId, timeoutMs) {
  const base = pbBaseUrl.replace(/\/$/, "");
  const room = await fetchJson(
    "GET",
    base + "/api/collections/game_rooms/records/" + encodeURIComponent(roomId),
    { Authorization: "Bearer " + token, Accept: "application/json" },
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
  return { host, guest, status: room.status || "" };
}

async function authenticateJoin(pbBaseUrl, token, roomId, timeoutMs) {
  const userId = await verifyPbToken(pbBaseUrl, token, timeoutMs);
  const room = await verifyRoomMember(pbBaseUrl, token, roomId, userId, timeoutMs);
  return { userId, room };
}

module.exports = { authenticateJoin, verifyPbToken };
