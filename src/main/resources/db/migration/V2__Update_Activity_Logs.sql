-- V2: Update activity_logs table with more structured fields
ALTER TABLE activity_logs ADD COLUMN method VARCHAR(10);
ALTER TABLE activity_logs ADD COLUMN uri VARCHAR(255);
ALTER TABLE activity_logs ADD COLUMN status INTEGER;
ALTER TABLE activity_logs ADD COLUMN ip_address VARCHAR(45);

-- Update existing records with dummy/safe values for NOT NULL constraints
UPDATE activity_logs SET method = 'UNKNOWN' WHERE method IS NULL;
UPDATE activity_logs SET uri = 'UNKNOWN' WHERE uri IS NULL;

-- Apply NOT NULL constraints after seeding
ALTER TABLE activity_logs ALTER COLUMN method SET NOT NULL;
ALTER TABLE activity_logs ALTER COLUMN uri SET NOT NULL;

CREATE INDEX idx_activity_logs_username ON activity_logs(username);
CREATE INDEX idx_activity_logs_timestamp ON activity_logs(timestamp DESC);
