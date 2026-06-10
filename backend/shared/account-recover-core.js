// ============================================================
// 账号恢复核心逻辑（纯函数，可单测）
// ------------------------------------------------------------
// 场景：用户清本机数据后，凭 username + passwordProof + deviceId
//       从云端 vip_users 验密并拉取可恢复资产（金币快照等）。
// ============================================================

/** 对外统一凭证错误码，防止用户名枚举 */
const CREDENTIALS_INVALID = "CREDENTIALS_INVALID";

/**
 * @param {object} input
 * @param {object|null} input.user vip_users 文档
 * @param {string} input.passwordProof 客户端 SHA-256 proof
 * @param {string} input.deviceId 当前设备指纹
 * @param {boolean} input.banned 是否被封
 * @param {string} [input.banReason]
 * @param {object|null} [input.walletSnapshot] vip_coin_snapshots 文档
 */
function evaluateAccountRecover(input) {
  const username = String(input.username || "").trim();
  const passwordProof = String(input.passwordProof || "").trim();
  const deviceId = String(input.deviceId || "").trim();

  if (!username || !deviceId) {
    return { ok: false, code: "INVALID", msg: "请求参数无效" };
  }
  if (!passwordProof || passwordProof.length < 32) {
    return { ok: false, code: "PROOF_REQUIRED", msg: "请升级 App 后重新登录" };
  }

  const user = input.user;
  if (!user) {
    return {
      ok: false,
      code: CREDENTIALS_INVALID,
      msg: "用户名或密码错误",
    };
  }

  if (input.banned) {
    return {
      ok: false,
      code: "BANNED",
      msg: input.banReason || "该账号已被封禁",
    };
  }

  if (user.passwordProof) {
    if (user.passwordProof !== passwordProof) {
      return {
        ok: false,
        code: CREDENTIALS_INVALID,
        msg: "用户名或密码错误",
        audit: { flag: "WRONG_PASSWORD" },
      };
    }
  } else if (!user.deviceId || user.deviceId !== deviceId) {
    // 无 passwordProof 的老账号：仅允许原设备恢复并补录 proof
    return {
      ok: false,
      code: CREDENTIALS_INVALID,
      msg: "用户名或密码错误",
      audit: { flag: "LEGACY_DEVICE_MISMATCH" },
    };
  }

  if (user.deviceId && user.deviceId !== deviceId) {
    return {
      ok: false,
      code: "DEVICE_CONFLICT",
      msg: "该账号已在其他设备注册，请使用 VIP 迁移或在原设备恢复",
      audit: { flag: "DEVICE_CONFLICT" },
    };
  }

  const wallet = normalizeWallet(input.walletSnapshot);
  return {
    ok: true,
    nickname: String(user.nickname || username).slice(0, 64),
    wallet,
    patchUser: {
      nickname: String(user.nickname || username).slice(0, 64),
      lastSeenAt: true,
      recoveredAt: true,
      recoverCount: (parseInt(user.recoverCount, 10) || 0) + 1,
      ...(user.passwordProof ? {} : { passwordProof }),
    },
    audit: { flag: "RECOVER_OK" },
  };
}

function normalizeWallet(snapshot) {
  if (!snapshot) {
    return {
      balance: 0,
      totalEarned: 0,
      totalSpent: 0,
      pointsBalance: 0,
      hasSnapshot: false,
    };
  }
  const balance = Math.max(0, parseInt(snapshot.balance, 10) || 0);
  const totalEarned = Math.max(0, parseInt(snapshot.totalEarned, 10) || 0);
  const totalSpent = Math.max(0, parseInt(snapshot.totalSpent, 10) || 0);
  const pointsBalance = Math.max(0, parseInt(snapshot.pointsBalance, 10) || 0);
  return {
    balance,
    totalEarned,
    totalSpent,
    pointsBalance,
    hasSnapshot: true,
  };
}

module.exports = {
  evaluateAccountRecover,
  CREDENTIALS_INVALID,
  normalizeWallet,
};
