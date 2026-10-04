-- ============================================
-- Coin katalogunu genisletir.
--
-- Sorun: coins tablosunda yalnizca 6 kayit vardi; "Islem Ekle" penceresindeki
-- arama bu tabloya baktigi icin kullanici BTC/ETH/BNB/SOL/ADA/XRP disinda
-- hicbir coini ekleyemiyor, "coin bulunamadi" aliyordu.
--
-- Her satir WHERE NOT EXISTS ile korunuyor: symbol sutununda UNIQUE kisit yok,
-- migration tekrar calistirilirsa kopya kayit olusmasin.
--
-- coingecko_id degerleri CoinGecko'nun kendi id'leri. Sembol ile id cogu zaman
-- ayni DEGIL (BNB -> binancecoin, AVAX -> avalanche-2); yanlis id girilirse
-- o coin icin fiyat bos doner, uygulama yine calisir.
--
-- is_free_tier_watchlist: FREE kullaniciya gosterilen sabit liste (D003).
-- Yalnizca BTC/ETH/SOL icin TRUE; digerleri katalogda arama icin duruyor.
-- ============================================

INSERT INTO coins (symbol, name, coingecko_id, is_free_tier_watchlist, created_at, updated_at)
SELECT v.symbol, v.name, v.gecko_id, FALSE, NOW(), NOW()
FROM (VALUES
    ('USDT', 'Tether',            'tether'),
    ('USDC', 'USD Coin',          'usd-coin'),
    ('DOGE', 'Dogecoin',          'dogecoin'),
    ('TRX',  'TRON',              'tron'),
    ('AVAX', 'Avalanche',         'avalanche-2'),
    ('LINK', 'Chainlink',         'chainlink'),
    ('DOT',  'Polkadot',          'polkadot'),
    ('TON',  'Toncoin',           'the-open-network'),
    ('SHIB', 'Shiba Inu',         'shiba-inu'),
    ('LTC',  'Litecoin',          'litecoin'),
    ('BCH',  'Bitcoin Cash',      'bitcoin-cash'),
    ('UNI',  'Uniswap',           'uniswap'),
    ('ATOM', 'Cosmos',            'cosmos'),
    ('XLM',  'Stellar',           'stellar'),
    ('ETC',  'Ethereum Classic',  'ethereum-classic'),
    ('FIL',  'Filecoin',          'filecoin'),
    ('HBAR', 'Hedera',            'hedera-hashgraph'),
    ('APT',  'Aptos',             'aptos'),
    ('ARB',  'Arbitrum',          'arbitrum'),
    ('OP',   'Optimism',          'optimism'),
    ('NEAR', 'NEAR Protocol',     'near'),
    ('VET',  'VeChain',           'vechain'),
    ('ICP',  'Internet Computer', 'internet-computer'),
    ('AAVE', 'Aave',              'aave'),
    ('GRT',  'The Graph',         'the-graph'),
    ('ALGO', 'Algorand',          'algorand'),
    ('SAND', 'The Sandbox',       'the-sandbox'),
    ('MANA', 'Decentraland',      'decentraland'),
    ('AXS',  'Axie Infinity',     'axie-infinity'),
    ('XTZ',  'Tezos',             'tezos'),
    ('INJ',  'Injective',         'injective-protocol'),
    ('SUI',  'Sui',               'sui'),
    ('SEI',  'Sei',               'sei-network'),
    ('PEPE', 'Pepe',              'pepe'),
    ('IMX',  'Immutable',         'immutable-x'),
    ('CRV',  'Curve DAO',         'curve-dao-token'),
    ('MKR',  'Maker',             'maker'),
    ('LDO',  'Lido DAO',          'lido-dao'),
    ('STX',  'Stacks',            'blockstack'),
    ('FLOW', 'Flow',              'flow'),
    ('CAKE', 'PancakeSwap',       'pancakeswap-token'),
    ('CHZ',  'Chiliz',            'chiliz'),
    ('ZEC',  'Zcash',             'zcash'),
    ('DASH', 'Dash',              'dash'),
    ('SNX',  'Synthetix',         'havven'),
    ('ENJ',  'Enjin Coin',        'enjincoin'),
    ('BAT',  'Basic Attention',   'basic-attention-token'),
    ('KSM',  'Kusama',            'kusama'),
    ('NEO',  'NEO',               'neo'),
    ('ZIL',  'Zilliqa',           'zilliqa'),
    ('GALA', 'Gala',              'gala'),
    ('YFI',  'yearn.finance',     'yearn-finance'),
    ('EOS',  'EOS',               'eos'),
    ('THETA','Theta Network',     'theta-token'),
    ('RUNE', 'THORChain',         'thorchain')
) AS v(symbol, name, gecko_id)
WHERE NOT EXISTS (SELECT 1 FROM coins c WHERE c.symbol = v.symbol);
