package com.cryptotracker.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

// Canli fiyatlar panelinin ihtiyac duydugu her sey tek nesnede.
// Serializable: Redis varsayilan olarak Java serilestirmesi kullaniyor,
// bu sinif cache'e yazilacagi icin zorunlu.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoinMarketResponse implements Serializable {

    // CoinGecko'nun kendi id'si. Eslestirme SEMBOL yerine bununla yapilir:
    // semboller degisebiliyor (Toncoin -> Gram ornegi), id'ler sabit.
    private String coingeckoId;

    private String symbol;              // BTC
    private String name;                // Bitcoin
    private BigDecimal price;           // anlik fiyat (USD)
    private BigDecimal change24h;       // son 24 saatteki degisim yuzdesi
    private String iconUrl;             // CoinGecko'nun kendi ikon adresi
    private List<BigDecimal> sparkline; // son 7 gunun seyrelmis fiyat noktalari
}
