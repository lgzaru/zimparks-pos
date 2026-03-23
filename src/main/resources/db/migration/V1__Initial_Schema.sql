-- V1: Initial schema based on core entities

CREATE TABLE banks (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE regions (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    cluster VARCHAR(10) NOT NULL
);

CREATE TABLE stations (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    region_id UUID REFERENCES regions(id),
    cluster_code VARCHAR(10)
);

CREATE TABLE station_banks (
    station_id VARCHAR(10) REFERENCES stations(id),
    bank_code VARCHAR(10) REFERENCES banks(code),
    PRIMARY KEY (station_id, bank_code)
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    station_id VARCHAR(10) REFERENCES stations(id),
    password VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    current_token VARCHAR(1000),
    cell_phone VARCHAR(20) UNIQUE,
    reset_otp VARCHAR(6),
    reset_otp_expiry TIMESTAMP
);

CREATE TABLE user_permissions (
    user_id UUID REFERENCES users(id),
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, permission)
);

CREATE TABLE product_categories (
    code VARCHAR(2) PRIMARY KEY,
    description VARCHAR(100) NOT NULL
);

CREATE TABLE products (
    code VARCHAR(20),
    station_id VARCHAR(10),
    descr VARCHAR(200) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category_code VARCHAR(2) REFERENCES product_categories(code),
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    entry_product BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (code, station_id),
    FOREIGN KEY (station_id) REFERENCES stations(id)
);

CREATE TABLE activity_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    username VARCHAR(255) NOT NULL,
    operation VARCHAR(255) NOT NULL,
    details TEXT,
    timestamp TIMESTAMP NOT NULL
);
