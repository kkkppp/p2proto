-- Begin Transaction
BEGIN;

-- Execute the PL/pgSQL DO block
DO $$
DECLARE
    table_comp_id UUID;    -- Variable to hold the 'users' table component ID
    field_comp_id UUID;    -- Variable to hold each field's component ID
    rec RECORD;            -- Record variable for looping through fields
BEGIN
    -- 1. Insert into component for the 'users' table
INSERT INTO components (component_type)
VALUES ('TABLE')
    RETURNING id INTO table_comp_id;

-- 2. Insert into table_component with the obtained UUID
INSERT INTO tables (id, logical_name, physical_name, removable)
VALUES (table_comp_id, 'users', 'users', false);

-- 3. Insert labels for the 'users' table
INSERT INTO nls_labels (component_id, language_code, label_type, label_text)
VALUES
    (table_comp_id, 'en', 'LABEL', 'User'),
    (table_comp_id, 'en', 'PLURAL_LABEL', 'Users'),
    (table_comp_id, 'es', 'LABEL', 'Usuario'),
    (table_comp_id, 'es', 'PLURAL_LABEL', 'Usuarios');

-- 4. Create and populate a temporary table to hold field information
CREATE TEMP TABLE temp_fields (
        field_order INT,
        field_name TEXT,
        data_type int,
        label_singular_en TEXT,
        label_singular_es TEXT
    ) ON COMMIT DROP;

    -- Populate the temporary table with field details
INSERT INTO temp_fields (field_order, field_name, data_type, label_singular_en, label_singular_es)
VALUES
    (1, 'id', 1, 'ID', 'ID'),
    (2, 'username', 2, 'Username', 'Nombre de usuario'),
    (3, 'email', 2, 'Email', 'Correo electrónico'),
    (4, 'first_name', 2, 'First Name', 'Nombre'),
    (5, 'last_name', 2, 'Last Name', 'Apellido'),
    (6, 'password_hash', 2, 'Password Hash', 'Hash de contraseña'),
    (7, 'email_verified', 3, 'Email Verified', 'Correo verificado'),
    (8, 'enabled', 3, 'Enabled', 'Habilitado');

-- 5. Loop through each field and insert into metadata tables
FOR rec IN SELECT * FROM temp_fields ORDER BY field_order LOOP
           -- a. Insert into component for the field
           INSERT INTO components (component_type)
           VALUES ('FIELD')
               RETURNING id INTO field_comp_id;

-- b. Insert into field_component
INSERT INTO fields (id, table_id, data_type, name, removable)
VALUES (field_comp_id, table_comp_id, rec.data_type, rec.field_name, false);

-- c. Insert English Singular Label
INSERT INTO nls_labels (component_id, language_code, label_type, label_text)
VALUES (field_comp_id, 'en', 'LABEL', rec.label_singular_en);

-- d. Insert Spanish Singular Label
INSERT INTO nls_labels (component_id, language_code, label_type, label_text)
VALUES (field_comp_id, 'es', 'LABEL', rec.label_singular_es);
END LOOP;
END $$;

-- Commit Transaction
COMMIT;