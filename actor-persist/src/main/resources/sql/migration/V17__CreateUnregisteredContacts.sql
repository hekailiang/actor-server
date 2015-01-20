CREATE TABLE unregistered_contacts (
       phone_number bigint,
       owner_user_id int,
       PRIMARY KEY (phone_number, owner_user_id)
);
