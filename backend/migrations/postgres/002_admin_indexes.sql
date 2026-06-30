-- Admin 仪表盘 / 日志查询加速（ORDER BY at / registeredAt）
-- 运行: psql $DATABASE_URL -f backend/migrations/postgres/002_admin_indexes.sql

-- vip_redeem_log: 最近兑换、7 日 count、审计关联
CREATE INDEX IF NOT EXISTS idx_vip_redeem_log_at
  ON documents ((data->>'at') DESC NULLS LAST)
  WHERE collection = 'vip_redeem_log';

CREATE INDEX IF NOT EXISTS idx_vip_redeem_log_sku_status
  ON documents ((data->>'skuCode'), (data->>'status'))
  WHERE collection = 'vip_redeem_log';

-- vip_users: 用户列表按注册时间倒序
CREATE INDEX IF NOT EXISTS idx_vip_users_registered_at
  ON documents ((data->>'registeredAt') DESC NULLS LAST)
  WHERE collection = 'vip_users';

CREATE INDEX IF NOT EXISTS idx_vip_users_username
  ON documents ((data->>'username'))
  WHERE collection = 'vip_users';

-- vip_coin_logs: 积分流水
CREATE INDEX IF NOT EXISTS idx_vip_coin_logs_at
  ON documents ((data->>'at') DESC NULLS LAST)
  WHERE collection = 'vip_coin_logs';

CREATE INDEX IF NOT EXISTS idx_vip_coin_logs_username
  ON documents ((data->>'username'))
  WHERE collection = 'vip_coin_logs';

-- vip_admin_audit: 审计日志
CREATE INDEX IF NOT EXISTS idx_vip_admin_audit_at
  ON documents ((data->>'at') DESC NULLS LAST)
  WHERE collection = 'vip_admin_audit';

-- galaxy_reports: 举报列表
CREATE INDEX IF NOT EXISTS idx_galaxy_reports_at
  ON documents ((data->>'at') DESC NULLS LAST)
  WHERE collection = 'galaxy_reports';

-- vip_codes: sku + status 组合 count（仪表盘 bySku）
CREATE INDEX IF NOT EXISTS idx_vip_codes_sku_status
  ON documents ((data->>'skuCode'), (data->>'status'), (data->>'disabled'))
  WHERE collection = 'vip_codes';

COMMENT ON INDEX idx_vip_redeem_log_at IS 'Admin LOG.orderBy(at) / 7日兑换 count';
