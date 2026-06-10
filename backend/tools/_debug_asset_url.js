// 调试 CloudBase getTempFileURL 格式
require("./_loadEnv").loadEnv();
const tcb = require("@cloudbase/node-sdk");
const app = tcb.init({
  env: process.env.TCB_ENV_ID,
  secretId: process.env.TCB_SECRET_ID,
  secretKey: process.env.TCB_SECRET_KEY,
});

const ENV = process.env.TCB_ENV_ID;
const path = "yishi-assetss/v1/bundles/dibu.zip";

async function main() {
  const candidates = [
    `cloud://${ENV}.${path}`,
    `cloud://${ENV}/${path}`,
    path,
  ];
  for (const fileID of candidates) {
    try {
      const r = await app.getTempFileURL({
        fileList: [{ fileID, maxAge: 3600 }],
      });
      console.log("try:", fileID);
      console.log(JSON.stringify(r.fileList?.[0], null, 2));
    } catch (e) {
      console.log("fail:", fileID, e.message);
    }
  }
}

main();
