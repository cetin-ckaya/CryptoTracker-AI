package com.cryptotracker.backend.application.dto.response;

import com.cryptotracker.backend.domain.model.SubscriptionTier;
import lombok.Getter;
import lombok.Setter;

// Plan degisikligi (yukseltme / dusurme) yaniti.
//
// NEDEN TOKEN DONUYORUZ: yetki JWT'nin icinde tasiniyor. Veritabaninda tier
// PREMIUM olsa bile kullanicinin elindeki eski token hala ROLE_FREE diyor;
// yani "Yukselt"e basan kullanici cikis yapip tekrar girene kadar hicbir
// degisiklik gormuyordu.
//
// Bu yuzden plan degisiminde yeni rolu tasiyan taze bir token uretip
// donuyoruz; arayuz eskisinin yerine yaziyor ve degisiklik aninda etkili
// oluyor. Alternatifi kisa omurlu token + refresh token mekanizmasiydi,
// bu olcekte gereginden buyuk kalirdi.
@Getter
@Setter
public class SubscriptionChangeResponse {

    private SubscriptionTier tier;
    private String token;

    public SubscriptionChangeResponse(SubscriptionTier tier, String token) {
        this.tier = tier;
        this.token = token;
    }
}
