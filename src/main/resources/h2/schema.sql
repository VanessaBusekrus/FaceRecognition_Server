CREATE SCHEMA IF NOT EXISTS public;
SET SCHEMA public;
CREATE TABLE IF NOT EXISTS login
(
    email VARCHAR(100) NOT NULL UNIQUE,
    hash  VARCHAR(100) NOT NULL
);
CREATE TABLE IF NOT EXISTS users
(
    id      INT auto_increment PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    email   VARCHAR(100) NOT NULL UNIQUE,
    joined  DATE         NOT NULL DEFAULT CURRENT_DATE(),
    entries INT                   DEFAULT 0,
    phone   VARCHAR(100),
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    temp_two_factor_secret VARCHAR(255)
);
