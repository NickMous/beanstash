CREATE TABLE authority
(
    id   UUID NOT NULL,
    name VARCHAR(255) NOT NULL UNIQUE,
    CONSTRAINT pk_authority PRIMARY KEY (id)
);

CREATE TABLE user_authority
(
    user_id      UUID NOT NULL,
    authority_id UUID NOT NULL,
    CONSTRAINT pk_user_authority PRIMARY KEY (user_id, authority_id),
    CONSTRAINT fk_user_authority_user FOREIGN KEY (user_id) REFERENCES "user" (id),
    CONSTRAINT fk_user_authority_authority FOREIGN KEY (authority_id) REFERENCES authority (id)
);

INSERT INTO authority (id, name) VALUES (gen_random_uuid(), 'package:read');

GRANT SELECT, INSERT, UPDATE, DELETE ON authority TO ${app_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON user_authority TO ${app_user};

CREATE TRIGGER authority_audit_log_trigger
    AFTER INSERT OR UPDATE OR DELETE ON authority
    FOR EACH ROW
EXECUTE FUNCTION audit_log_trigger('id');

CREATE TRIGGER user_authority_audit_log_trigger
    AFTER INSERT OR UPDATE OR DELETE ON user_authority
    FOR EACH ROW
EXECUTE FUNCTION audit_log_trigger('user_id');
