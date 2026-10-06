package com.cryptotracker.backend.presentation.controller;

import com.cryptotracker.backend.application.exception.NotFoundException;
import com.cryptotracker.backend.application.service.SubscriptionService;
import com.cryptotracker.backend.application.service.UserService;
import com.cryptotracker.backend.application.dto.response.SubscriptionChangeResponse;
import com.cryptotracker.backend.application.dto.response.SubscriptionResponse;
import com.cryptotracker.backend.infrastructure.security.JwtTokenProvider;
import com.cryptotracker.backend.domain.model.Subscription;
import com.cryptotracker.backend.infrastructure.persistence.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public SubscriptionController(SubscriptionService subscriptionService, UserRepository userRepository,
                                 JwtTokenProvider jwtTokenProvider) {
        this.subscriptionService = subscriptionService;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // JWT'den email al, DB'den userId bul — diger controller'larla ayni pattern
    private Long getAuthenticatedUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"))
                .getId();
    }

    // Kullanicinin mevcut subscription tier'ini dondurur
    @GetMapping()
    public ResponseEntity<SubscriptionResponse> getSubscription(){
        return ResponseEntity.ok(
                toResponse(subscriptionService.getOrCreateSubscription(getAuthenticatedUserId())));
    }

    // Kullaniciyi PREMIUM'a yukselttir.
    //
    // Yanitta TAZE TOKEN var: yetki JWT'nin icinde tasindigi icin yeni rolu
    // tasiyan bir token uretilmezse kullanici cikis yapip tekrar girene kadar
    // premium ozellikleri goremezdi.
    @PostMapping("/upgrade")
    public ResponseEntity<SubscriptionChangeResponse> upgradeToPremium(){
        return ResponseEntity.ok(planDegistir(
                subscriptionService.upgradeToPremium(getAuthenticatedUserId())));
    }

    // PREMIUM'dan FREE'ye don
    @PostMapping("/downgrade")
    public ResponseEntity<SubscriptionChangeResponse> downgradeToFree(){
        return ResponseEntity.ok(planDegistir(
                subscriptionService.downgradeToFree(getAuthenticatedUserId())));
    }

    // Plan degisikliginden sonra yeni rolu tasiyan token uretir.
    // Token uretimi UserService.login ile ayni mantik: ROLE_ + tier adi.
    private SubscriptionChangeResponse planDegistir(Subscription subscription) {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String role = "ROLE_" + subscription.getTier().name();
        String token = jwtTokenProvider.generateToken(email, role);
        return new SubscriptionChangeResponse(subscription.getTier(), token);
    }

    // Entity -> DTO.
    // Entity'yi dogrudan dondurmek, icindeki User iliskisi uzerinden
    // passwordHash'i de JSON'a tasiyordu. Alanlari elle seciyoruz.
    private SubscriptionResponse toResponse(Subscription subscription) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(subscription.getId());
        response.setTier(subscription.getTier());
        response.setStartedAt(subscription.getStartedAt());
        response.setExpiresAt(subscription.getExpiresAt());
        return response;
    }
}
