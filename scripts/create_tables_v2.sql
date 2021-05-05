CREATE DATABASE IF NOT EXISTS dosomething_db;

USE dosomething_db;

CREATE SEQUENCE user_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS user (
    id INT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR user_sequence),
    facebook_id VARCHAR(20) DEFAULT NULL,
    firebase_uid VARCHAR(30) NOT NULL,
    name VARCHAR(50) DEFAULT NULL,
    longitude DOUBLE(15, 13) DEFAULT NULL,
    latitude DOUBLE(15, 13) DEFAULT NULL,
    high_school_id INT NULL,
    university_id INT NULL,
    work_place_id INT NULL
);

CREATE TABLE IF NOT EXISTS friend (
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    PRIMARY KEY (user_id, friend_id)
);

CREATE SEQUENCE tag_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR tag_sequence),
    creator_id INT NOT NULL,
    tag VARCHAR(100) NOT NULL
);

CREATE SEQUENCE event_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS event (
    id BIGINT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR event_sequence),
    creator_id INT NOT NULL,
    activity VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    date DATE,
    time TIME,
    longitude DOUBLE(15, 13),
    latitude DOUBLE(15, 13),
    is_cancelled BOOL DEFAULT FALSE,
    is_confirmed BOOL DEFAULT FALSE,
    visibility_preference INT NOT NULL,
    interest_preference INT NOT NULL,
    timestamp TIMESTAMP
);

CREATE TABLE IF NOT EXISTS event_interested (
    event_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    description VARCHAR(100),
    PRIMARY KEY (event_id, user_id)
--    code_name VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS interest_notification (
    event_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (event_id, user_id)
);

CREATE TABLE IF NOT EXISTS event_visibility (
    event_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    PRIMARY KEY (event_id, user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS visibility_request (
    event_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    requester_id INT NOT NULL,
    PRIMARY KEY (event_id, user_id, requester_id)
);

CREATE TABLE IF NOT EXISTS visibility_notification (
    event_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    PRIMARY KEY (event_id, user_id, friend_id)
);

CREATE SEQUENCE confirmed_event_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS confirmed_event (
    id BIGINT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR confirmed_event_sequence),
    event_id BIGINT NOT NULL,
    creator_id INT NOT NULL,
    activity VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    date DATE,
    time TIME,
    longitude DOUBLE(15, 13),
    latitude DOUBLE(15, 13),
    is_cancelled BOOL DEFAULT FALSE,
    is_public BOOL DEFAULT TRUE,
    is_happening BOOL DEFAULT FALSE,
    visibility_preference INT NOT NULL,
    timestamp TIMESTAMP
);

CREATE TABLE IF NOT EXISTS event_join_request (
    event_id BIGINT NOT NULL,
    requester_id INT NOT NULL,
    created_time TIMESTAMP,
    PRIMARY KEY (event_id, requester_id)
);

CREATE TABLE IF NOT EXISTS event_participant (
    event_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    is_confirmed BOOL DEFAULT FALSE,
    PRIMARY KEY (event_id, user_id)
--    code_name VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS event_invite (
    event_id BIGINT NOT NULL,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    PRIMARY KEY (event_id, sender_id, receiver_id)
--    code_name VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS accept_notification (
    event_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (event_id, user_id)
);

CREATE TABLE IF NOT EXISTS user_activity_history (
    user_id INT NOT NULL,
    event_id BIGINT NOT NULL,
    updated_time TIMESTAMP,
    PRIMARY KEY (user_id, event_id)
);

CREATE TABLE IF NOT EXISTS current_activity (
    event_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    updated_time TIMESTAMP,
    PRIMARY KEY (user_id)
);

CREATE TABLE IF NOT EXISTS popular_event (
    event_id BIGINT NOT NULL,
--    tag VARCHAR(100) NOT NULL,
    updated_time TIMESTAMP,
    PRIMARY KEY (event_id)
);

CREATE TABLE IF NOT EXISTS popular_confirmed_event (
    event_id BIGINT NOT NULL,
--    tag VARCHAR(100) NOT NULL,
    updated_time TIMESTAMP,
    PRIMARY KEY (event_id)
);

CREATE TABLE IF NOT EXISTS popular_active_event (
    event_id BIGINT NOT NULL,
    user_id INT NOT NULL,
--    tag VARCHAR(100) NULL,
    updated_time TIMESTAMP,
    PRIMARY KEY (event_id, user_id)
);

CREATE SEQUENCE high_school_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS high_school (
    id INT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR high_school_sequence),
    name VARCHAR(100) DEFAULT NULL
);

CREATE SEQUENCE university_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS university (
    id INT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR university_sequence),
    name VARCHAR(100) DEFAULT NULL
);

CREATE SEQUENCE work_place_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS work_place (
    id INT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR work_place_sequence),
    name VARCHAR(100) DEFAULT NULL
);

ALTER TABLE user ADD CONSTRAINT user_high_school_fk FOREIGN KEY (high_school_id) REFERENCES high_school(id);
ALTER TABLE user ADD CONSTRAINT user_university_fk FOREIGN KEY (university_id) REFERENCES university(id);
ALTER TABLE user ADD CONSTRAINT user_work_place_fk FOREIGN KEY (work_place_id) REFERENCES work_place(id);

ALTER TABLE friend ADD CONSTRAINT friend_user_id_fk FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE friend ADD CONSTRAINT friend_friend_id_fk FOREIGN KEY (friend_id) REFERENCES user(id);

ALTER TABLE event ADD CONSTRAINT event_user_fk FOREIGN KEY (creator_id) REFERENCES user(id);

ALTER TABLE popular_event ADD CONSTRAINT popular_event_event_fk FOREIGN KEY (event_id) REFERENCES event(id);

ALTER TABLE popular_confirmed_event ADD CONSTRAINT popular_confirmed_event_fk FOREIGN KEY (event_id) REFERENCES confirmed_event(id);

ALTER TABLE popular_active_event ADD CONSTRAINT popular_active_event_fk FOREIGN KEY (event_id) REFERENCES confirmed_event(id);
ALTER TABLE popular_active_event ADD CONSTRAINT popular_active_user_fk FOREIGN KEY (user_id) REFERENCES user(id);

ALTER TABLE event_interested ADD CONSTRAINT event_interested_event_fk FOREIGN KEY (event_id) REFERENCES event(id);
ALTER TABLE event_interested ADD CONSTRAINT event_interested_user_fk FOREIGN KEY (user_id) REFERENCES user(id);

ALTER TABLE interest_notification ADD CONSTRAINT interest_notification_need_fk FOREIGN KEY (event_id) REFERENCES event(id);
ALTER TABLE interest_notification ADD CONSTRAINT interest_notification_user_fk FOREIGN KEY (user_id) REFERENCES user(id);

ALTER TABLE event_visibility ADD CONSTRAINT event_visibility_need_fk FOREIGN KEY (event_id) REFERENCES event(id);
ALTER TABLE event_visibility ADD CONSTRAINT event_visibility_user_fk FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE event_visibility ADD CONSTRAINT event_visibility_friend_fk FOREIGN KEY (friend_id) REFERENCES user(id);

ALTER TABLE visibility_request ADD CONSTRAINT visibility_request_need_fk FOREIGN KEY (event_id) REFERENCES event(id);
ALTER TABLE visibility_request ADD CONSTRAINT visibility_request_user_fk FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE visibility_request ADD CONSTRAINT visibility_request_friend_user_fk FOREIGN KEY (requester_id) REFERENCES user(id);

ALTER TABLE visibility_notification ADD CONSTRAINT visibility_notification_need_fk FOREIGN KEY (event_id) REFERENCES event(id);
ALTER TABLE visibility_notification ADD CONSTRAINT visibility_notification_user_fk FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE visibility_notification ADD CONSTRAINT visibility_notification_friend_user_fk FOREIGN KEY (friend_id) REFERENCES user(id);

ALTER TABLE confirmed_event ADD CONSTRAINT confirmed_event_event_fk FOREIGN KEY (event_id) REFERENCES event(id);
ALTER TABLE confirmed_event ADD CONSTRAINT confirmed_creator_fk FOREIGN KEY (creator_id) REFERENCES user(id);

ALTER TABLE event_join_request ADD CONSTRAINT event_join_event_fk FOREIGN KEY (event_id) REFERENCES confirmed_event(id);
ALTER TABLE event_join_request ADD CONSTRAINT event_join_user_fk FOREIGN KEY (requester_id) REFERENCES user(id);

ALTER TABLE event_participant ADD CONSTRAINT event_participant_need_fk FOREIGN KEY (event_id) REFERENCES confirmed_event(id);
ALTER TABLE event_participant ADD CONSTRAINT event_participant_user_fk FOREIGN KEY (user_id) REFERENCES user(id);

ALTER TABLE event_invite ADD CONSTRAINT invite_event_fk FOREIGN KEY (event_id) REFERENCES confirmed_event(id);
ALTER TABLE event_invite ADD CONSTRAINT sender_user_fk FOREIGN KEY (sender_id) REFERENCES user(id);
ALTER TABLE event_invite ADD CONSTRAINT receiver_id_user_fk FOREIGN KEY (receiver_id) REFERENCES user(id);

ALTER TABLE accept_notification ADD CONSTRAINT accept_notification_need_fk FOREIGN KEY (event_id) REFERENCES confirmed_event(id);
ALTER TABLE accept_notification ADD CONSTRAINT accept_notification_user_fk FOREIGN KEY (user_id) REFERENCES user(id);

ALTER TABLE current_activity ADD CONSTRAINT current_activity_user_fk FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE current_activity ADD CONSTRAINT current_activity_event_fk FOREIGN KEY (event_id) REFERENCES confirmed_event(id);

ALTER TABLE user_activity_history ADD CONSTRAINT active_event_hist_user_fk FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE user_activity_history ADD CONSTRAINT active_event_hist_event_fk FOREIGN KEY (event_id) REFERENCES confirmed_event(id);