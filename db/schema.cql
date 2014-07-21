CREATE KEYSPACE IF NOT EXISTS secret WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE secret;

CREATE TABLE auth_ids (
  authid bigint,
  userid int,
  PRIMARY KEY ((authid))
);

CREATE TABLE auth_sms_codes (
  phone_number bigint,
  sms_code text,
  sms_hash text,
  PRIMARY KEY ((phone_number))
);

CREATE TABLE common_updates (
  pubkey_hash bigint,
  uuid timeuuid,
  aes_key_hex text,
  dest_uid int,
  key_hash bigint,
  message text,
  mid int,
  random_id bigint,
  sender_uid int,
  seq int,
  update_id int,
  use_aes_key boolean,
  user_ids set<int>,
  PRIMARY KEY ((pubkey_hash), uuid)
);

CREATE TABLE phones (
  number bigint,
  user_id int,
  PRIMARY KEY ((number))
);

CREATE TABLE sessions (
  authid bigint,
  sessionid bigint,
  PRIMARY KEY ((authid, sessionid))
);

CREATE TABLE users (
  id int,
  public_key_hash bigint,
  access_salt text static,
  first_name text static,
  key_hashes set<bigint> static,
  last_name text static,
  public_key varint,
  sex int static,
  PRIMARY KEY ((id), public_key_hash)
);
