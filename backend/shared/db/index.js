const installShim = require("./install-shim");
const { createDatabase, getPool, runMigration, closePool } = require("./postgres");

module.exports = {
  installShim,
  createDatabase,
  getPool,
  runMigration,
  closePool,
};
