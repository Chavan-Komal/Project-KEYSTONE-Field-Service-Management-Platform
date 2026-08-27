-- Seed data — lets a reviewer clone the repo, run migrations, and log in immediately.
-- All seed users share the password: Password123!
-- (BCrypt hash below is real and verifiable — not a placeholder.)

INSERT INTO customers (id, name, contact_email) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Meridian Facilities Management', 'ops@meridianfm.com'),
    ('22222222-2222-2222-2222-222222222222', 'Brightside Retail Group', 'facilities@brightside.example');

INSERT INTO sites (id, customer_id, name, address) VALUES
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'Meridian HQ Tower', '221 Baker Street, Pune, MH'),
    ('44444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', 'Meridian Warehouse 3', 'Plot 12, MIDC, Pune, MH'),
    ('55555555-5555-5555-5555-555555555555', '22222222-2222-2222-2222-222222222222', 'Brightside Mall - Nashik', 'College Road, Nashik, MH');

-- password for all seed users: Password123!
INSERT INTO users (id, name, email, password_hash, role, customer_id) VALUES
    ('a1111111-aaaa-1111-aaaa-111111111111', 'Divya Kulkarni', 'dispatcher@keystone.dev', '$2b$10$JuXOz./LurIQ7/nv.dk/2uNXOYNnChofMPsO5KR3BlzH9FBqwllTe', 'DISPATCHER', NULL),
    ('a2222222-aaaa-2222-aaaa-222222222222', 'Rahul Shinde', 'technician@keystone.dev', '$2b$10$JuXOz./LurIQ7/nv.dk/2uNXOYNnChofMPsO5KR3BlzH9FBqwllTe', 'TECHNICIAN', NULL),
    ('a3333333-aaaa-3333-aaaa-333333333333', 'Priya Deshpande', 'manager@keystone.dev', '$2b$10$JuXOz./LurIQ7/nv.dk/2uNXOYNnChofMPsO5KR3BlzH9FBqwllTe', 'MANAGER', NULL),
    ('a4444444-aaaa-4444-aaaa-444444444444', 'Meridian Front Desk', 'customer@keystone.dev', '$2b$10$JuXOz./LurIQ7/nv.dk/2uNXOYNnChofMPsO5KR3BlzH9FBqwllTe', 'CUSTOMER', '11111111-1111-1111-1111-111111111111');

INSERT INTO parts (id, name, sku, unit_cost, stock_qty) VALUES
    ('b1111111-bbbb-1111-bbbb-111111111111', 'HVAC Air Filter (20x20)', 'HVAC-FLT-2020', 450.00, 40),
    ('b2222222-bbbb-2222-bbbb-222222222222', 'Copper Pipe 1/2" (per meter)', 'PLB-CU-050', 180.00, 120),
    ('b3333333-bbbb-3333-bbbb-333333333333', 'Circuit Breaker 32A', 'ELE-CB-32A', 620.00, 25);

INSERT INTO work_orders (id, code, title, description, priority, status, customer_id, site_id, assigned_to, sla_due_at, created_at, updated_at) VALUES
    ('c1111111-cccc-1111-cccc-111111111111', 'WO-1001', 'AC unit not cooling on 3rd floor', 'Tenants reporting warm air from vents since this morning.', 'HIGH', 'ASSIGNED', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'a2222222-aaaa-2222-aaaa-222222222222', NOW() + INTERVAL 20 HOUR, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 1 HOUR),
    ('c2222222-cccc-2222-cccc-222222222222', 'WO-1002', 'Leaking pipe under warehouse sink', NULL, 'MEDIUM', 'NEW', '11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', NULL, NOW() + INTERVAL 72 HOUR, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 1 HOUR),
    ('c3333333-cccc-3333-cccc-333333333333', 'WO-1003', 'Breaker tripping in electrical room', 'Happens intermittently, worse in the afternoon.', 'CRITICAL', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555', 'a2222222-aaaa-2222-aaaa-222222222222', NOW() + INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 30 MINUTE);

INSERT INTO work_order_status_history (id, work_order_id, from_status, to_status, changed_by, changed_at, note) VALUES
    (UUID(), 'c1111111-cccc-1111-cccc-111111111111', NULL, 'NEW', 'a1111111-aaaa-1111-aaaa-111111111111', NOW() - INTERVAL 4 HOUR, 'Raised from tenant call'),
    (UUID(), 'c1111111-cccc-1111-cccc-111111111111', 'NEW', 'ASSIGNED', 'a1111111-aaaa-1111-aaaa-111111111111', NOW() - INTERVAL 1 HOUR, 'Assigned to Rahul'),
    (UUID(), 'c2222222-cccc-2222-cccc-222222222222', NULL, 'NEW', 'a4444444-aaaa-4444-aaaa-444444444444', NOW() - INTERVAL 1 HOUR, 'Raised via customer portal'),
    (UUID(), 'c3333333-cccc-3333-cccc-333333333333', NULL, 'NEW', 'a1111111-aaaa-1111-aaaa-111111111111', NOW() - INTERVAL 2 HOUR, NULL),
    (UUID(), 'c3333333-cccc-3333-cccc-333333333333', 'NEW', 'ASSIGNED', 'a1111111-aaaa-1111-aaaa-111111111111', NOW() - INTERVAL 90 MINUTE, NULL),
    (UUID(), 'c3333333-cccc-3333-cccc-333333333333', 'ASSIGNED', 'IN_PROGRESS', 'a2222222-aaaa-2222-aaaa-222222222222', NOW() - INTERVAL 30 MINUTE, 'On site now');
