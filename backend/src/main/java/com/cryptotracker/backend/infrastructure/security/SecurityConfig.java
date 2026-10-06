package com.cryptotracker.backend.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS: React frontend localhost:3000'den gelen isteklere izin ver
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    // Cookie veya Authorization header'i credentials olarak sayilir
                    config.setAllowCredentials(true);
                    return config;
                }))
                // Guvenlik basliklarini ekle — tarayici saldirilarini engeller
                .headers(headers -> headers
                        // Clickjacking'e karsi: sayfamiz baska bir iframe icine alınamaz
                        .frameOptions(frame -> frame.deny())
                        // MIME sniffing'e karsi: tarayici Content-Type'i degistirmesin
                        .contentTypeOptions(Customizer.withDefaults())
                        // HSTS: tarayiciyi her zaman HTTPS kullanmaya zorla (1 yil)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true))
                )
                // JWT kullandığımız için session tutmuyoruz — her istek kendi token'ını taşır.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoint'leri herkese acik — token olmadan erisilebilir.
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Piyasa verisi herkese acik: icinde kullaniciya ait hicbir
                        // bilgi yok, CoinGecko'nun zaten herkese acik fiyatlari.
                        // Giris ekranindaki fiyat seritleri bu uctan besleniyor;
                        // kapali oldugunda ziyaretci hicbir zaman dolmayan kutular
                        // goruyordu.
                        //
                        // Dis servise yuk binmiyor: yanit Redis'ten geliyor,
                        // onbellegi 5 dakikada bir scheduler tazeliyor.
                        .requestMatchers(HttpMethod.GET, "/api/v1/market").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                        // Diger her endpoint token zorunlu.
                        .anyRequest().authenticated()
                )
                // JWT filter'ı, Spring'in kendi UsernamePasswordAuthenticationFilter'ından ONCE calistir.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // BCrypt bean — UserService'te şifre hash'lemek için inject edilecek.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}