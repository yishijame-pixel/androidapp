/**
 * PocketBase 社交 E2E 测试公共库（与 Android SocialSecureStore / PocketBaseApiClient 对齐）
 */
"use strict";

const crypto = require("crypto");

function syntheticIdentity(localUserId, funlifeUsername) {
  const safe = funlifeUsername.toLowerCase().replace(/[^a-z0-9_]/g, "");
  return `u${localUserId}_${safe}@funlife.social.invalid`;
}

function randomPassword() {
  return crypto.randomBytes(24).toString("base64url");
}

function computePairKey(pbIdA, pbIdB) {
  const a = pbIdA.trim();
  const b = pbIdB.trim();
  if (!a || !b || a === b) throw new Error("invalid pair");
  return a < b ? `${a}|${b}` : `${b}|${a}`;
}

class PbSocialClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
    this.apiBase = `${this.baseUrl}/api`;
  }

  async request(method, path, { token, body, expectOk = true } = {}) {
    const headers = { "Content-Type": "application/json" };
    if (token) headers.Authorization = `Bearer ${token}`;
    const res = await fetch(`${this.apiBase}${path}`, {
      method,
      headers,
      body: body != null ? JSON.stringify(body) : undefined,
    });
    const text = await res.text();
    let json = null;
    try {
      json = text ? JSON.parse(text) : null;
    } catch {
      json = { _raw: text };
    }
    if (expectOk && !res.ok) {
      const msg = json?.message || json?.data || text || res.status;
      throw new Error(`HTTP ${res.status} ${method} ${path}: ${msg}`);
    }
    return { status: res.status, json, ok: res.ok };
  }

  async health() {
    const res = await fetch(`${this.apiBase}/health`);
    return res.ok;
  }

  async adminAuth(email, password) {
    const { json } = await this.request("POST", "/collections/_superusers/auth-with-password", {
      body: { identity: email, password },
    });
    return json.token;
  }

  async registerUser({ localUserId, funlifeUsername, displayName, password }) {
    const identity = syntheticIdentity(localUserId, funlifeUsername);
    const pwd = password || randomPassword();
    await this.request("POST", "/collections/users/records", {
      body: {
        email: identity,
        password: pwd,
        passwordConfirm: pwd,
        name: displayName,
        funlife_local_id: localUserId,
        funlife_username: funlifeUsername,
        online: false,
      },
    });
    return this.authWithPassword(identity, pwd);
  }

  async authWithPassword(identity, password) {
    const { json } = await this.request("POST", "/collections/users/auth-with-password", {
      body: { identity, password },
    });
    return {
      token: json.token,
      recordId: json.record.id,
      record: json.record,
      identity,
      password,
    };
  }

  async authRefresh(token) {
    const { json } = await this.request("POST", "/collections/users/auth-refresh", {
      token,
      body: {},
    });
    return { token: json.token, recordId: json.record.id };
  }

  async findUserByUsername(token, username) {
    const filter = encodeURIComponent(`funlife_username = '${username.replace(/'/g, "\\'")}'`);
    const { json } = await this.request(
      "GET",
      `/collections/users/records?filter=${filter}&perPage=1`,
      { token },
    );
    return json.items?.[0] || null;
  }

  async getUserById(token, userId) {
    const { json } = await this.request("GET", `/collections/users/records/${userId}`, { token });
    return json;
  }

  async createFriendRequest(token, requesterId, addresseeId) {
    const { json } = await this.request("POST", "/collections/friendships/records", {
      token,
      body: { requester: requesterId, addressee: addresseeId, status: "pending" },
    });
    const expand = encodeURIComponent("requester,addressee");
    const got = await this.request(
      "GET",
      `/collections/friendships/records/${json.id}?expand=${expand}`,
      { token },
    );
    return got.json;
  }

  async listPendingIncoming(token, myPbId) {
    const filter = encodeURIComponent(`addressee = '${myPbId}' && status = 'pending'`);
    const expand = encodeURIComponent("requester");
    const { json } = await this.request(
      "GET",
      `/collections/friendships/records?filter=${filter}&expand=${expand}&perPage=50`,
      { token },
    );
    return json.items || [];
  }

  async listFriendships(token, myPbId) {
    const filter = encodeURIComponent(
      `(requester = '${myPbId}' || addressee = '${myPbId}') && status != 'blocked'`,
    );
    const expand = encodeURIComponent("requester,addressee");
    const { json } = await this.request(
      "GET",
      `/collections/friendships/records?filter=${filter}&expand=${expand}&perPage=100`,
      { token },
    );
    return json.items || [];
  }

  async acceptFriendship(token, friendshipId) {
    const { json } = await this.request("PATCH", `/collections/friendships/records/${friendshipId}`, {
      token,
      body: { status: "accepted" },
    });
    return json;
  }

  async deleteFriendship(token, friendshipId) {
    await this.request("DELETE", `/collections/friendships/records/${friendshipId}`, { token });
  }

  async findConversationByPairKey(token, pairKey) {
    const filter = encodeURIComponent(`pair_key = '${pairKey.replace(/'/g, "\\'")}'`);
    const { json } = await this.request(
      "GET",
      `/collections/conversations/records?filter=${filter}&perPage=1`,
      { token },
    );
    return json.items?.[0] || null;
  }

  async findOrCreateConversation(token, myPbId, peerPbId) {
    const pairKey = computePairKey(myPbId, peerPbId);
    const existing = await this.findConversationByPairKey(token, pairKey);
    if (existing) return existing;
    const [memberA, memberB] = pairKey.split("|");
    const { json } = await this.request("POST", "/collections/conversations/records", {
      token,
      body: {
        member_a: memberA,
        member_b: memberB,
        pair_key: pairKey,
        last_preview: "",
        last_message_at: 0,
      },
    });
    return json;
  }

  async sendMessage(token, conversationId, senderPbId, memberAId, memberBId, body) {
    const { json } = await this.request("POST", "/collections/messages/records", {
      token,
      body: {
        conversation: conversationId,
        member_a: memberAId,
        member_b: memberBId,
        sender: senderPbId,
        body,
      },
    });
    return json;
  }

  async listMessages(token, conversationId) {
    const filter = encodeURIComponent(`conversation = '${conversationId.replace(/'/g, "\\'")}'`);
    const { json } = await this.request(
      "GET",
      `/collections/messages/records?filter=${filter}&perPage=50&sort=created`,
      { token },
    );
    return json.items || [];
  }

  async deleteRecordAdmin(adminToken, collection, recordId) {
    await this.request("DELETE", `/collections/${collection}/records/${recordId}`, {
      token: adminToken,
    });
  }
}

class TestReporter {
  constructor() {
    this.pass = 0;
    this.fail = 0;
    this.results = [];
  }

  expect(name, cond, detail) {
    if (cond) {
      console.log(`  ✅ ${name}`);
      this.pass++;
      this.results.push({ name, pass: true });
    } else {
      console.log(`  ❌ ${name}`);
      if (detail) console.log(`     → ${typeof detail === "string" ? detail : JSON.stringify(detail)}`);
      this.fail++;
      this.results.push({ name, pass: false, detail });
    }
  }

  async expectThrows(name, fn, statusHint) {
    try {
      await fn();
      this.expect(name, false, "expected failure but succeeded");
    } catch (e) {
      const msg = String(e.message);
      const hints = Array.isArray(statusHint) ? statusHint : [statusHint];
      const ok =
        hints.filter(Boolean).length === 0 ||
        hints.some((h) => msg.includes(String(h)));
      this.expect(name, ok, ok ? undefined : e.message);
    }
  }

  summary() {
    return { pass: this.pass, fail: this.fail, total: this.pass + this.fail, results: this.results };
  }
}

module.exports = { PbSocialClient, TestReporter, syntheticIdentity, randomPassword, computePairKey };
