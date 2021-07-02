CREATE TABLE IF NOT EXISTS device_token (
    user_id BIGINT NOT NULL,
    token VARCHAR NOT NULL,
    PRIMARY KEY (user_id, token)
);

ALTER TABLE device_token ADD CONSTRAINT token_user_id_fk FOREIGN KEY (user_id) REFERENCES user_profile(id);