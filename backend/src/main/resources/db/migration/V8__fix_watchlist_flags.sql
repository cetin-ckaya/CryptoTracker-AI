-- ============================================
-- is_free_tier_watchlist bayraklarini duzeltir.
--
-- Sorun: bayraklar tutarsizdi — BTC isaretli DEGILDI, ADA isaretliydi.
-- Oysa "Canli Fiyatlar" paneli BTC, ETH, BNB, SOL, XRP gosteriyor.
--
-- Bu sutun artik iki isi birden yapiyor:
--   1. Panelde hangi coinlerin gosterilecegi
--   2. /api/v1/market ucunun CoinGecko'dan hangi coinlerin 7 gunluk
--      grafigini (sparkline) cekecegi
--
-- Ikincisi onemli: katalog 100 coine cikti ve hepsinin grafigini cekmek
-- 5 coin gosteren bir panel icin gereksiz veri indirmek demekti.
-- ============================================

-- Once hepsini temizle, sonra yalnizca panel coinlerini isaretle.
-- Boylece migration kac kez calisirsa calissin sonuc ayni olur.
UPDATE coins SET is_free_tier_watchlist = FALSE;

UPDATE coins SET is_free_tier_watchlist = TRUE
WHERE symbol IN ('BTC', 'ETH', 'BNB', 'SOL', 'XRP');
