CREATE DATABASE IF NOT EXISTS dosomething_db;

USE dosomething_db;

CREATE TABLE IF NOT EXISTS user (
    facebook_id VARCHAR(20) NOT NULL PRIMARY KEY,
    name VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS friend (
    user_id VARCHAR(20) NOT NULL,
    friend_id VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, friend_id)
);

CREATE SEQUENCE event_need_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS event_need (
    id INT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR event_need_sequence),
    user_id VARCHAR(20) NOT NULL,
    need TEXT,
    start_date DATE,
    end_date DATE,
    date_scope VARCHAR(20),
    description TEXT,
    is_confirmed BOOL
);

CREATE TABLE IF NOT EXISTS event_participants (
    event_need_id INT NOT NULL,
    user_id VARCHAR(20) NOT NULL
--    code_name VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS event_user_reveal (
    event_need_id INT NOT NULL,
    revealer_id VARCHAR(20) NOT NULL,
    revealed_to_id VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS join_request (
    event_need_id INT NOT NULL,
    joiner_id VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS reveal_request (
    event_need_id INT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    requester_id VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS reveal_notification (
    event_need_id INT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    revealer_id VARCHAR(20) NOT NULL
);

ALTER TABLE friend ADD CONSTRAINT friend_user_id_fk FOREIGN KEY (user_id) REFERENCES user(facebook_id);
ALTER TABLE friend ADD CONSTRAINT friend_friend_id_fk FOREIGN KEY (friend_id) REFERENCES user(facebook_id);

ALTER TABLE event_need ADD CONSTRAINT event_need_user_fk FOREIGN KEY (user_id) REFERENCES user(facebook_id);

ALTER TABLE event_participants ADD CONSTRAINT event_participants_need_fk FOREIGN KEY (event_need_id) REFERENCES event_need(id);
ALTER TABLE event_participants ADD CONSTRAINT event_participants_user_fk FOREIGN KEY (user_id) REFERENCES user(facebook_id);

ALTER TABLE event_user_reveal ADD CONSTRAINT event_reveal_need_fk FOREIGN KEY (event_need_id) REFERENCES event_need(id);
ALTER TABLE event_user_reveal ADD CONSTRAINT event_revealer_user_fk FOREIGN KEY (revealer_id) REFERENCES user(facebook_id);
ALTER TABLE event_user_reveal ADD CONSTRAINT event_revealed_to_user_fk FOREIGN KEY (revealed_to_id) REFERENCES user(facebook_id);

ALTER TABLE join_request ADD CONSTRAINT join_request_need_fk FOREIGN KEY (event_need_id) REFERENCES event_need(id);
ALTER TABLE join_request ADD CONSTRAINT join_request_user_fk FOREIGN KEY (joiner_id) REFERENCES user(facebook_id);

ALTER TABLE reveal_request ADD CONSTRAINT reveal_request_need_fk FOREIGN KEY (event_need_id) REFERENCES event_need(id);
ALTER TABLE reveal_request ADD CONSTRAINT reveal_request_user_fk FOREIGN KEY (user_id) REFERENCES user(facebook_id);
ALTER TABLE reveal_request ADD CONSTRAINT reveal_requester_user_fk FOREIGN KEY (requester_id) REFERENCES user(facebook_id);

ALTER TABLE reveal_notification ADD CONSTRAINT reveal_notification_need_fk FOREIGN KEY (event_need_id) REFERENCES event_need(id);
ALTER TABLE reveal_notification ADD CONSTRAINT reveal_notification_user_fk FOREIGN KEY (user_id) REFERENCES user(facebook_id);
ALTER TABLE reveal_notification ADD CONSTRAINT reveal_revealer_user_fk FOREIGN KEY (revealer_id) REFERENCES user(facebook_id);

--CREATE SEQUENCE verb_sequence START WITH 1 INCREMENT BY 1;
--
--CREATE TABLE IF NOT EXISTS verb (
--    id INT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR verb_sequence),
--    name VARCHAR(50)
--);
--
--CREATE SEQUENCE user_sequence START WITH 1 INCREMENT BY 1;
--
--CREATE TABLE IF NOT EXISTS noun (
--    id INT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR user_sequence),
--    name VARCHAR(50)
--);
--
--CREATE SEQUENCE outbox_sequence START WITH 1 INCREMENT BY 1;
--
--CREATE TABLE IF NOT EXISTS outbox (
--    id INT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR outbox_sequence),
--    user_id INT NOT NULL,
--    content TEXT
--);
--
--CREATE SEQUENCE inbox_sequence START WITH 1 INCREMENT BY 1;
--
--CREATE TABLE IF NOT EXISTS inbox (
--    id INT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR inbox_sequence),
--    user_id INT NOT NULL,
--    sender_id INT NOT NULL,
--    content TEXT
--);

--ALTER TABLE outbox ADD CONSTRAINT outbox_user_fk FOREIGN KEY (user_id) REFERENCES user(id);
--ALTER TABLE inbox ADD CONSTRAINT inbox_user_fk FOREIGN KEY (user_id) REFERENCES user(id);
--ALTER TABLE inbox ADD CONSTRAINT inbox_sender_fk FOREIGN KEY (sender_id) REFERENCES user(id);