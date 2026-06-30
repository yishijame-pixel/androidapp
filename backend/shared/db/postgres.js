/**
 * PostgreSQL 实现的 CloudBase database() 兼容层。
 * 单表 documents(collection, doc_id, data JSONB) 存储全部集合。
 */
const { Pool } = require("pg");
const crypto = require("crypto");
const command = require("./command");

let pool = null;

function getPool() {
  if (!pool) {
    const url = process.env.DATABASE_URL;
    if (!url) throw new Error("DATABASE_URL 未配置");
    pool = new Pool({ connectionString: url, max: 20 });
  }
  return pool;
}

function isOp(v) {
  return v && typeof v === "object" && v.__cb_op;
}

function jsonValue(v) {
  if (v == null) return null;
  if (v instanceof Date) return v.toISOString();
  if (isOp(v)) return null;
  if (typeof v === "object" && v.$date) return new Date(v.$date).toISOString();
  return v;
}

function buildWhere(filter, params, collection) {
  const parts = ["collection = $" + params.push(collection)];
  if (!filter || typeof filter !== "object") return parts.join(" AND ");

  for (const [key, val] of Object.entries(filter)) {
    if (isOp(val)) {
      const op = val.__cb_op;
      const col = `data->>'${key.replace(/'/g, "")}'`;
      if (op === "in") {
        const arr = val.value || [];
        if (!arr.length) parts.push("FALSE");
        else {
          const placeholders = arr.map((x) => "$" + params.push(String(x)) + "::text").join(",");
          parts.push(`${col} IN (${placeholders})`);
        }
      } else {
        const p = "$" + params.push(jsonValue(val.value));
        const pt = p + "::text";
        if (op === "gt") parts.push(`${col} > ${pt}`);
        else if (op === "gte") parts.push(`${col} >= ${pt}`);
        else if (op === "lt") parts.push(`${col} < ${pt}`);
        else if (op === "lte") parts.push(`${col} <= ${pt}`);
        else if (op === "neq") parts.push(`(${col} IS DISTINCT FROM ${pt})`);
      }
    } else if (val === true || val === false) {
      parts.push(`(data->>'${key}')::boolean = $${params.push(val)}`);
    } else if (typeof val === "number") {
      parts.push(`(data->>'${key}')::numeric = $${params.push(val)}`);
    } else {
      parts.push(`data->>'${key.replace(/'/g, "")}' = $${params.push(String(val))}::text`);
    }
  }
  return parts.join(" AND ");
}

function rowToDoc(row) {
  if (!row) return null;
  const doc = { ...row.data, _id: row.data._id || row.doc_id };
  return doc;
}

function applyPatchToData(data, patch) {
  const out = { ...data };
  for (const [k, v] of Object.entries(patch)) {
    if (isOp(v) && v.__cb_op === "inc") {
      const cur = Number(out[k] || 0);
      out[k] = cur + v.value;
    } else if (v instanceof Date) {
      out[k] = v.toISOString();
    } else {
      out[k] = v;
    }
  }
  return out;
}

class DocRef {
  constructor(col, id) {
    this.col = col;
    this.id = String(id);
  }

  async get() {
    const r = await getPool().query(
      "SELECT doc_id, data FROM documents WHERE collection = $1 AND doc_id = $2",
      [this.col.name, this.id],
    );
    const doc = rowToDoc(r.rows[0]);
    return { data: doc ? [doc] : [] };
  }

  async set(doc) {
    const payload = { ...doc };
    if (!payload._id) payload._id = this.id;
    await getPool().query(
      `INSERT INTO documents (collection, doc_id, data, updated_at)
       VALUES ($1, $2, $3::jsonb, now())
       ON CONFLICT (collection, doc_id) DO UPDATE SET data = EXCLUDED.data, updated_at = now()`,
      [this.col.name, this.id, JSON.stringify(payload)],
    );
    return { ok: true };
  }

  async update(patch) {
    const cur = await this.get();
    const existing = cur.data[0] || { _id: this.id };
    const merged = applyPatchToData(existing, patch);
    await this.set(merged);
    return { updated: 1 };
  }

  async remove() {
    const r = await getPool().query(
      "DELETE FROM documents WHERE collection = $1 AND doc_id = $2",
      [this.col.name, this.id],
    );
    return { deleted: r.rowCount || 0 };
  }
}

class Query {
  constructor(col, filter = {}, opts = {}) {
    this.col = col;
    this.filter = filter;
    this.limitVal = opts.limit ?? null;
    this.skipVal = opts.skip ?? 0;
    this.order = opts.order ?? null;
  }

  where(f) {
    return new Query(this.col, f, {
      limit: this.limitVal,
      skip: this.skipVal,
      order: this.order,
    });
  }

  limit(n) {
    return new Query(this.col, this.filter, {
      limit: n,
      skip: this.skipVal,
      order: this.order,
    });
  }

  skip(n) {
    return new Query(this.col, this.filter, {
      limit: this.limitVal,
      skip: n,
      order: this.order,
    });
  }

  orderBy(field, dir = "asc") {
    return new Query(this.col, this.filter, {
      limit: this.limitVal,
      skip: this.skipVal,
      order: { field, dir: String(dir).toLowerCase() === "desc" ? "DESC" : "ASC" },
    });
  }

  async get() {
    const params = [];
    const where = buildWhere(this.filter, params, this.col.name);
    let sql = `SELECT doc_id, data FROM documents WHERE ${where}`;
    if (this.order && this.order.field) {
      const f = this.order.field.replace(/'/g, "");
      sql += ` ORDER BY data->>'${f}' ${this.order.dir} NULLS LAST`;
    }
    if (this.skipVal) sql += ` OFFSET ${Number(this.skipVal)}`;
    if (this.limitVal != null) sql += ` LIMIT ${Number(this.limitVal)}`;
    const r = await getPool().query(sql, params);
    return { data: r.rows.map(rowToDoc) };
  }

  async update(patch) {
    const params = [];
    const where = buildWhere(this.filter, params, this.col.name);
    const sel = await getPool().query(
      `SELECT doc_id, data FROM documents WHERE ${where}`,
      params,
    );
    if (!sel.rows.length) return { updated: 0 };
    let updated = 0;
    for (const row of sel.rows) {
      const merged = applyPatchToData(row.data, patch);
      await getPool().query(
        `UPDATE documents SET data = $1::jsonb, updated_at = now()
         WHERE collection = $2 AND doc_id = $3`,
        [JSON.stringify(merged), this.col.name, row.doc_id],
      );
      updated++;
    }
    return { updated };
  }

  async remove() {
    const params = [];
    const where = buildWhere(this.filter, params, this.col.name);
    const r = await getPool().query(
      `DELETE FROM documents WHERE ${where}`,
      params,
    );
    return { deleted: r.rowCount || 0 };
  }

  async count() {
    const params = [];
    const where = buildWhere(this.filter, params, this.col.name);
    const r = await getPool().query(
      `SELECT COUNT(*)::int AS total FROM documents WHERE ${where}`,
      params,
    );
    return { total: r.rows[0]?.total || 0 };
  }
}

class Collection {
  constructor(name) {
    this.name = name;
  }

  doc(id) {
    return new DocRef(this, id);
  }

  where(filter) {
    return new Query(this, filter);
  }

  limit(n) {
    return new Query(this, {}, { limit: n });
  }

  skip(n) {
    return new Query(this, {}, { skip: n });
  }

  orderBy(field, dir) {
    return new Query(this, {}, { order: { field, dir } });
  }

  async add(payload) {
    const doc = payload && payload.data ? { ...payload.data } : { ...payload };
    const id = doc._id || doc.code || doc.key || crypto.randomBytes(12).toString("hex");
    doc._id = id;
    await new DocRef(this, id).set(doc);
    return { id, _id: id };
  }

  async count() {
    return new Query(this, {}).count();
  }
}

function createDatabase() {
  return {
    collection: (name) => new Collection(name),
    command,
    serverDate: () => new Date(),
  };
}

async function runMigration(sqlPath) {
  const fs = require("fs");
  const sql = fs.readFileSync(sqlPath, "utf-8");
  await getPool().query(sql);
}

async function closePool() {
  if (pool) {
    await pool.end();
    pool = null;
  }
}

module.exports = {
  createDatabase,
  getPool,
  runMigration,
  closePool,
  command,
};
