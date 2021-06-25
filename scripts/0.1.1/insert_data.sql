INSERT INTO user_profile
    (facebook_id, firebase_uid, name, longitude, latitude)
    VALUES
    (null, 'Zqtr3oLBGXYMScjbwYVeHA2kr652', null, null, null ),
    ('101027082060778', 'RZXJulHjreOTf8wvrVvxEKe3qZn2', 'Asitha Kuruppu',79.95389526684494, 7.310342507856055 ),
    (null, 'NBBZFL6UgLUMNa9UEWUQhA43xwF3', null, null, null ),
    ('100981172065335', 'xennpuVX5qMx7ozWqgYd91JWrOh2', 'Pasan Kalpana', 79.893805402944, 6.922079125183138 ),
    (null, 'test1', null, null, null ),
    (null, 'test2', null, null, null ),
    (null, 'dkpxM4xfdWRl0kZXnC1VyeCvwKn1', 'Dulitha Dabare',null, null )
    ;

INSERT INTO friend
    (user_id, friend_id)
    VALUES
    ('2', '4'),
    ('2', '7'),
    ('4', '2'),
    ('4', '7'),
    ('7', '2'),
    ('7', '4')
    ;