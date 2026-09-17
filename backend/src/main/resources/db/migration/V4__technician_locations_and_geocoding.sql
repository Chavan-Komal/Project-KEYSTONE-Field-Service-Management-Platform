-- Adds map/geolocation support: site coordinates (geocoded from each site's
-- existing address) and a technician "base location" — a home-base address
-- a manager sets when provisioning the technician account (Section 03: staff
-- accounts are manager-provisioned). Together these back the nearest-
-- technician dispatch suggestion and the manager/customer tracking map.

ALTER TABLE sites ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE sites ADD COLUMN longitude DOUBLE PRECISION;

ALTER TABLE users ADD COLUMN base_address VARCHAR(300);
ALTER TABLE users ADD COLUMN base_latitude DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN base_longitude DOUBLE PRECISION;

-- Backfill coordinates for the seed sites/technician so the map and
-- nearest-technician features work immediately, without waiting on a live
-- geocoding call against old rows.
UPDATE sites SET latitude = 18.5204, longitude = 73.8567
    WHERE id = '33333333-3333-3333-3333-333333333333'; -- Meridian HQ Tower, Pune
UPDATE sites SET latitude = 18.6298, longitude = 73.8131
    WHERE id = '44444444-4444-4444-4444-444444444444'; -- Meridian Warehouse 3, MIDC Pune
UPDATE sites SET latitude = 19.9975, longitude = 73.7898
    WHERE id = '55555555-5555-5555-5555-555555555555'; -- Brightside Mall, Nashik

UPDATE users SET base_address = 'Shivaji Nagar, Pune, MH', base_latitude = 18.5308, base_longitude = 73.8474
    WHERE id = 'a2222222-aaaa-2222-aaaa-222222222222'; -- Rahul Shinde, technician
