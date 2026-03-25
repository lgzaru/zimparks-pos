-- ============================================================
--  ZimParks POS – Seed Data
--  Passwords are generated as BCrypt for "1234"
-- ============================================================

CREATE
EXTENSION IF NOT EXISTS pgcrypto;

-- Regions
INSERT INTO regions (id, cluster, name)
VALUES ('6ba7b810-9dad-11d1-80b4-00c04fd43010', 'HW', 'Hwange Cluster'),
       ('6ba7b810-9dad-11d1-80b4-00c04fd43040', 'HE', 'Harare Cluster'),
       ('6ba7b810-9dad-11d1-80b4-00c04fd43060', 'MT', 'Matobo Cluster'),
       ('6ba7b810-9dad-11d1-80b4-00c04fd43070', 'MD', 'Matusadonha Cluster'),
       ('6ba7b810-9dad-11d1-80b4-00c04fd43080', 'MZ', 'Mid Zambezi Cluster'),
       ('6ba7b810-9dad-11d1-80b4-00c04fd43090', 'NG', 'Ngezi Cluster'),
       ('6ba7b345-9dad-11d1-80b4-00c04fd230a0', 'NY', 'Nyanga Cluster'),
       ('6ba7b945-9dad-11d1-80b4-00c67fd120a0', 'HQ', 'Corporate Office') ON CONFLICT (id) DO NOTHING;

-- Stations
INSERT INTO stations (id, name, cluster_code, region_id)
VALUES
    -- HE Cluster
    ('ST_HE_01', 'Chivero South', 'HE', '6ba7b810-9dad-11d1-80b4-00c04fd43040'),
    ('ST_HE_02', 'Boulton Atlantica', 'HE', '6ba7b810-9dad-11d1-80b4-00c04fd43040'),

    -- HW Cluster
    ('ST_HW_01', 'Hwange Main Camp', 'HW', '6ba7b810-9dad-11d1-80b4-00c04fd43010'),
    ('ST_HW_02', 'Kazuma Pan', 'HW', '6ba7b810-9dad-11d1-80b4-00c04fd43010'),
    ('ST_HW_03', 'Matetsi 2', 'HW', '6ba7b810-9dad-11d1-80b4-00c04fd43010'),
    ('ST_HW_04', 'Matetsi 5', 'HW', '6ba7b810-9dad-11d1-80b4-00c04fd43010'),
    ('ST_HW_05', 'Katombora', 'HW', '6ba7b810-9dad-11d1-80b4-00c04fd43010'),

    -- MZ Cluster
    ('ST_MZ_01', 'Mkanga Field Station', 'MZ', '6ba7b810-9dad-11d1-80b4-00c04fd43080'),
    ('ST_MZ_02', 'Dande Safari Area', 'MZ', '6ba7b810-9dad-11d1-80b4-00c04fd43080'),
    ('ST_MZ_03', 'Kapirinhengu', 'MZ', '6ba7b810-9dad-11d1-80b4-00c04fd43080'),
    ('ST_MZ_04', 'Mana Pools', 'MZ', '6ba7b810-9dad-11d1-80b4-00c04fd43080')
    ON CONFLICT (id) DO NOTHING;

-- Users
INSERT INTO users (id, username, full_name, role, password, active, station_id, cell_phone)
VALUES ('5c2ec8f9-6a3f-4d2a-b9ee-39f11f7ec8b1', 'AMUDAVANHU', 'Alfred Mudavanhu', 'OPERATOR',
        crypt('1234', gen_salt('bf', 10)), TRUE, 'ST_HE_01', '263784481927'),
       ('09b5ccf3-4195-46ac-8f84-2ac7c6f7da8a', 'MNYABANGA', 'Michael Nyabanga', 'OPERATOR',
        crypt('1234', gen_salt('bf', 10)), TRUE, 'ST_HE_02', '263773288725'),
       ('4c6e1372-12b5-4f4c-8a52-e5ee7802f4c2', 'RSATORO', 'Rufaro Satoro', 'SUPERVISOR',
        crypt('1234', gen_salt('bf', 10)), TRUE, 'ST_HE_01', '263773361828'),
       ('f2dd7026-8557-42c2-b4a8-934a8f8f4c41', 'IMOYO', 'Ike Moyo', 'SUPERVISOR', crypt('1234', gen_salt('bf', 10)),
        TRUE, 'ST_HE_02', '263787715129'),
       ('4c6e1372-12b5-4f4c-8a52-e5ee0317f4c2', 'LZARU', 'Lovemore Zaru', 'ADMIN', crypt('1234', gen_salt('bf', 10)),
        TRUE, NULL, '263715023202'),
       ('f2dd7026-8557-42c2-b4a8-034a8f8f4c01', 'PMUJERI', 'Prisca Mujeri', 'ADMIN', crypt('1234', gen_salt('bf', 10)),
        TRUE, NULL, '263782440382') ON CONFLICT (username) DO NOTHING;

-- User Permissions
INSERT INTO user_permissions (user_id, permission)
VALUES ('5c2ec8f9-6a3f-4d2a-b9ee-39f11f7ec8b1', 'CLOSE_SHIFT'),
       ('5c2ec8f9-6a3f-4d2a-b9ee-39f11f7ec8b1', 'UPDATE_PRODUCT_PRICING'),
       ('5c2ec8f9-6a3f-4d2a-b9ee-39f11f7ec8b1', 'ADD_PRODUCT'),
       ('5c2ec8f9-6a3f-4d2a-b9ee-39f11f7ec8b1', 'LINK_BANKS'),
       ('5c2ec8f9-6a3f-4d2a-b9ee-39f11f7ec8b1', 'UNLINK_BANKS') ON CONFLICT (user_id, permission) DO NOTHING;

-- Currencies
INSERT INTO currencies (code, name, exchange_rate)
VALUES ('USD', 'United States Dollar', 1.0000),
       ('ZWG', 'Zimbabwe Gold', 25.5000),
       ('ZAR', 'South African Rand', 19.1000),
       ('BWP', 'Botswana Pula', 13.8000) ON CONFLICT (code) DO NOTHING;

-- Banks
INSERT INTO banks (code, name)
VALUES ('CBZ001', 'CBZ Bank USD - 36009000'),
       ('FBC001', 'FBC POS USD - 36032000'),
       ('CBZ002', 'CBZ POS ZWG Bank - 37453000'),
       ('ZBZ001', 'ZB POS ZWG - 35023000') ON CONFLICT (code) DO NOTHING;

-- Station Banks (Linking)
INSERT INTO station_banks (station_id, bank_code)
VALUES ('ST_HE_01', 'CBZ001'),
       ('ST_HW_02', 'CBZ001'),
       ('ST_HW_03', 'CBZ002'),
       ('ST_HW_04', 'ZBZ001'),
       ('ST_HW_05', 'CBZ001'),
       ('ST_HW_01', 'CBZ001'),
       ('ST_MZ_02', 'ZBZ001') ON CONFLICT DO NOTHING;

-- Product Categories
INSERT INTO product_categories (code, description)
VALUES ('A', 'Conservation Fees Land'),
       ('B', 'Conservation Fees Water'),
       ('C', 'Accommodation'),
       ('D', 'Permits'),
       ('E', 'Services and Facilities'),
       ('F', 'Law Enforcement'),
       ('G', 'Hunting'),
       ('H', 'Sale of Park Products'),
       ('J', 'Fishing Permit') ON CONFLICT (code) DO NOTHING;

-- Products
INSERT INTO products (code, station_id, created_by, created_at, category_code, descr, price, entry_product)
VALUES ('STHE01P001', 'ST_HE_01', 'System', NOW(), 'A', 'Vehicle (Regional)', 5.00, TRUE),
       ('STHE01P002', 'ST_HE_01', 'System', NOW(), 'A', 'Vehicle (Local)', 5.00, TRUE),
       ('STHE01P003', 'ST_HE_01', 'System', NOW(), 'A', 'Vehicle (International)', 15.00, TRUE),

       ('STHE01P004', 'ST_HE_01', 'System', NOW(), 'A', 'Adult (Regional)', 15.00, TRUE),
       ('STHE01P005', 'ST_HE_01', 'System', NOW(), 'A', 'Adult (Local)', 10.00, TRUE),
       ('STHE01P006', 'ST_HE_01', 'System', NOW(), 'A', 'Adult (International)', 20.00, TRUE),

       ('STHE01P007', 'ST_HE_01', 'System', NOW(), 'A', 'Child (Regional)', 10.00, TRUE),
       ('STHE01P008', 'ST_HE_01', 'System', NOW(), 'A', 'Child (Local)', 5.00, TRUE),
       ('STHE01P009', 'ST_HE_01', 'System', NOW(), 'A', 'Child (International)', 15.00, TRUE),

       ('STHE01P010', 'ST_HE_01', 'System', NOW(), 'C', 'Camping Fee', 25.00, FALSE),
       ('STHE01P011', 'ST_HE_01', 'System', NOW(), 'E', 'Vehicle Entry', 10.00, FALSE),
       ('STHE01P012', 'ST_HE_01', 'System', NOW(), 'E', 'Guide Service', 30.00,
        FALSE) ON CONFLICT (code, station_id) DO NOTHING;

-- VAT Settings
INSERT INTO vat_settings (id, zwg_rate, other_rate)
VALUES (1, 15.00, 15.50) ON CONFLICT (id) DO
UPDATE SET
    zwg_rate = EXCLUDED.zwg_rate,
    other_rate = EXCLUDED.other_rate;

-- Customers
INSERT INTO customers (id, name, email, phone, type, nationality)
VALUES ('CUST001', 'Cuckoo Safaris', 'cuckoo@gmail.com', '+263773000001', 'Individual', 'Zimbabwean'),
       ('CUST002', 'Africa Zim Travel & Tours', 'africatours@gmail.com', '+263773000002', 'Individual',
        'South African'),
       ('CUST003', 'Africa Uncovered Safaris', 'safaris@gmail.com', '+263773000003', 'Corporate', 'Zimbabwean'),
       ('CUST004', 'Savannah Adventures', 'savanha@gmail.com', '+263773000004', 'Individual', 'Zimbabwean'),
       ('CUST005', 'DK Tours and Safaris', 'dk@gmail.com', '+263773000005', 'Individual', 'South African'),
       ('CUST006', 'Zambezi Quest Travel and Tours', 'quest@gmail.com', '+263773000006', 'Corporate',
        'Zimbabwean') ON CONFLICT (id) DO NOTHING;