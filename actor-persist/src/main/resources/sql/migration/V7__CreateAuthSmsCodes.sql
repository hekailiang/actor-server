CREATE TABLE auth_sms_codes (
       phone_number bigint NOT NULL,
       sms_hash varchar(64) NOT NULL,
       sms_code varchar(8) NOT NULL,
       PRIMARY KEY (phone_number)
);
