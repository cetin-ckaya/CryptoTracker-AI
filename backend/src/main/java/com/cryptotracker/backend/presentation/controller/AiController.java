package com.cryptotracker.backend.presentation.controller;

import com.cryptotracker.backend.application.dto.response.AiAnalysisResponse;
import com.cryptotracker.backend.application.exception.NotFoundException;
import com.cryptotracker.backend.application.service.AiAnalysisService;
import com.cryptotracker.backend.infrastructure.persistence.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiAnalysisService aiAnalysisService;
    private final UserRepository userRepository;

    public AiController(AiAnalysisService aiAnalysisService, UserRepository userRepository) {
        this.aiAnalysisService = aiAnalysisService;
        this.userRepository = userRepository;
    }

    // JWT'den email al, DB'den userId bul — diger controller'larla ayni pattern
    private Long getAuthenticatedUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"))
                .getId();
    }

    // D003 geregi iki farkli analiz var ve uc, kullanicinin planina gore dallaniyor:
    //   FREE    -> BTC/ETH/SOL icin genel, herkese ayni AL/SAT/TUT yorumu
    //   PREMIUM -> kullanicinin kendi portfoyune ozel analiz
    //
    // Burada @PreAuthorize YOK: ucu tamamen kapatmak FREE kullaniciyi bos
    // ekranla birakiyordu. Kisisellestirilmis analiz yine korunuyor, cunku
    // ona yalnizca ROLE_PREMIUM tasiyan token ile ulasilabiliyor.
    @GetMapping("/analyze")
    public ResponseEntity<AiAnalysisResponse> analyzePortfolio() {
        AiAnalysisResponse analysis = isPremium()
                ? aiAnalysisService.analyzePortfolio(getAuthenticatedUserId())
                : aiAnalysisService.analyzeFreeWatchlist();

        return ResponseEntity.ok(analysis);
    }

    // Rol JWT'nin icinde tasiniyor (JwtAuthenticationFilter authority'ye cevirir).
    // DIKKAT: kullanici PREMIUM'a yukseldiginde elindeki eski token hala
    // ROLE_FREE tasir; yeni analizi gormek icin tekrar giris yapmasi gerekir.
    private boolean isPremium() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> "ROLE_PREMIUM".equals(a.getAuthority()));
    }
}