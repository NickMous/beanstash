INSERT INTO authority (id, name) VALUES (gen_random_uuid(), 'user:read');
INSERT INTO authority (id, name) VALUES (gen_random_uuid(), 'user:update');
INSERT INTO authority (id, name) VALUES (gen_random_uuid(), 'user:delete');

INSERT INTO role (id, name) VALUES (gen_random_uuid(), 'admin');

INSERT INTO role_authority (role_id, authority_id)
SELECT r.id, a.id
FROM role r,
     authority a
WHERE r.name = 'admin'
  AND a.name IN ('package:read', 'user:read', 'user:update', 'user:delete');

INSERT INTO role_authority (role_id, authority_id)
SELECT r.id, a.id
FROM role r,
     authority a
WHERE r.name = 'user'
  AND a.name IN ('user:read');
