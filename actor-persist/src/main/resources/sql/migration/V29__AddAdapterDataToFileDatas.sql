ALTER TABLE file_datas ADD COLUMN adapter_data bytea;
UPDATE file_datas SET adapter_data = CAST(CAST(id AS text) AS bytea);
ALTER TABLE file_datas ALTER COLUMN adapter_data SET NOT NULL;
