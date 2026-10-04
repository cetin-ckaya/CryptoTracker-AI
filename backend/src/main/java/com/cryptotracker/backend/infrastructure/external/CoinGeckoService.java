package com.cryptotracker.backend.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.cryptotracker.backend.application.dto.response.CoinMarketResponse;
import com.cryptotracker.backend.domain.model.Coin;
import com.cryptotracker.backend.infrastructure.persistence.CoinRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CoinGeckoService {
    private final RestClient restClient;

    // Onbellege disaridan yazabilmek icin CacheManager'i enjekte ediyoruz.
    // Neden gerekli: @Cacheable yalnizca "metot cagrildi, sonucu sakla" senaryosunu
    // cozer. Bizim ihtiyacimiz ters yonde — scheduler TEK istekle 6 coinin fiyatini
    // birden aliyor ve bunlari getPrice()'in onbellegine tek tek yazmak istiyor.
    private final CacheManager cacheManager;

    // Sembol -> CoinGecko id cevrimi icin: dogru id zaten coins tablosunda.
    private final CoinRepository coinRepository;

    private static final Logger log = LoggerFactory.getLogger(CoinGeckoService.class);

    // SimpleClientHttpRequestFactory: Java'nin eski HttpURLConnection'ini kullanir
    // Spring Boot 4.x'te yeni HttpClient Windows'ta loopback baglantisindan hata verir
    public CoinGeckoService(CacheManager cacheManager, CoinRepository coinRepository) {
        this.cacheManager = cacheManager;
        this.coinRepository = coinRepository;
        this.restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    // CoinGecko'da her coin'in kendine ozgu bir "id"si var ve sembol ile id
    // cogu zaman AYNI DEGIL: BNB -> binancecoin, AVAX -> avalanche-2.
    //
    // Dogru id zaten coins tablosunun coingecko_id sutununda duruyor; once
    // oraya bakiyoruz. Onceden burada elle yazilmis bir switch vardi ve
    // katalog her buyudugunde ayni bilgiyi iki yerde tutmak gerekiyordu —
    // tabloya eklenen ama switch'e eklenmeyen her coin sessizce yanlis
    // id uretiyordu.
    //
    // Tabloda bulunamazsa son care olarak sembolun kucuk harfi denenir.
    private String toCoinGeckoId(String symbol) {
        return coinRepository.findAll().stream()
                .filter(c -> c.getSymbol() != null && c.getSymbol().equalsIgnoreCase(symbol))
                .map(Coin::getCoingeckoId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElseGet(() -> {
                    log.warn("{} icin coins tablosunda coingecko_id yok, sembol kullaniliyor", symbol);
                    return symbol.toLowerCase();
                });
    }

    // Verilen sembol için anlık USD fiyatını döndürür.
    // Örnek: getPrice("BTC") → 95000.00
    @SuppressWarnings("unchecked")
    //Bu anotasyon şunu yapar: symbol parametresini key olarak kullanarak sonucu "prices" adlı cache'e yazar.
    // Aynı symbol ile tekrar çağrılınca metot gövdesi çalışmaz, Redis'ten döner.
    @Cacheable(value = "prices",key = "#symbol")
    public BigDecimal getPrice(String symbol){
        log.info("CoinGecko API called for: {}", symbol);

        String coinId = toCoinGeckoId(symbol);
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId + "&vs_currencies=usd";

        Map<String, Map<String, Object>> response = restClient.get()
                .uri(url)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey(coinId)) {
            return BigDecimal.ZERO;
        }

        Object price = response.get(coinId).get("usd");
        return new BigDecimal(price.toString());
    }
    // Canli fiyatlar paneli icin: fiyat + 24 saatlik degisim + sparkline'i
    // TEK CoinGecko cagrisinda getirir. getPrice()'i coin basina cagirmak
    // 6 coin icin 6 istek demekti; /coins/markets ucu hepsini birlikte donuyor.
    //
    // ids: CoinGecko id listesi (bitcoin, ethereum, ...) — coins tablosundaki
    //      coingecko_id sutunundan gelir.
    @Cacheable(value = "markets", key = "#ids.toString()")
    public List<CoinMarketResponse> getMarkets(List<String> ids) {
        // Onbellekte yoksa buraya dusulur ve gercek istek atilir.
        return fetchMarkets(ids);
    }

    // Onbellege BAKMADAN dogrudan CoinGecko'ya gider.
    // Scheduler bunu kullanir: amaci zaten onbellegi tazelemek oldugu icin
    // onbellekten okumasi anlamsiz olurdu.
    @SuppressWarnings("unchecked")
    public List<CoinMarketResponse> fetchMarkets(List<String> ids) {
        log.info("CoinGecko markets API called for: {}", ids);

        String url = "https://api.coingecko.com/api/v3/coins/markets"
                + "?vs_currency=usd&ids=" + String.join(",", ids)
                + "&order=market_cap_desc&sparkline=true&price_change_percentage=24h";

        List<Map<String, Object>> response = restClient.get()
                .uri(url)
                .retrieve()
                .body(List.class);

        List<CoinMarketResponse> result = new ArrayList<>();
        if (response == null) {
            return result;
        }

        for (Map<String, Object> item : response) {
            CoinMarketResponse coin = new CoinMarketResponse();
            coin.setSymbol(String.valueOf(item.get("symbol")).toUpperCase());
            coin.setName(String.valueOf(item.get("name")));
            coin.setPrice(toBigDecimal(item.get("current_price")));
            coin.setChange24h(toBigDecimal(item.get("price_change_percentage_24h")));
            coin.setIconUrl(item.get("image") == null ? null : String.valueOf(item.get("image")));

            // sparkline_in_7d.price 168 nokta (7 gun x 24 saat) donuyor.
            // Arayuzde 40 piksellik bir cizgi icin bu fazla — 24 noktaya seyreltiyoruz.
            Map<String, Object> spark = (Map<String, Object>) item.get("sparkline_in_7d");
            if (spark != null && spark.get("price") instanceof List<?> raw) {
                coin.setSparkline(downsample(raw, 24));
            } else {
                coin.setSparkline(List.of());
            }

            result.add(coin);
        }
        return result;
    }

    // Listeyi esit arailiklarla hedef sayida noktaya indirger.
    private List<BigDecimal> downsample(List<?> raw, int target) {
        List<BigDecimal> out = new ArrayList<>();
        if (raw.isEmpty()) {
            return out;
        }
        int step = Math.max(1, raw.size() / target);
        for (int i = 0; i < raw.size(); i += step) {
            BigDecimal v = toBigDecimal(raw.get(i));
            if (v != null) {
                out.add(v);
            }
        }
        return out;
    }

    // JSON'dan gelen sayi Integer veya Double olabilir; ikisini de guvenle cevirir.
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString());
    }

    // getPrice()'in onbellegine disaridan tek bir fiyat yazar.
    // DIKKAT: anahtar getPrice()'taki key = "#symbol" ile birebir ayni olmali,
    // yoksa PortfolioService'in getPrice(symbol) cagrisi bu kaydi bulamaz ve
    // bosuna yeni bir HTTP istegi atar. Bu yuzden sembolu oldugu gibi kullan.
    public void cachePrice(String symbol, BigDecimal price) {
        Cache cache = cacheManager.getCache("prices");
        if (cache != null) {
            cache.put(symbol, price);
        }
    }

    // getMarkets()'in onbellegine hazir listeyi yazar.
    // Anahtar getMarkets()'taki key = "#ids.toString()" ile ayni uretilmeli;
    // boylece /api/v1/market ucu scheduler'in cektigi veriyi gorur ve
    // ikinci bir istek atmaz.
    public void cacheMarkets(List<String> ids, List<CoinMarketResponse> markets) {
        Cache cache = cacheManager.getCache("markets");
        if (cache != null) {
            cache.put(ids.toString(), markets);
        }
    }

    //tüm fiyat cache'ini temizler
    @CacheEvict(value = {"prices", "markets"}, allEntries = true)
    public void evictPriceCache(){
    }
}

