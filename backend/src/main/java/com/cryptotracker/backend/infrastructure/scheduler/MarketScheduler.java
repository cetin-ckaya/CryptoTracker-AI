package com.cryptotracker.backend.infrastructure.scheduler;

import com.cryptotracker.backend.application.dto.response.CoinMarketResponse;
import com.cryptotracker.backend.domain.model.Coin;
import com.cryptotracker.backend.domain.model.PortfolioValueHistory;
import com.cryptotracker.backend.infrastructure.external.CoinGeckoService;
import com.cryptotracker.backend.infrastructure.messaging.PriceUpdateMessage;
import com.cryptotracker.backend.infrastructure.messaging.RabbitMQConfig;
import com.cryptotracker.backend.infrastructure.persistence.CoinRepository;
import com.cryptotracker.backend.infrastructure.persistence.PortfolioRepository;
import com.cryptotracker.backend.infrastructure.persistence.PortfolioValueHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MarketScheduler {
    private static final Logger log = LoggerFactory.getLogger(MarketScheduler.class);

    private final CoinGeckoService coinGeckoService;
    private final CoinRepository coinRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioValueHistoryRepository portfolioValueHistoryRepository;
    // WebSocket'e direkt yazmak yerine RabbitMQ'ya gonderiyoruz
    // Consumer (PriceMessageConsumer) kuyruktaki mesaji alip WebSocket'e iletecek
    private final RabbitTemplate rabbitTemplate;

    public MarketScheduler(CoinGeckoService coinGeckoService, CoinRepository coinRepository, PortfolioRepository portfolioRepository, PortfolioValueHistoryRepository portfolioValueHistoryRepository, RabbitTemplate rabbitTemplate) {
        this.coinGeckoService = coinGeckoService;
        this.coinRepository = coinRepository;
        this.portfolioRepository = portfolioRepository;
        this.portfolioValueHistoryRepository = portfolioValueHistoryRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Her 5 dakikada bir calisir.
    //
    // cron yerine fixedDelay kullaniyoruz: cron bir sonraki 5'in katini bekliyordu,
    // yani uygulama 18:01'de baslatilsa fiyatlar 18:05'e kadar bos kaliyordu.
    // initialDelay = 0 ile ilk tur uygulama acilir acilmaz calisir, sonrakiler
    // bir onceki tur bittikten 5 dakika sonra tetiklenir.
    @Scheduled(initialDelay = 0, fixedDelay = 5 * 60 * 1000)
    public void refreshPrice(){
        log.info("Scheduled price refresh started");

        List<Coin> coins = coinRepository.findAll();

        // coingecko_id bos olan satirlar atlanir — o id olmadan CoinGecko'ya sorulamaz.
        // sorted() sart: "markets" onbelleginin anahtari bu listenin metin hali.
        // Scheduler ile MarketController ayni siralamayi uretmezse controller
        // onbellegi iskalar ve gereksiz bir HTTP istegi daha atar.
        List<String> ids = coins.stream()
                .map(Coin::getCoingeckoId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .sorted()
                .toList();

        if (ids.isEmpty()) {
            log.warn("coins tablosunda coingecko_id dolu kayit yok, fiyat guncellemesi atlandi");
            return;
        }

        // TEK HTTP istegi ile tum coinlerin fiyati.
        //
        // Onceki hali coin basina getPrice() cagiriyordu ve mesaji kurarken ayni
        // cagriyi tekrarliyordu: 6 coin icin 12 istek. CoinGecko'nun ucretsiz plani
        // bunu birkac saniyede 429 Too Many Requests ile reddediyordu.
        // /coins/markets ucu hepsini birlikte donuyor — 12 istek 1 oldu.
        List<CoinMarketResponse> markets;
        try {
            markets = coinGeckoService.fetchMarkets(ids);
        } catch (Exception e) {
            // Zamanlanmis gorev asla istisna firlatmamali: karsisinda bir istemci yok,
            // sadece log'a duser. Dis servisin cokmesi beklenen bir durum, bu yuzden
            // stack trace degil tek satir uyari yaziyoruz.
            //
            // Onbellege hic dokunmadan cikiyoruz — boylece son bilinen fiyatlar
            // Redis'te yerinde kalir ve ekran bosalmaz.
            log.warn("Fiyat guncellemesi atlandi, CoinGecko'ya ulasilamadi: {}", e.getMessage());
            return;
        }

        if (markets.isEmpty()) {
            log.warn("CoinGecko bos liste dondu, onbellek korundu");
            return;
        }

        // /api/v1/market ucunun gordugu onbellegi tazele
        coinGeckoService.cacheMarkets(ids, markets);

        // Sembol -> fiyat sozlugu. CoinGecko sembolu buyuk harf doner.
        Map<String, BigDecimal> priceBySymbol = new HashMap<>();
        for (CoinMarketResponse m : markets) {
            if (m.getSymbol() != null && m.getPrice() != null) {
                priceBySymbol.put(m.getSymbol().toUpperCase(), m.getPrice());
            }
        }

        for (Coin coin : coins) {
            String symbol = coin.getSymbol();
            BigDecimal price = priceBySymbol.get(symbol.toUpperCase());

            if (price == null) {
                log.warn("CoinGecko {} icin fiyat dondurmedi, atlandi", symbol);
                continue;
            }

            // getPrice()'in onbellegini doldur — PortfolioService bu sayede
            // portfoy sorgusunda HTTP istegi atmaz, Redis'ten okur.
            coinGeckoService.cachePrice(symbol, price);

            // Fiyati kuyruğa gonder — Consumer WebSocket'e iletecek.
            // Tek bir coinin kuyruk hatasi digerlerini durdurmasin.
            try {
                rabbitTemplate.convertAndSend(RabbitMQConfig.PRICE_QUEUE,
                        new PriceUpdateMessage(symbol, price));
            } catch (Exception e) {
                log.warn("{} fiyati kuyruğa gonderilemedi: {}", symbol, e.getMessage());
            }

            log.info("Price refreshed for: {} = {}", symbol, price);
        }

        log.info("Scheduled price refresh completed ({} coin, 1 HTTP istegi)", priceBySymbol.size());
    }

    // Her saatin basinda calisir — tum portfolylerin anlık degerini DB'ye kaydeder
    // Dashboard'da "gecen haftaya gore nasil degisti" grafigi icin kullanilir
    @Scheduled(cron = "0 0 * * * *")
    public void savePortfolioHistory(){
        log.info("Portfolio history save started");

        portfolioRepository.findAll().forEach(portfolio -> {
            // Bir portfoyde cikan hata digerlerinin kaydini engellemesin.
            try {
                // Her holding icin: guncel fiyat x miktar = anlık deger.
                // Fiyatlar refreshPrice() tarafindan onbellege yazildigi icin
                // bu cagri normalde Redis'ten doner, HTTP istegi atmaz.
                BigDecimal totalValue = portfolio.getHoldings().stream()
                        .map(holding -> coinGeckoService.getPrice(holding.getCoin().getSymbol())
                                .multiply(holding.getQuantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Snapshot olustur ve kaydet
                PortfolioValueHistory history = new PortfolioValueHistory();
                history.setPortfolio(portfolio);
                history.setTotalValue(totalValue);
                history.setRecordedAt(LocalDateTime.now());

                portfolioValueHistoryRepository.save(history);
                log.info("Portfolio history saved for portfolioId: {}", portfolio.getId());
            } catch (Exception e) {
                // Bilerek kayit ATMIYORUZ. Fiyat alinamadiginda totalValue = 0
                // yazmak, Gunluk Kar/Zarar hesabinin 24 saat onceki referansini
                // sifirlar ve ekranda tamami kar gibi gorunur. Eksik kayit,
                // yanlis kayittan iyidir.
                log.warn("portfolioId {} icin anlık deger kaydedilemedi: {}",
                        portfolio.getId(), e.getMessage());
            }
        });
        log.info("Portfolio history save completed");
    }
}
