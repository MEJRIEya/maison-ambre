-- Demo data. Loaded by Flyway after the schema, but not in the automated tests
-- (they only use db/migration and create their own data).
-- Brand, boutiques, products, people and prices are fictional.

INSERT INTO boutique (id, code, name, city, currency) VALUES
    (1, 'DXB', 'Maison Ambre Dubai',      'Dubai',      'AED'),
    (2, 'PAR', 'Maison Ambre Paris',      'Paris',      'EUR'),
    (3, 'CAS', 'Maison Ambre Casablanca', 'Casablanca', 'MAD');

INSERT INTO product (id, sku, name, family, size_ml, description) VALUES
    (1,  'MA-001', 'Ambre Nuit',      'oriental', 100, 'Warm amber, vanilla and labdanum for long evenings.'),
    (2,  'MA-002', 'Rose de Minuit',  'floral',    50, 'Dark Damask rose with a touch of black pepper.'),
    (3,  'MA-003', 'Cèdre Blanc',     'woody',    100, 'Clean Atlas cedar softened with white musk.'),
    (4,  'MA-004', 'Fleur d''Oranger', 'floral',   50, 'Orange blossom picked at dawn, bright and honeyed.'),
    (5,  'MA-005', 'Oud Royal',       'oriental',  75, 'Rich oud with saffron and smoked leather.'),
    (6,  'MA-006', 'Santal Doré',     'woody',    100, 'Creamy sandalwood with a hint of fig.'),
    (7,  'MA-007', 'Jasmin Sambac',   'floral',    50, 'Lush jasmine with green tea leaves.'),
    (8,  'MA-008', 'Vétiver Sauvage', 'woody',    100, 'Earthy vetiver and grapefruit zest.'),
    (9,  'MA-009', 'Encens Sacré',    'oriental',  75, 'Frankincense and myrrh over dry woods.'),
    (10, 'MA-010', 'Iris Poudré',     'floral',    50, 'Powdery iris and violet, soft and elegant.'),
    (11, 'MA-011', 'Bois d''Argan',   'woody',    100, 'Toasted argan wood with cardamom.'),
    (12, 'MA-012', 'Safran Ambré',    'oriental', 100, 'Saffron, rose and amber in a golden blend.');

-- Local prices and stock. Casablanca does not sell Iris Poudré (product 10).
-- A few items are at or below the low-stock threshold (5) and Paris is out of Oud Royal.
INSERT INTO inventory (boutique_id, product_id, price, stock) VALUES
    (1, 1,  590.00, 18), (1, 2,  490.00, 12), (1, 3,  450.00,  9), (1, 4,  390.00, 20),
    (1, 5,  980.00,  3), (1, 6,  530.00, 14), (1, 7,  470.00, 11), (1, 8,  430.00,  7),
    (1, 9,  650.00, 10), (1, 10, 510.00,  6), (1, 11, 550.00, 15), (1, 12, 710.00,  8),

    (2, 1,  145.00, 10), (2, 2,  120.00,  4), (2, 3,  110.00, 16), (2, 4,   95.00, 12),
    (2, 5,  240.00,  0), (2, 6,  130.00,  9), (2, 7,  115.00, 13), (2, 8,  105.00, 11),
    (2, 9,  160.00,  5), (2, 10, 125.00, 14), (2, 11, 135.00,  8), (2, 12, 175.00,  7),

    (3, 1, 1590.00, 12), (3, 2, 1320.00,  9), (3, 3, 1210.00, 10), (3, 4, 1050.00, 17),
    (3, 5, 2650.00,  6), (3, 6, 1430.00, 11), (3, 7, 1270.00,  8), (3, 8, 1150.00, 13),
    (3, 9, 1760.00,  9), (3, 11, 1490.00, 7), (3, 12, 1930.00, 2);

-- Prospects with the boutique where they were first seen.
-- Ines (6) was first seen in Paris but placed her first order in Dubai.
INSERT INTO prospect (id, email, first_seen_boutique_id, created_at) VALUES
    (1,  'sara.haddad@example.com',    1, '2026-10-01 10:15:00+04'),
    (2,  'omar.khalil@example.com',    1, '2026-10-10 18:40:00+04'),
    (3,  'lina.farouk@example.com',    1, '2026-11-06 12:05:00+04'),
    (4,  'camille.martin@example.com', 2, '2026-10-02 11:20:00+02'),
    (5,  'julien.bernard@example.com', 2, '2026-11-05 16:45:00+01'),
    (6,  'ines.moreau@example.com',    2, '2026-10-18 09:30:00+02'),
    (7,  'yasmine.alaoui@example.com', 3, '2026-10-14 15:10:00+01'),
    (8,  'karim.bennani@example.com',  3, '2026-10-27 19:55:00+01'),
    (9,  'nadia.tazi@example.com',     3, '2026-11-08 13:25:00+01'),
    (10, 'hugo.petit@example.com',     2, '2026-10-29 10:00:00+01');

-- Orders. The currency is taken from the boutique and the total is computed from the lines below.
INSERT INTO orders (id, boutique_id, prospect_id, status, currency, total, created_at, completed_at, cancelled_at)
SELECT o.id, o.boutique_id, o.prospect_id, o.status, b.currency, 0,
       o.created_at::timestamptz,
       CASE WHEN o.status = 'COMPLETED' THEN o.created_at::timestamptz END,
       CASE WHEN o.status = 'CANCELLED' THEN o.created_at::timestamptz + INTERVAL '2 hours' END
FROM (VALUES
    (1,  1, 1,  'COMPLETED', '2026-10-05 19:00:00+04'),
    (2,  1, 2,  'COMPLETED', '2026-10-12 17:30:00+04'),
    (3,  1, 1,  'COMPLETED', '2026-10-20 20:10:00+04'),
    (4,  1, 3,  'PENDING',   '2026-11-08 11:00:00+04'),
    (5,  2, 4,  'COMPLETED', '2026-10-08 14:00:00+02'),
    (6,  2, 5,  'PENDING',   '2026-11-07 10:30:00+01'),
    (7,  1, 6,  'COMPLETED', '2026-10-25 16:20:00+04'),
    (8,  2, 10, 'CANCELLED', '2026-10-30 12:00:00+01'),
    (9,  3, 7,  'COMPLETED', '2026-10-15 18:45:00+01'),
    (10, 3, 8,  'COMPLETED', '2026-10-28 11:15:00+01'),
    (11, 3, 9,  'PENDING',   '2026-11-09 17:00:00+01'),
    (12, 2, 4,  'COMPLETED', '2026-11-02 15:40:00+01')
) AS o (id, boutique_id, prospect_id, status, created_at)
JOIN boutique b ON b.id = o.boutique_id;

-- Lines take the unit price from the boutique's inventory.
INSERT INTO order_line (order_id, product_id, quantity, unit_price, line_total)
SELECT l.order_id, l.product_id, l.quantity, i.price, i.price * l.quantity
FROM (VALUES
    (1, 1, 1), (1, 5, 1),
    (2, 3, 2),
    (3, 1, 1), (3, 7, 1),
    (4, 2, 1),
    (5, 2, 1), (5, 4, 1),
    (6, 6, 1),
    (7, 5, 1),
    (8, 3, 1),
    (9, 9, 1), (9, 12, 1),
    (10, 11, 2),
    (11, 4, 1),
    (12, 2, 1)
) AS l (order_id, product_id, quantity)
JOIN orders o ON o.id = l.order_id
JOIN inventory i ON i.boutique_id = o.boutique_id AND i.product_id = l.product_id;

UPDATE orders o SET total = (SELECT SUM(line_total) FROM order_line WHERE order_id = o.id);

-- Clients follow from the data: a prospect with at least one completed order,
-- converted at the date of that first completed order.
INSERT INTO client (prospect_id, converted_at)
SELECT prospect_id, MIN(completed_at)
FROM orders
WHERE status = 'COMPLETED'
GROUP BY prospect_id;

-- Explicit ids were used above, so move the identity sequences past them.
SELECT setval(pg_get_serial_sequence('boutique', 'id'),   (SELECT MAX(id) FROM boutique));
SELECT setval(pg_get_serial_sequence('product', 'id'),    (SELECT MAX(id) FROM product));
SELECT setval(pg_get_serial_sequence('inventory', 'id'),  (SELECT MAX(id) FROM inventory));
SELECT setval(pg_get_serial_sequence('prospect', 'id'),   (SELECT MAX(id) FROM prospect));
SELECT setval(pg_get_serial_sequence('orders', 'id'),     (SELECT MAX(id) FROM orders));
SELECT setval(pg_get_serial_sequence('order_line', 'id'), (SELECT MAX(id) FROM order_line));
