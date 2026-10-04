package com.cryptotracker.backend.presentation.controller;

import com.cryptotracker.backend.application.dto.response.CoinResponse;
import com.cryptotracker.backend.infrastructure.persistence.CoinRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Coin katalogunu disariya acan read-only controller.
// Kullanici bazli veri icermedigi icin getAuthenticatedUserId'ye gerek yok;
// yine de SecurityConfig geregi istek JWT ile gelmek zorunda.
@RestController
@RequestMapping("/api/v1/coins")
public class CoinController {

    private final CoinRepository coinRepository;

    public CoinController(CoinRepository coinRepository) {
        this.coinRepository = coinRepository;
    }

    // GET /api/v1/coins            -> tum coinler
    // GET /api/v1/coins?search=bt  -> sembol veya isimde "bt" gecenler
    //
    // @RequestParam(required = false): parametre gonderilmezse null gelir,
    // o durumda filtre uygulanmaz.
    @GetMapping
    public ResponseEntity<List<CoinResponse>> getCoins(
            @RequestParam(required = false) String search) {

        List<CoinResponse> coins = coinRepository.findAll().stream()
                .filter(coin -> matches(coin.getSymbol(), coin.getName(), search))
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(coins);
    }

    // Arama bos ise her kayit gecer; doluysa sembol veya isimde aranir.
    // toLowerCase ile buyuk/kucuk harf duyarsiz karsilastirma yapiyoruz.
    private boolean matches(String symbol, String name, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String q = search.toLowerCase();
        return (symbol != null && symbol.toLowerCase().contains(q))
                || (name != null && name.toLowerCase().contains(q));
    }

    // Entity -> DTO. Katalog kucuk oldugu icin MapStruct yerine elle yaziyoruz.
    private CoinResponse toResponse(com.cryptotracker.backend.domain.model.Coin coin) {
        CoinResponse response = new CoinResponse();
        response.setId(coin.getId());
        response.setSymbol(coin.getSymbol());
        response.setName(coin.getName());
        response.setIconUrl(coin.getIconUrl());
        return response;
    }
}
