-- ============================================
-- Canli fiyatlar panelinde gosterilecek coinler
-- ADA ve XRP ekleniyor; boylece "Canli Fiyatlar" paneli 6 coin gosterir.
-- V4'teki gibi her satir WHERE NOT EXISTS ile korunuyor (symbol'de UNIQUE kisit yok).
-- ============================================

INSERT INTO coins (symbol, name, coingecko_id, is_free_tier_watchlist, created_at, updated_at)
SELECT 'ADA', 'Cardano', 'cardano', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM coins WHERE symbol = 'ADA');

INSERT INTO coins (symbol, name, coingecko_id, is_free_tier_watchlist, created_at, updated_at)
SELECT 'XRP', 'XRP', 'ripple', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM coins WHERE symbol = 'XRP');
