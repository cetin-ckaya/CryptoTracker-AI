-- ============================================
-- Coin katalogunu 100'e tamamlar.
--
-- Bu dosyadaki id'ler ELLE YAZILMADI: CoinGecko'nun
-- /coins/markets?order=market_cap_desc ucundan alindi, yani kaynagin
-- kendi id'leri. V6'daki 61 kaydin id'leri de ayni ucla ve
-- /simple/price ile tek tek dogrulandi; hatali cikan olmadi.
--
-- Secim piyasa degeri sirasina gore: tabloda zaten bulunan semboller
-- atlanarak ilk 39 yeni coin eklendi, toplam 100 oldu.
--
-- is_free_tier_watchlist FALSE: bunlar katalogda arama icin duruyor.
-- "Canli Fiyatlar" panelindeki sabit liste (D003) arayuzde belirleniyor.
--
-- WHERE NOT EXISTS: symbol'de UNIQUE kisit yok, migration tekrar
-- calisirsa kopya olusmasin.
-- ============================================

INSERT INTO coins (symbol, name, coingecko_id, is_free_tier_watchlist, created_at, updated_at)
SELECT v.symbol, v.name, v.gecko_id, FALSE, NOW(), NOW()
FROM (VALUES
    ('FIGR_HELOC', 'Figure Heloc', 'figure-heloc'),
    ('HYPE', 'Hyperliquid', 'hyperliquid'),
    ('XMR', 'Monero', 'monero'),
    ('WBT', 'WhiteBIT Coin', 'whitebit'),
    ('USDS', 'USDS', 'usds'),
    ('LEO', 'LEO Token', 'leo-token'),
    ('RAIN', 'Rain', 'rain'),
    ('USDE', 'Ethena USDe', 'ethena-usde'),
    ('CC', 'Canton', 'canton-network'),
    ('DAI', 'Dai', 'dai'),
    ('USD1', 'USD1', 'usd1-wlfi'),
    ('GRAM', 'Gram (prev. Toncoin)', 'the-open-network'),
    ('QNT', 'Quant', 'quant-network'),
    ('TAO', 'Bittensor', 'bittensor'),
    ('XAUT', 'Tether Gold', 'tether-gold'),
    ('CRO', 'Cronos', 'crypto-com-chain'),
    ('USDG', 'Global Dollar', 'global-dollar'),
    ('PUMP', 'Pump.fun', 'pump-fun'),
    ('PYUSD', 'PayPal USD', 'paypal-usd'),
    ('BTW', 'Bitway', 'bitway'),
    ('OKB', 'OKB', 'okb'),
    ('RLUSD', 'Ripple USD', 'ripple-usd'),
    ('ONDO', 'Ondo', 'ondo-finance'),
    ('ENA', 'Ethena', 'ethena'),
    ('USYC', 'Circle USYC', 'hashnote-usyc'),
    ('M', 'MemeCore', 'memecore'),
    ('USDY', 'Ondo US Dollar Yield', 'ondo-us-dollar-yield'),
    ('WLD', 'Worldcoin', 'worldcoin-wld'),
    ('BUIDL', 'BlackRock USD Institutional Digital Liquidity Fund', 'blackrock-usd-institutional-digital-liquidity-fund'),
    ('MNT', 'Mantle', 'mantle'),
    ('SKY', 'Sky', 'sky'),
    ('ASTER', 'Aster', 'aster-2'),
    ('MORPHO', 'Morpho', 'morpho'),
    ('PAXG', 'PAX Gold', 'pax-gold'),
    ('WLFI', 'World Liberty Financial', 'world-liberty-financial'),
    ('USDF', 'Falcon USD', 'falcon-finance'),
    ('U', 'United Stables', 'united-stables'),
    ('EURSAFO', 'Spiko Amundi Overnight Swap Fund (EUR)', 'spiko-amundi-overnight-swap-fund-eur'),
    ('HTX', 'HTX DAO', 'htx-dao')
) AS v(symbol, name, gecko_id)
WHERE NOT EXISTS (SELECT 1 FROM coins c WHERE c.symbol = v.symbol);
