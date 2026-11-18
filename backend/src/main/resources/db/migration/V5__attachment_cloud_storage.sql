-- Photos can now live in cloud object storage (S3-compatible) instead of
-- inline in Postgres. For a cloud-stored photo, `storage_key` holds the object
-- key and `data` is NULL; for a database-stored photo (the default) it's the
-- reverse. Existing rows are untouched and keep working as database-stored.
ALTER TABLE work_order_attachments ALTER COLUMN data DROP NOT NULL;
ALTER TABLE work_order_attachments ADD COLUMN storage_key VARCHAR(500);
