"use strict";

/** 你画我猜 WS 二进制帧 v2 — 与 Android DrawWsBinaryCodec 对齐（v2 含 color） */

const MAGIC = Buffer.from([0xfd, 0x47]);
const VERSION_V1 = 1;
const VERSION_V2 = 2;
const TYPE_CHUNK = 1;
const TYPE_END = 2;
const TYPE_CLEAR = 3;
const TYPE_PING = 4;

function isBinary(data) {
  return Buffer.isBuffer(data) && data.length >= 3 && data[0] === 0xfd && data[1] === 0x47;
}

function readF32BE(buf, off) {
  return buf.readFloatBE(off);
}

function normalizeColor(raw) {
  const c = String(raw || "").trim();
  if (!c) return "#222222";
  return c.startsWith("#") ? c : "#" + c;
}

function decodeStroke(data, type) {
  const version = data[2];
  let off = 4;
  const sidLen = data[off];
  off += 1;
  const strokeId = data.toString("utf8", off, off + sidLen);
  off += sidLen;
  const chunk = data.readUInt16BE(off);
  off += 2;
  const round = data[off];
  off += 1;
  const width = readF32BE(data, off);
  off += 4;
  let seq = null;
  if (type === TYPE_END) {
    seq = data.readUInt16BE(off);
    off += 2;
  }
  const pointCount = data.readUInt16BE(off);
  off += 2;
  const points = [];
  for (let i = 0; i < pointCount; i++) {
    points.push([readF32BE(data, off), readF32BE(data, off + 4)]);
    off += 8;
  }
  let color = "#222222";
  if (version >= VERSION_V2 && off < data.length) {
    const colorLen = data[off];
    off += 1;
    if (colorLen > 0 && off + colorLen <= data.length) {
      color = normalizeColor(data.toString("utf8", off, off + colorLen));
      off += colorLen;
    }
  }
  let from = "";
  if (off < data.length) {
    const fromLen = data[off];
    off += 1;
    if (fromLen > 0 && off + fromLen <= data.length) {
      from = data.toString("utf8", off, off + fromLen);
    }
  }
  const wireType = type === TYPE_END ? "stroke_end" : "stroke_chunk";
  const out = {
    t: wireType,
    v: version,
    strokeId,
    chunk,
    round,
    width,
    color,
    points,
  };
  if (seq != null) out.seq = seq;
  if (from) out.from = from;
  return out;
}

function decodeMessage(data) {
  if (!isBinary(data)) return null;
  const type = data[3];
  if (type === TYPE_CLEAR) {
    const round = data.length >= 5 ? data[4] : 1;
    return { t: "clear", v: data[2], round };
  }
  if (type === TYPE_PING) return { t: "ping", v: data[2] };
  if (type === TYPE_CHUNK || type === TYPE_END) return decodeStroke(data, type);
  return null;
}

function appendRelayFrom(payload, fromPbId) {
  const from = Buffer.from(fromPbId, "utf8");
  return Buffer.concat([payload, Buffer.from([from.length]), from]);
}

function encodeClear(round) {
  return Buffer.from([0xfd, 0x47, VERSION_V2, TYPE_CLEAR, round || 1]);
}

function encodeForRelay(msg, fromPbId) {
  if (!msg || !msg.t) return null;
  if (msg.t === "clear") return encodeClear(msg.round || 1);
  if (msg.t === "stroke_chunk" || msg.t === "stroke_end") {
    const sid = Buffer.from(String(msg.strokeId || ""), "utf8");
    const pts = msg.points || [];
    const color = Buffer.from(normalizeColor(msg.color), "utf8");
    const type = msg.t === "stroke_end" ? TYPE_END : TYPE_CHUNK;
    const extraSeq = type === TYPE_END ? 2 : 0;
    const size = 4 + 1 + sid.length + 2 + 1 + 4 + extraSeq + 2 + pts.length * 8 + 1 + color.length;
    const buf = Buffer.alloc(size);
    let off = 0;
    MAGIC.copy(buf, off);
    off += 2;
    buf[off++] = VERSION_V2;
    buf[off++] = type;
    buf[off++] = sid.length;
    sid.copy(buf, off);
    off += sid.length;
    buf.writeUInt16BE(msg.chunk || 0, off);
    off += 2;
    buf[off++] = msg.round || 1;
    buf.writeFloatBE(msg.width || 4, off);
    off += 4;
    if (type === TYPE_END) {
      buf.writeUInt16BE(msg.seq || 0, off);
      off += 2;
    }
    buf.writeUInt16BE(pts.length, off);
    off += 2;
    for (const p of pts) {
      buf.writeFloatBE(p[0] || 0, off);
      buf.writeFloatBE(p[1] || 0, off + 4);
      off += 8;
    }
    buf[off++] = color.length;
    color.copy(buf, off);
    off += color.length;
    return appendRelayFrom(buf, fromPbId);
  }
  return null;
}

module.exports = {
  isBinary,
  decodeMessage,
  appendRelayFrom,
  encodeForRelay,
  encodeClear,
  TYPE_CHUNK,
  TYPE_END,
  TYPE_CLEAR,
  TYPE_PING,
};
