CREATE TABLE role
(
    id   UUID NOT NULL,
    name VARCHAR(255) NOT NULL UNIQUE,
    CONSTRAINT pk_role PRIMARY KEY (id)
);

CREATE TABLE role_authority
(
    role_id      UUID NOT NULL,
    authority_id UUID NOT NULL,
    CONSTRAINT pk_role_authority PRIMARY KEY (role_id, authority_id),
    CONSTRAINT fk_role_authority_role FOREIGN KEY (role_id) REFERENCES role (id),
    CONSTRAINT fk_role_authority_authority FOREIGN KEY (authority_id) REFERENCES authority (id)
);

CREATE TABLE user_role
(
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES "user" (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (id)
);

INSERT INTO role (id, name) VALUES (gen_random_uuid(), 'user');

INSERT INTO role_authority (role_id, authority_id)
SELECT r.id, a.id
FROM role r,
     authority a
WHERE r.name = 'user'
  AND a.name = 'package:read';

GRANT SELECT, INSERT, UPDATE, DELETE ON role TO ${app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON role_authority TO ${app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON user_role TO ${app_user};

CREATE TRIGGER role_audit_log_trigger
    AFTER INSERT OR UPDATE OR DELETE ON role
    FOR EACH ROW
EXECUTE FUNCTION audit_log_trigger('id');

CREATE TRIGGER role_authority_audit_log_trigger
    AFTER INSERT OR UPDATE OR DELETE ON role_authority
    FOR EACH ROW
EXECUTE FUNCTION audit_log_trigger('role_id');

CREATE TRIGGER user_role_audit_log_trigger
    AFTER INSERT OR UPDATE OR DELETE ON user_role
    FOR EACH ROW
EXECUTE FUNCTION audit_log_trigger('user_id');
