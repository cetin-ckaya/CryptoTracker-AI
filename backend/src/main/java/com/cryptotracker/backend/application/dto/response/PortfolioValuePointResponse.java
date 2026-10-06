package com.cryptotracker.backend.application.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Portfoy deger grafiginin tek bir noktasi.
// Entity'yi (PortfolioValueHistory) dogrudan dondurmuyoruz: icinde Portfolio
// iliskisi var, JSON'a cevrilirken kullanicinin tum varliklarini da sizdirirdi.
@Getter
@Setter
public class PortfolioValuePointResponse {

    private LocalDateTime recordedAt;
    private BigDecimal totalValue;

    public PortfolioValuePointResponse(LocalDateTime recordedAt, BigDecimal totalValue) {
        this.recordedAt = recordedAt;
        this.totalValue = totalValue;
    }
}
