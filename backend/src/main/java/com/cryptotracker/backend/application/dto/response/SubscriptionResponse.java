package com.cryptotracker.backend.application.dto.response;

import com.cryptotracker.backend.domain.model.SubscriptionTier;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Abonelik bilgisinin API'ye acilan hali.
//
// NEDEN GEREKLI: Controller eskiden Subscription ENTITY'sini dondururdu.
// O entity'nin icinde @OneToOne User var ve User'da passwordHash alani
// bulunuyor. Jackson entity'yi serilestirince BCrypt hash'i de JSON'a
// yaziyordu — yani /api/v1/subscription cagiran her oturum kendi parola
// hash'ini goruyordu.
//
// Bu DTO yalnizca gereken alanlari tasir; user iliskisi disarida kalir.
@Getter
@Setter
public class SubscriptionResponse {

    private Long id;
    private SubscriptionTier tier;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
}
