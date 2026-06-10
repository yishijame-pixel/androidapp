// ============================================================
// 账号恢复核心逻辑 · 单元测试
// 运行: node backend/functions/account_recover/account_recover.test.js
// ============================================================

const crypto = require("crypto");
const {
  evaluateAccountRecover,
  CREDENTIALS_INVALID,
  normalizeWallet,
} = require("./account-recover-core");

function proof(username, password) {
  return crypto.createHash("sha256").update(`FunLifeAuth|${username.trim()}|${password}`, "utf8").digest("hex");
}

let pass = 0;
let fail = 0;
function expect(name, cond, detail) {
  if (cond) {
    console.log(`  ✅ ${name}`);
    pass++;
  } else {
    console.log(`  ❌ ${name}`);
    if (detail !== undefined) console.log(`     → ${JSON.stringify(detail)}`);
    fail++;
  }
}

const USER = "recover_test_user";
const PASS = "RecoverPwd123!";
const DEV = "device_recover_abc";
const P = proof(USER, PASS);

console.log("\n=== account-recover-core unit tests ===\n");

expect(
  "missing user → CREDENTIALS_INVALID",
  evaluateAccountRecover({
    username: USER,
    passwordProof: P,
    deviceId: DEV,
    user: null,
    banned: false,
  }).code === CREDENTIALS_INVALID,
);

expect(
  "wrong password → CREDENTIALS_INVALID",
  evaluateAccountRecover({
    username: USER,
    passwordProof: proof(USER, "wrong"),
    deviceId: DEV,
    user: { username: USER, passwordProof: P, deviceId: DEV, nickname: "Nick" },
    banned: false,
  }).code === CREDENTIALS_INVALID,
);

expect(
  "device mismatch → DEVICE_CONFLICT",
  evaluateAccountRecover({
    username: USER,
    passwordProof: P,
    deviceId: "other_device",
    user: { username: USER, passwordProof: P, deviceId: DEV, nickname: "Nick" },
    banned: false,
  }).code === "DEVICE_CONFLICT",
);

expect(
  "banned user → BANNED",
  evaluateAccountRecover({
    username: USER,
    passwordProof: P,
    deviceId: DEV,
    user: { username: USER, passwordProof: P, deviceId: DEV },
    banned: true,
    banReason: "测试封禁",
  }).code === "BANNED",
);

expect(
  "legacy user same device ok + backfill proof",
  (() => {
    const r = evaluateAccountRecover({
      username: USER,
      passwordProof: P,
      deviceId: DEV,
      user: { username: USER, deviceId: DEV, nickname: "Legacy" },
      banned: false,
      walletSnapshot: { balance: 100, totalEarned: 200, totalSpent: 100, pointsBalance: 5 },
    });
    return r.ok && r.patchUser.passwordProof === P && r.wallet.balance === 100;
  })(),
);

expect(
  "legacy user wrong device rejected",
  evaluateAccountRecover({
    username: USER,
    passwordProof: P,
    deviceId: "wrong_dev",
    user: { username: USER, deviceId: DEV },
    banned: false,
  }).code === CREDENTIALS_INVALID,
);

expect(
  "happy path returns wallet + nickname",
  (() => {
    const r = evaluateAccountRecover({
      username: USER,
      passwordProof: P,
      deviceId: DEV,
      user: { username: USER, passwordProof: P, deviceId: DEV, nickname: "OK", recoverCount: 2 },
      banned: false,
      walletSnapshot: { balance: 50, pointsBalance: 10 },
    });
    return r.ok && r.nickname === "OK" && r.wallet.pointsBalance === 10 && r.patchUser.recoverCount === 3;
  })(),
);

expect(
  "normalizeWallet empty snapshot",
  normalizeWallet(null).hasSnapshot === false && normalizeWallet(null).balance === 0,
);

expect(
  "missing proof rejected",
  evaluateAccountRecover({
    username: USER,
    passwordProof: "",
    deviceId: DEV,
    user: { username: USER, passwordProof: P, deviceId: DEV },
    banned: false,
  }).code === "PROOF_REQUIRED",
);

console.log(`\n结果: ${pass} passed, ${fail} failed\n`);
if (fail > 0) process.exit(1);
