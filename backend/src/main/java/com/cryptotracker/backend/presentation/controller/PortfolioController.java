package com.cryptotracker.backend.presentation.controller;


import com.cryptotracker.backend.application.dto.request.AddHoldingRequest;
import com.cryptotracker.backend.application.dto.response.HoldingResponse;
import com.cryptotracker.backend.application.dto.response.PortfolioResponse;
import com.cryptotracker.backend.application.dto.response.PortfolioValuePointResponse;
import com.cryptotracker.backend.application.exception.NotFoundException;
import com.cryptotracker.backend.application.service.BehaviorTrackingService;
import com.cryptotracker.backend.application.service.PortfolioService;
import com.cryptotracker.backend.infrastructure.persistence.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {
    private final PortfolioService portfolioService;
    private final UserRepository userRepository;
    private final BehaviorTrackingService behaviorTrackingService;

    public PortfolioController(PortfolioService portfolioService, UserRepository userRepository, BehaviorTrackingService behaviorTrackingService) {
        this.portfolioService = portfolioService;
        this.userRepository = userRepository;
        this.behaviorTrackingService = behaviorTrackingService;
    }

    // JWT filter email'i SecurityContext'e koymuştu — buradan okuyoruz
    private Long getAuthenticatedUserId() {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"))
                .getId();
    }


    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio(){
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        behaviorTrackingService.track(email, "GET_PORTFOLIO");
        return ResponseEntity.ok(portfolioService.getPortfolio(getAuthenticatedUserId()));
    }

    // GET /api/v1/portfolio/history?range=1G
    // Grafigin ustundeki 1G / 1H / 1A / 3A / 1Y / Tumu butonlari buraya baglanir.
    @GetMapping("/history")
    public ResponseEntity<List<PortfolioValuePointResponse>> getHistory(
            @RequestParam(defaultValue = "1G") String range) {
        return ResponseEntity.ok(portfolioService.getValueHistory(getAuthenticatedUserId(), range));
    }

    @PostMapping("/holdings")
    public ResponseEntity<HoldingResponse> addHolding(@RequestBody AddHoldingRequest request){
        return ResponseEntity.status(201).body(portfolioService.addHolding(getAuthenticatedUserId(), request));
    }

    @DeleteMapping("/holdings/{holdingId}")
    public ResponseEntity<Void> removeHolding(@PathVariable Long holdingId){
        portfolioService.removeHolding(getAuthenticatedUserId(), holdingId);
        return ResponseEntity.noContent().build();
    }
}

