INSERT INTO user
    (facebook_id, firebase_uid, name)
    VALUES
    ('111157127696026', 'E1G1c6qsOCdwjtVpnHj3UkNdgal2', 'Dulitha Dabare'),
    ('101027082060778', 'KLHWWtRraQhfOmms2rzv0T9dr0r1', 'Asitha Kuruppu'),
    ('100981172065335', 'pAoLmck2i1QBLdwfHtY6ZfsDX842', 'Pasan Kalpana'),
    ('108942124588004', 'NeCLFD07PbYV8Nwodh4wQ5iDCot1', 'Shehan Gunathilake'),
    ('104824335006510', 'L7Z2J9OEwzcSnKDcisemUtAuz5B2', 'Dilanka Gamage'),
    ('104940924994723', '6Iuk4cvi8DeZXUSnAeP2HG4n4fn2', 'Nipuna Dinendra'),
    ('106329398186384', 'OWgkCz7dtdRq0YKqWQ9SEBvXRMz2', 'Umesh Harshana'),
    ('100447065454355', 'XfS7SMpaIlgaHPF6NuTiwCtukTE2', 'Betty Zamoresen'),
    ('110285511117093', 'KJG0u7QvSVPFxv2Q5AZ22Zw24Zr1', 'Surath Gajanayake');

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

--INSERT INTO inbox
--    (user_id, content, sender_id)
--    VALUES
--    (2, "I want to watch a movie this Weekend", 1),
--    (3, "I want to watch a movie this Weekend", 1),
--    (4, "I want to watch a movie this Weekend", 1),
--    (5, "I want to watch a movie this Weekend", 1),
--    (6, "I want to watch a movie this Weekend", 1);
