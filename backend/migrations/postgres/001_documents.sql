-- FunLife VIP 文档库（CloudBase 集合 → PostgreSQL JSONB）
-- 运行: psql $DATABASE_URL -f backend/migrations/postgres/001_documents.sql

CREATE TABLE IF NOT EXISTS documents (
  collection TEXT NOT NULL,
  doc_id     TEXT NOT NULL,
  data       JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (collection, doc_id)
);

CREATE INDEX IF NOT EXISTS idx_documents_collection ON documents (collection);
CREATE INDEX IF NOT EXISTS idx_documents_data_gin ON documents USING gin (data);

CREATE INDEX IF NOT EXISTS idx_vip_codes_code
  ON documents ((data->>'code')) WHERE collection = 'vip_codes';
CREATE INDEX IF NOT EXISTS idx_vip_codes_status
  ON documents ((data->>'status')) WHERE collection = 'vip_codes';
CREATE INDEX IF NOT EXISTS idx_vip_codes_device
  ON documents ((data->>'usedByDevice')) WHERE collection = 'vip_codes';
CREATE INDEX IF NOT EXISTS idx_vip_rate_limit_key
  ON documents ((data->>'key')) WHERE collection = 'vip_rate_limit';
CREATE INDEX IF NOT EXISTS idx_chat_ai_quota_device_ymd
  ON documents ((data->>'deviceId'), (data->>'ymd')) WHERE collection = 'chat_ai_quota';

COMMENT ON TABLE documents IS 'CloudBase collection 兼容存储；data 含原 _id 及全部字段';
