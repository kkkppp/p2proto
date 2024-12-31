BEGIN;

-- 1) Enable extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2) Create enums
CREATE TYPE component_type_enum AS ENUM (
    'TABLE',
    'FIELD',
    'PAGE',
    'ELEMENT'
);

CREATE TYPE label_type_enum AS ENUM (
    'LABEL',
    'PLURAL_LABEL',
    'DESCRIPTION',
    'INSTRUCTION',
    'POPUP'
);

-- 3) Parent table: component
CREATE TABLE component (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    component_type component_type_enum NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 4a) table_component
CREATE TABLE table_component (
    id UUID PRIMARY KEY,
    logical_name TEXT NOT NULL,
    physical_name TEXT NOT NULL,
    CONSTRAINT fk_tablecomponent_component
        FOREIGN KEY (id)
        REFERENCES component (id)
        ON DELETE CASCADE,
    CONSTRAINT l_name UNIQUE on (logical_name),
    CONSTRAINT p_name UNIQUE on (physical_name)
);

-- 4b) field_component
CREATE TABLE field_component (
    id UUID PRIMARY KEY,
    table_id UUID NOT NULL,
    name TEXT NOT NULL,
    data_type int NOT NULL,
    CONSTRAINT fk_fieldcomponent_component
        FOREIGN KEY (id)
        REFERENCES component (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_fieldcomponent_tablecomponent
        FOREIGN KEY (table_id)
        REFERENCES table_component (id)
        ON DELETE CASCADE
);

-- 4c) page_component
CREATE TABLE page_component (
    id UUID PRIMARY KEY,
    page_name TEXT NOT NULL,
    page_url TEXT,
    CONSTRAINT fk_pagecomponent_component
        FOREIGN KEY (id)
        REFERENCES component (id)
        ON DELETE CASCADE
);

-- 4d) element_component
CREATE TABLE element_component (
    id UUID PRIMARY KEY,
    page_id UUID NOT NULL,
    element_name TEXT NOT NULL,
    element_selector TEXT,
    CONSTRAINT fk_elementcomponent_component
        FOREIGN KEY (id)
        REFERENCES component (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_elementcomponent_pagecomponent
        FOREIGN KEY (page_id)
        REFERENCES page_component (id)
        ON DELETE CASCADE
);

-- 5) label
CREATE TABLE label (
    component_id UUID NOT NULL,
    language_code int NOT NULL,
    label_type label_type_enum NOT NULL,
    label_text TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (component_id, language_code, label_type)
    CONSTRAINT fk_label_component
        FOREIGN KEY (component_id)
        REFERENCES component (id)
        ON DELETE CASCADE
);

COMMIT;
