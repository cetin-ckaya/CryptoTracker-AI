-- ============================================
-- Satis islemlerinde GERCEKLESEN kar/zarari saklar.
--
-- Sorun: ortalama maliyet yonteminde satis, holding'in ortalama alis fiyatini
-- degistirmez; yalnizca miktari azaltir. Dogru davranis bu, ama satistan elde
-- edilen kar hicbir yere yazilmadigi icin kayboluyordu.
--
-- Ornek: 0,2 BTC, ortalama maliyeti 24.244,90 iken 80.000'den satildi.
-- 11.151 dolarlik kazanc ne holding'de ne de baska bir yerde goruluyordu.
-- Sonucta totalProfitLoss yalnizca ELDEKI varliklarin gerceklesmemis
-- kar/zararini gosteriyordu; "toplam ne kazandim" sorusu cevapsizdi.
--
-- NULL kalabilir: alim islemlerinde gerceklesen kar/zarar kavrami yoktur.
-- Bu yuzden sutun nullable, varsayilan degeri de yok.
--
-- NUMERIC(20,8): para alanlarinda projenin geri kalaniyla ayni hassasiyet.
-- ============================================

ALTER TABLE transactions
    ADD COLUMN realized_profit_loss NUMERIC(20,8);

COMMENT ON COLUMN transactions.realized_profit_loss IS
    'Yalnizca SELL islemlerinde dolu: (satis fiyati - ortalama alis maliyeti) x miktar';
