ALTER TABLE file_blocks ADD COLUMN adapter_data bytea;
UPDATE file_blocks SET adapter_data = CAST('' as bytea);
ALTER TABLE file_blocks ALTER COLUMN adapter_data SET NOT NULL;
