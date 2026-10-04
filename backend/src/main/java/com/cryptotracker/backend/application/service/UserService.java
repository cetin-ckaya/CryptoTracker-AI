package com.cryptotracker.backend.application.service;

import com.cryptotracker.backend.application.dto.UserMapper;
import com.cryptotracker.backend.application.dto.request.CreateUserRequest;
import com.cryptotracker.backend.application.dto.request.LoginRequest;
import com.cryptotracker.backend.application.dto.response.AuthResponse;
import com.cryptotracker.backend.application.dto.response.UserResponse;
import com.cryptotracker.backend.application.exception.BusinessException;
import com.cryptotracker.backend.application.exception.NotFoundException;
import com.cryptotracker.backend.domain.model.Portfolio;
import com.cryptotracker.backend.domain.model.Subscription;
import com.cryptotracker.backend.domain.model.User;
import com.cryptotracker.backend.infrastructure.persistence.PortfolioRepository;
import com.cryptotracker.backend.infrastructure.persistence.UserRepository;
import com.cryptotracker.backend.infrastructure.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// @Service: Spring'e "bu sınıf bir iş mantığı bileşenidir, bean olarak kaydet" der.
// Controller bu sınıfı @Autowired veya constructor injection ile kullanacak.
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final SubscriptionService subscriptionService;


    public UserService(UserRepository userRepository, PortfolioRepository portfolioRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, UserMapper userMapper, SubscriptionService subscriptionService) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
        this.subscriptionService = subscriptionService;
    }

    // Oturumdaki kullanicinin bilgisi — /api/v1/auth/me icin.
    // Entity degil UserResponse donuyoruz: User icinde passwordHash var,
    // entity dogrudan JSON'a cevrilirse hash disari sizar.
    public UserResponse getByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public UserResponse register(CreateUserRequest request) {
        // Aynı email ile kayıt varsa hata fırlat (DB'de unique constraint var ama
        // önceden kontrol etmek daha anlamlı bir hata mesajı üretir).
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Email already in use: " + request.getEmail());
        }

        // Şifreyi asla düz metin saklama — BCrypt ile hash'le.
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        User savedUser = userRepository.save(user);

        // D001: Kayıt olunca otomatik portföy oluştur.
        Portfolio portfolio = new Portfolio();
        portfolio.setUser(savedUser);
        portfolioRepository.save(portfolio);

        return userMapper.toResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new NotFoundException("Invalid credentials");
        }
        Subscription subscription = subscriptionService.getOrCreateSubscription(user.getId());
        String role = "ROLE_" + subscription.getTier().name();
        String token = jwtTokenProvider.generateToken(user.getEmail(), role);
        return new AuthResponse(token, user.getEmail(), user.getFullName());
    }

}