-- Seed data for local development

-- Riders
INSERT INTO users (id, phone, name) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '+919876543210', 'Rider Alice'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', '+919876543211', 'Rider Bob'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', '+919876543212', 'Rider Charlie')
ON CONFLICT (id) DO NOTHING;

-- Driver users
INSERT INTO users (id, phone, name) VALUES
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21', '+919876543220', 'Driver Dave'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', '+919876543221', 'Driver Eve'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a23', '+919876543222', 'Driver Frank')
ON CONFLICT (id) DO NOTHING;

-- Drivers
INSERT INTO drivers (id, user_id, license_no, rating, is_available, current_lat, current_lng) VALUES
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a31', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21', 'KA01DL1234', 4.85, true, 12.9716, 77.5946),
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a32', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'KA01DL5678', 4.92, true, 12.9352, 77.6245),
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a23', 'KA01DL9012', 4.70, false, 12.9542, 77.5845)
ON CONFLICT (id) DO NOTHING;

-- Vehicles
INSERT INTO vehicles (id, driver_id, model, plate, capacity) VALUES
    ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a41', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a31', 'Maruti Swift', 'KA01AB1234', 4),
    ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a42', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a32', 'Hyundai i20', 'KA01CD5678', 4),
    ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a43', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'Toyota Innova', 'KA01EF9012', 7)
ON CONFLICT (id) DO NOTHING;

-- Wallets
INSERT INTO wallets (id, driver_id, balance_cents) VALUES
    ('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a51', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a31', 150000),
    ('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a52', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a32', 230000),
    ('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a53', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 85000)
ON CONFLICT (id) DO NOTHING;
