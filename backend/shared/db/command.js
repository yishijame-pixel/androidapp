/** CloudBase db.command 兼容操作符 */
const command = {
  inc: (n) => ({ __cb_op: "inc", value: Number(n) || 0 }),
  gt: (v) => ({ __cb_op: "gt", value: v }),
  gte: (v) => ({ __cb_op: "gte", value: v }),
  lt: (v) => ({ __cb_op: "lt", value: v }),
  lte: (v) => ({ __cb_op: "lte", value: v }),
  neq: (v) => ({ __cb_op: "neq", value: v }),
  in: (arr) => ({ __cb_op: "in", value: Array.isArray(arr) ? arr : [] }),
};

module.exports = command;
