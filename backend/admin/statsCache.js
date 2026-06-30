/**
 * 管理后台统计接口 TTL 缓存（PostgreSQL count/orderBy 聚合较贵）。
 * 默认 30s；卡密变更类写操作可调用 invalidateAll()。
 */

const DEFAULT_TTL_MS = Number(process.env.ADMIN_STATS_CACHE_TTL_MS || 30_000);

class TtlEntry {
  constructor(value, expiresAt) {
    this.value = value;
    this.expiresAt = expiresAt;
  }
}

class StatsCache {
  constructor(ttlMs = DEFAULT_TTL_MS) {
    this.ttlMs = ttlMs;
    this.store = new Map();
    this.inflight = new Map();
  }

  async get(key, loader) {
    const now = Date.now();
    const hit = this.store.get(key);
    if (hit && hit.expiresAt > now) return hit.value;

    if (this.inflight.has(key)) return this.inflight.get(key);

    const p = Promise.resolve()
      .then(loader)
      .then((value) => {
        this.store.set(key, new TtlEntry(value, Date.now() + this.ttlMs));
        return value;
      })
      .finally(() => {
        this.inflight.delete(key);
      });
    this.inflight.set(key, p);
    return p;
  }

  invalidate(key) {
    this.store.delete(key);
    this.inflight.delete(key);
  }

  invalidateAll() {
    this.store.clear();
    this.inflight.clear();
  }
}

const dashboardStatsCache = new StatsCache();
const chatAiProductStatsCache = new StatsCache();

module.exports = {
  dashboardStatsCache,
  chatAiProductStatsCache,
  StatsCache,
};
