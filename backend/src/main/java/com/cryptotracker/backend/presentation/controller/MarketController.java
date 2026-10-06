package com.cryptotracker.backend.presentation.controller;

import com.cryptotracker.backend.application.dto.response.CoinMarketResponse;
import com.cryptotracker.backend.infrastructure.external.CoinGeckoService;
import com.cryptotracker.backend.infrastructure.persistence.CoinRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Canli piyasa verisi. WebSocket yalnizca 5 dakikada bir yayin yaptigi icin
// sayfa ilk acildiginda ekran bos kaliyordu; bu uc, o anda Redis'teki
// guncel veriyi hemen dondurerek o bosluğu kapatir.
@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final CoinGeckoService coinGeckoService;
    private final CoinRepository coinRepository;

    public MarketController(CoinGeckoService coinGeckoService, CoinRepository coinRepository) {
        this.coinGeckoService = coinGeckoService;
        this.coinRepository = coinRepository;
    }

    // GET /api/v1/market
    // Takip edilen tum coinlerin fiyati, 24 saatlik degisimi ve sparkline'i.
    @GetMapping
    public ResponseEntity<List<CoinMarketResponse>> getMarket() {
        // Yalnizca izleme listesindeki coinler (is_free_tier_watchlist).
        // Katalogda 100 coin var ama panel 5 tanesini gosteriyor; hepsinin
        // 7 gunluk grafigini cekmek gereksiz veri indirmekti.
        //
        // coingecko_id bos olan satirlar atlanir — o id olmadan sorulamaz.
        // sorted() sart: onbellek anahtari bu listenin metin hali oldugu icin
        // MarketScheduler ile ayni siralama uretilmeli, yoksa scheduler'in
        // yazdigi kayit bulunamaz ve bosuna yeni istek atilir.
        List<String> ids = coinRepository.findByIsFreeTierWatchlistTrue().stream()
                .map(coin -> coin.getCoingeckoId())
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .sorted()
                .toList();

        if (ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(coinGeckoService.getMarkets(ids));
    }
}
