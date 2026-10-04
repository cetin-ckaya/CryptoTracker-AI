package com.cryptotracker.backend.application.dto.response;

import lombok.Getter;
import lombok.Setter;

// Coin katalogu — islem ekleme ekranindaki arama kutusunu besler.
// Entity yerine DTO donuyoruz: Coin entity'si ileride ic alanlar kazanirsa
// (ornegin coingecko_id gibi dis sisteme ozel bilgiler) API sozlesmesi bozulmasin.
@Getter
@Setter
public class CoinResponse {

    private Long id;        // transactions endpoint'i coinId bekliyor
    private String symbol;  // BTC, ETH gibi
    private String name;    // Bitcoin, Ethereum gibi
    private String iconUrl; // arayuzde ikon gostermek icin (su an bos olabilir)
}
