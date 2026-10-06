package com.cryptotracker.backend.application.service;

import com.cryptotracker.backend.application.exception.NotFoundException;
import com.cryptotracker.backend.domain.model.Subscription;
import com.cryptotracker.backend.domain.model.SubscriptionTier;
import com.cryptotracker.backend.infrastructure.persistence.SubscriptionRepository;
import com.cryptotracker.backend.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    // Kullanicinin subscription'ini getirir — yoksa FREE tier ile otomatik olusturur
    public Subscription getOrCreateSubscription(Long userId){
        return subscriptionRepository.findByUserId(userId).orElseGet(() -> {
            Subscription sub = new Subscription();
            sub.setUser(userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found")));
            sub.setTier(SubscriptionTier.FREE);
            sub.setStartedAt(LocalDateTime.now());
            return subscriptionRepository.save(sub);
        });
    }

    // Kullanicinin tier'ini PREMIUM yapar ve DB'ye kaydeder
    public Subscription upgradeToPremium(Long userId){
        return changeTier(userId, SubscriptionTier.PREMIUM);
    }

    // PREMIUM'dan FREE'ye doner.
    //
    // Neden var: plan degisiminin iki yonu de denenebilmeli. Yalnizca
    // yukseltme olsaydi bir kez PREMIUM olan kullanici ucretsiz ekrani
    // bir daha goremezdi.
    public Subscription downgradeToFree(Long userId){
        return changeTier(userId, SubscriptionTier.FREE);
    }

    private Subscription changeTier(Long userId, SubscriptionTier tier){
        Subscription sub = getOrCreateSubscription(userId);
        sub.setTier(tier);
        return subscriptionRepository.save(sub);
    }
}
