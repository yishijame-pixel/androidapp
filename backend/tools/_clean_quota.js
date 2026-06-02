const { loadEnv } = require("./_loadEnv");
loadEnv();
const tcb = require("@cloudbase/node-sdk");
const app = tcb.init({
  env: process.env.TCB_ENV_ID,
  secretId: process.env.TCB_SECRET_ID,
  secretKey: process.env.TCB_SECRET_KEY,
});
const db = app.database();
const _ = db.command;
(async () => {
  for (const col of ["letter_quota", "chat_ai_quota"]) {
    const r = await db.collection(col).where({ _id: _.exists(true) }).limit(1000).remove()
      .catch(e => ({ err: e.message }));
    console.log(col, r);
  }
})();
