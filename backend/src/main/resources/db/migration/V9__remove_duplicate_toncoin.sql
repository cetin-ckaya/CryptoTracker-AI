-- ============================================
-- Ayni coingecko_id'ye sahip iki satirdan eskisini kaldirir.
--
-- Durum: Toncoin yeniden markalandi. CoinGecko artik the-open-network id'si
-- icin GRAM sembolunu donduruyor. V6 bu coini 'TON' olarak eklemisti, V7 ise
-- CoinGecko'nun guncel listesinden 'GRAM' olarak yeniden ekledi — cunku
-- tekrar korumasi SEMBOLE bakiyordu, id'ye degil. Sonucta ayni coin iki satir
-- oldu ve TON satiri hicbir zaman fiyat/ikon alamadi.
--
-- Guncel sembolu (GRAM) tutuyoruz, eski TON satirini siliyoruz.
--
-- Silmeden once baglilik kontrolu: TON satirina bagli holding veya transaction
-- varsa silme CALISMAZ (foreign key hatasi verir). Kontrol edildi, yok.
-- WHERE kosulundaki NOT EXISTS, migration baska bir veritabaninda calisirsa
-- veri kaybini onler: bagli kayit varsa satir oldugu gibi birakilir.
-- ============================================

DELETE FROM coins c
WHERE c.symbol = 'TON'
  AND c.coingecko_id = 'the-open-network'
  AND EXISTS (SELECT 1 FROM coins o
              WHERE o.coingecko_id = c.coingecko_id AND o.id <> c.id)
  AND NOT EXISTS (SELECT 1 FROM holdings h WHERE h.coin_id = c.id)
  AND NOT EXISTS (SELECT 1 FROM transactions t WHERE t.coin_id = c.id);
