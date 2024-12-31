drop database platform;

create database platform;

create user keycloak with password 'qwerty';

create user platform with password 'qwerty';

\connect platform

GRANT ALL ON SCHEMA public TO keycloak;

GRANT ALL ON SCHEMA public TO platform;

-- Enable the uuid-ossp extension for UUID generation (PostgreSQL specific)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create the users table with UUID primary key
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       username VARCHAR(255) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE,
                       first_name VARCHAR(255),
                       last_name VARCHAR(255),
                       password_hash VARCHAR(255) NOT NULL,
                       email_verified BOOLEAN DEFAULT FALSE,
                       enabled BOOLEAN DEFAULT TRUE
);

-- Create an index on username for faster lookups
CREATE INDEX idx_users_username ON users(username);

-- Create an index on email for faster lookups
CREATE INDEX idx_users_email ON users(email);

-- Create the user_attributes table with UUID primary key and foreign key
CREATE TABLE user_attributes (
                                 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 name VARCHAR(255) NOT NULL,
                                 value VARCHAR(255),
                                 UNIQUE (user_id, name)
);

-- Create an index on user_id for faster lookups
CREATE INDEX idx_user_attributes_user_id ON user_attributes(user_id);

-- Optional: Create indexes on name and value if you frequently search by attributes
CREATE INDEX idx_user_attributes_name ON user_attributes(name);
CREATE INDEX idx_user_attributes_value ON user_attributes(value);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE users TO keycloak;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE user_attributes TO keycloak;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO platform;

insert into users(username, email, first_name, last_name, password_hash) values ('admin', 'admin@example.com','Admin','Account','$2a$10$F.RKkkj5BaSipxxpAQnx2.dogjoEsBNvgSLAwvcgkvQcUYThxke52');
insert into users(username, email, first_name, last_name, password_hash) values ('user','user@example.com','User','Account','$2a$10$GScPuSmLxLwwdOCmar1abOCXr9xSogz/1/ABBKRXy8YMAnS6cUqr2');

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
    CONSTRAINT l_name UNIQUE(logical_name),
    CONSTRAINT p_name UNIQUE(physical_name)
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
    PRIMARY KEY (component_id, language_code, label_type),
    CONSTRAINT fk_label_component
        FOREIGN KEY (component_id)
        REFERENCES component (id)
        ON DELETE CASCADE
);
