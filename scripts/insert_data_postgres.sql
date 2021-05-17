INSERT INTO high_school
    (name)
    VALUES
    ('Royal College');

INSERT INTO university
    (name)
    VALUES
    ('University of Moratuwa'),
    ('University of Colombo');

INSERT INTO work_place
    (name)
    VALUES
    ('DoSomething Inc.'),
    ('John Keells'),
    ('Nestle');


-- These UIDs are email-password login only. Besides Asitha, Kalpana and Dulitha, others are not linked with facebook accounts
INSERT INTO user_profile
    (facebook_id, firebase_uid, name, longitude, latitude, high_school_id, university_id, work_place_id)
    VALUES
    ('111157127696026', 'V4kLwsaSJdgRJWaEK2wAJv9WxEG3', 'Dulitha Dabare', 79.8928502000000, 6.9275161000000 , 1, 1, 1),
    ('101027082060778', 'FK6NcgwcisNmMzTQR1CcR7RNyUp1', 'Asitha Kuruppu',79.95389526684494, 7.310342507856055 , 1, 2, null),
    ('100981172065335', 'fAb0IDiWYAM26X5VeLe3nCIvvRA2', 'Pasan Kalpana', 79.893805402944, 6.922079125183138, 1, 1, null),
    ('108942124588004', '4GUnSPt4JfOn74xdaaSQDL07zfl1', 'Shehan Gunathilake', null, null, 1, null, 2),
    ('104824335006510', '9FiVzcRIyJggIKbe0UGXqaaM7Xu1', 'Dilanka Gamage', null, null, 1, 1, 3),
    ('104940924994723', 'uln4EYGC2dZ0o3uOTCVCp5VkCri2', 'Nipuna Dinendra', null, null, 1, 1, null),
    ('106329398186384', 'Ux44WbkQcPXXiLsWFIrxwU11PYt2', 'Umesh Harshana', null, null, 1, 1, null),
    ('100447065454355', '1JRupGv2WFRm8oXQQ1J7VTZXyYI2', 'Betty Zamoresen', null, null, null, null, null),
    ('110285511117093', 'KJG0u7QvSVPFxv2Q5AZ22Zw24Zr1', 'Surath Gajanayake', null, null, 1, null, null);

INSERT INTO friend
    (user_id, friend_id)
    VALUES
    ('1', '2'),
    ('2', '1'),
    ('1', '3'),
    ('3', '1'),
    ('1', '4'),
    ('4', '1'),
    ('1', '5'),
    ('5', '1'),
    ('1', '6'),
    ('6', '1'),
    ('1', '7'),
    ('7', '1'),
    ('1', '9'),
    ('9', '1'),
    ('2', '3'),
    ('3', '2'),
    ('2', '4'),
    ('4', '2'),
    ('2', '5'),
    ('5', '2'),
    ('3', '4'),
    ('4', '3'),
    ('5', '9'),
    ('9', '5'),
    ('6', '9'),
    ('9', '6'),
    ('7', '9'),
    ('9', '7');

INSERT INTO current_activity
    (user_id, event_id)
    VALUES
    (1, null),
    (2, null),
    (3, null),
    (4, null),
    (5, null),
    (6, null),
    (7, null),
    (8, null),
    (9, null);

--INSERT INTO inbox
--    (user_id, content, sender_id)
--    VALUES
--    (2, "I want to watch a movie this Weekend", 1),
--    (3, "I want to watch a movie this Weekend", 1),
--    (4, "I want to watch a movie this Weekend", 1),
--    (5, "I want to watch a movie this Weekend", 1),
--    (6, "I want to watch a movie this Weekend", 1);
