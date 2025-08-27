BEGIN;

-- Make sure we're hitting the right tables
-- SET search_path TO platform;

DO $$
DECLARE
table_comp_id UUID;
    field_comp_id UUID;
    rec RECORD;
BEGIN
    -- 1) users table component
INSERT INTO components (component_type, status)
VALUES ('TABLE', 'ACTIVE')
    RETURNING id INTO table_comp_id;

-- 2) table row
INSERT INTO tables (id, type, logical_name, removable)
VALUES (table_comp_id, 'USERS', 'users', false);

-- 3) set NLS in one statement (no nested jsonb_set)
UPDATE components
SET nls_labels = '{
      "en":{"LABEL":"User","PLURAL_LABEL":"Users"},
      "es":{"LABEL":"Usuario","PLURAL_LABEL":"Usuarios"}
    }'::jsonb
WHERE id = table_comp_id;

-- 4) temp fields
CREATE TEMP TABLE temp_fields (
        field_order INT,
        field_name TEXT,
        data_type   INT,
        primary_key BOOLEAN,
        auto_generated BOOLEAN,
        label_singular_en TEXT,
        label_singular_es TEXT
    ) ON COMMIT DROP;

INSERT INTO temp_fields
(field_order, field_name, data_type, primary_key, auto_generated, label_singular_en, label_singular_es)
VALUES
    (1, 'id',             8, true,  true,  'ID',             'ID'),
    (2, 'uuid',           1, false, true,  'UUID',           'UUID'),
    (3, 'username',       2, false, false, 'Username',       'Nombre de usuario'),
    (4, 'email',          2, false, false, 'Email',          'Correo electrónico'),
    (5, 'first_name',     2, false, false, 'First Name',     'Nombre'),
    (6, 'last_name',      2, false, false, 'Last Name',      'Apellido'),
    (7, 'password_hash',  9, false, false, 'Password',       'Hash de contraseña'),
    (8, 'email_verified', 3, false, false, 'Email Verified', 'Correo verificado'),
    (9, 'enabled',        3, false, false, 'Enabled',        'Habilitado');

-- 5) loop fields
FOR rec IN SELECT * FROM temp_fields ORDER BY field_order LOOP
           -- a) field component
           INSERT INTO components (component_type, status)
           VALUES ('FIELD', 'ACTIVE')
               RETURNING id INTO field_comp_id;

-- b) field row
INSERT INTO fields (id, table_id, data_type, name, primary_key, auto_generated, removable)
VALUES (field_comp_id, table_comp_id, rec.data_type, rec.field_name, rec.primary_key, rec.auto_generated, false);

-- c) set NLS in one shot
UPDATE components
SET nls_labels = jsonb_build_object(
        'en', jsonb_build_object('LABEL', rec.label_singular_en),
        'es', jsonb_build_object('LABEL', rec.label_singular_es)
                 )
WHERE id = field_comp_id;
END LOOP;
END $$;

-- attribute created_by
UPDATE components
SET created_by = users.id
    FROM users
WHERE users.username = 'admin';

COMMIT;