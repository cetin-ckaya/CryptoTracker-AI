package com.cryptotracker.backend.application.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

// Kullanıcının portföy dashboard verisi — HoldingResponse listesi içerir.
// "Toplam Portföy Değeri" ve kar/zarar hesaplaması service katmanında
// yapılıp bu DTO'ya set edilecek (Sprint 3'te).
@Getter
@Setter
public class PortfolioResponse {

    private Long id;

    // Her coin pozisyonu bir HoldingResponse olarak listeye girer.
    // Dashboard'daki "Portföy Varlıklarım" tablosu bu listeden beslenir.
    private List<HoldingResponse> holdings;

    // Tüm holdinglerin anlık değerlerinin toplamı.
    // CoinGecko'dan çekilen güncel fiyat × quantity formülüyle hesaplanır.
    private BigDecimal totalValue;

    // Tüm holdinglerin (averageBuyPrice × quantity) toplamı — yatırılan tutar.
    private BigDecimal totalInvested;

    // totalValue - totalInvested = kar ise pozitif, zarar ise negatif.
    private BigDecimal totalProfitLoss;

    // Satilmis pozisyonlardan elde edilen kazanc.
    // totalProfitLoss gerceklesmemis (eldeki varliklar), bu ise gerceklesmis.
    private BigDecimal realizedProfitLoss;

    private BigDecimal dailyProfitLoss;

    private BigDecimal dailyProfitLossPercentage;

    // Yüzde olarak kar/zarar: (totalProfitLoss / totalInvested) * 100
    private BigDecimal totalProfitLossPercentage;
}