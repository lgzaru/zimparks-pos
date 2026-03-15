-- ============================================================
--  ZimParks POS – Seed Data
--  Passwords are generated as BCrypt for "1234"
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Regions
INSERT INTO regions (id, name, cluster)
VALUES
  ('6ba7b810-9dad-11d1-80b4-00c04fd43010', 'Hwange National Park', 'HW'),
  ('6ba7b810-9dad-11d1-80b4-00c04fd43020', 'Binga', 'HW'),
  ('6ba7b810-9dad-11d1-80b4-00c04fd43030', 'Matetsi', 'HW'),
  ('6ba7b810-9dad-11d1-80b4-00c04fd43040', 'Chivero National Park', 'HE'),
  ('6ba7b810-9dad-11d1-80b4-00c04fd43050', 'Darwendale', 'HE'),
  ('6ba7b810-9dad-11d1-80b4-00c04fd43060', 'Matobo National Park', 'MT'),
  ('6ba7b810-9dad-11d1-80b4-00c04fd43070', 'Matusadonha National Park', 'MD'),
  ('6ba7b810-9dad-11d1-80b4-00c04fd43080', 'Mana Pools National Park', 'MZ'),
  ('6ba7b810-9dad-11d1-80b4-00c04fd43090', 'Ngezi Recreational Park', 'NG'),
  ('6ba7b810-9dad-11d1-80b4-00c04fd430a0', 'Nyanga National Park', 'NY')
ON CONFLICT (id) DO NOTHING;

-- Stations
INSERT INTO stations (id, name, cluster_code, region_id)
VALUES
  ('ST01', 'Main Gate', 'HW', '6ba7b810-9dad-11d1-80b4-00c04fd43010'),
  ('ST02', 'North Entrance', 'HW', '6ba7b810-9dad-11d1-80b4-00c04fd43010'),
  ('ST03', 'East Gate', 'HE', '6ba7b810-9dad-11d1-80b4-00c04fd43040')
ON CONFLICT (id) DO NOTHING;

-- Users
INSERT INTO users (id, username, full_name, role, password, active, station_id, cell_phone)
VALUES
  ('5c2ec8f9-6a3f-4d2a-b9ee-39f11f7ec8b1', 'OPERATOR',   'Tendai Moyo',   'OPERATOR',   crypt('1234', gen_salt('bf', 10)), TRUE, 'ST01', '263773814511'),
  ('09b5ccf3-4195-46ac-8f84-2ac7c6f7da8a', 'OPERATOR2',  'Farai Dube',    'OPERATOR',   crypt('1234', gen_salt('bf', 10)), TRUE, 'ST02', '263773814511'),
  ('4c6e1372-12b5-4f4c-8a52-e5ee7802f4c2', 'SUPERVISOR', 'Rudo Chikwanda','SUPERVISOR', crypt('1234', gen_salt('bf', 10)), TRUE, 'ST01', '263773814511'),
  ('f2dd7026-8557-42c2-b4a8-934a8f8f4c41', 'ADMIN',      'System Admin',  'ADMIN',      crypt('1234', gen_salt('bf', 10)), TRUE, NULL, '263773814511')
ON CONFLICT (username) DO NOTHING;

-- User Permissions (Assigned to supervisor Chikwanda)
INSERT INTO user_permissions (user_id, permission)
VALUES
  ('4c6e1372-12b5-4f4c-8a52-e5ee7802f4c2', 'CLOSE_SHIFT'),
  ('4c6e1372-12b5-4f4c-8a52-e5ee7802f4c2', 'UPDATE_PRODUCT_PRICING'),
  ('4c6e1372-12b5-4f4c-8a52-e5ee7802f4c2', 'ADD_PRODUCT'),
  ('4c6e1372-12b5-4f4c-8a52-e5ee7802f4c2', 'LINK_BANKS'),
  ('4c6e1372-12b5-4f4c-8a52-e5ee7802f4c2', 'UNLINK_BANKS')
ON CONFLICT DO NOTHING;

-- Currencies
INSERT INTO currencies (code, name, exchange_rate)
VALUES
    ('USD', 'United States Dollar', 1.0000),
    ('ZWG', 'Zimbabwe Gold', 25.5000),
    ('ZAR', 'South African Rand', 19.1000),
    ('BWP', 'Botswana Pula', 13.8000)
ON CONFLICT (code) DO NOTHING;

-- Banks
INSERT INTO banks (code, name)
VALUES
    ('CBZ', 'CBZ Bank'),
    ('FBC', 'FBC Bank'),
    ('STB', 'Stanbic Bank')
ON CONFLICT (code) DO NOTHING;

-- Station Banks (Linking)
INSERT INTO station_banks (station_id, bank_code)
VALUES
  ('ST01', 'CBZ'),
  ('ST01', 'FBC'),
  ('ST01', 'STB'),
  ('ST02', 'CBZ'),
  ('ST02', 'FBC'),
  ('ST03', 'CBZ'),
  ('ST03', 'FBC')
ON CONFLICT DO NOTHING;

-- Product Categories
INSERT INTO product_categories (code, description)
VALUES
  ('A', 'Conservation Fees Land'),
  ('B', 'Conservation Fees Water'),
  ('C', 'Accommodation'),
  ('D', 'Permits'),
  ('E', 'Services and Facilities'),
  ('F', 'Law Enforcement'),
  ('G', 'Hunting'),
  ('H', 'Sale of Park Products'),
  ('J', 'Fishing Permit')
ON CONFLICT (code) DO NOTHING;

-- Products
INSERT INTO products (code, station_id, category_code, descr, price)
VALUES
  ('HWST01A001', 'ST01', 'A', 'Park Entry - Adult', 15.00),
  ('HWST02A002', 'ST02', 'A', 'Park Entry - Adult', 15.00),
  ('HWST01A003', 'ST01', 'A', 'Park Entry - Child',  8.00),
  ('HWST01C004', 'ST01', 'C', 'Camping Fee',        25.00),
  ('HWST01E005', 'ST01', 'E', 'Vehicle Entry',      10.00),
  ('HWST01E006', 'ST01', 'E', 'Guide Service',      30.00)
ON CONFLICT (code, station_id) DO NOTHING;

-- VAT Settings
INSERT INTO vat_settings (id, zwg_rate, other_rate)
VALUES (1, 15.00, 15.50)
ON CONFLICT (id) DO UPDATE SET 
    zwg_rate = EXCLUDED.zwg_rate,
    other_rate = EXCLUDED.other_rate;

-- Customers
INSERT INTO customers (id, name, email, phone, type, nationality)
VALUES
  ('CUST001', 'John Doe',    'john@example.com',   '+263771234567', 'Individual', 'Zimbabwean'),
  ('CUST002', 'Jane Smith',  'jane@example.com',   '+263772345678', 'Individual', 'South African'),
  ('CUST003', 'Safari Tours','info@safari.co.zw',  '+263773456789', 'Corporate',  'Zimbabwean')
ON CONFLICT (id) DO NOTHING;

