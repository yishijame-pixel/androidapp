// ⚠️ 自动生成，请勿手改。源文件: backend/shared/sku.js
// 修改方式：编辑 shared/sku.js 后运行 `node sync-sku.js`
// SKU 配置：云函数 / 卡密生成脚本 / 后续客户端都从这里读
// 修改时云函数和工具脚本都要重新部署/运行
//
// vipLevel 对应客户端 VipLevel 枚举：
//   1 = 普通 VIP（NORMAL_VIP）
//   2 = 年费 VIP（YEARLY_VIP）
//   3 = 终身 VIP（LIFETIME_VIP）

module.exports = {
  VIP_NORMAL: {
    type: "vip",
    name: "月卡 VIP",
    price: 39.9,
    vipLevel: 1,
    durationDays: 30, // 30 天月卡
    bonusCoins: 50,
  },
  VIP_YEAR: {
    type: "vip",
    name: "年卡 VIP",
    price: 99,
    vipLevel: 2,
    durationDays: 365,
    bonusCoins: 300,
  },
  VIP_LIFETIME: {
    type: "vip",
    name: "终身 VIP",
    price: 399,
    vipLevel: 3,
    durationDays: -1, // -1 = 永久
    bonusCoins: 1000,
  },
  // 注册阶段使用的内测邀请码（不开 VIP，仅放行注册）
  BETA_INVITE: {
    type: "beta",
    name: "内测邀请码",
    price: 9.9,
  },
};
