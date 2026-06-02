const { loadEnv } = require("./_loadEnv");
loadEnv();
const tcb = require("@cloudbase/node-sdk");
const app = tcb.init({
  env: process.env.TCB_ENV_ID,
  secretId: process.env.TCB_SECRET_ID,
  secretKey: process.env.TCB_SECRET_KEY,
});
const db = app.database();
(async () => {
  for (const col of ["letter_quota", "chat_ai_quota"]) {
    const r = await db.collection(col).limit(50).get().catch(e => ({ err: e.message }));
    console.log("===", col, "===");
    console.log(JSON.stringify(r, null, 2));
  }
})();
