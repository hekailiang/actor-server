CREATE TABLE user_contacts (
       owner_user_id int NOT NULL,
       contact_user_id int NOT NULL,
       phone_number bigint NOT NULL,
       name varchar(255) NOT NULL,
       access_salt varchar(255) NOT NULL,
       is_deleted boolean NOT NULL default false,
       PRIMARY KEY (owner_user_id, contact_user_id)
);

CREATE INDEX on user_contacts(owner_user_id, is_deleted);
