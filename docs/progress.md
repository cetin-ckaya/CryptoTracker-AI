# CryptoTracker v2 - Progress Log

Roadmap kaynağı: `AI Destekli_plan1.pdf` (~150 günlük plan), kapsam sapmaları için bkz. `decisions.md`.
Bu dosya, aktif olarak neredeyiz sorusuna hızlı cevap vermek için oturum sonlarında güncellenir.

## Durum Özeti (2026-08-26)

**Faz:** Phase 1 — Monolitik Backend Geliştirme
**Sprint:** Sprint 10 — Performans & Final (Gun 77-80) ✅ TAMAMLANDI
**Şu an:** Phase 1 tamamlandi — Phase 3 React Frontend basliyor

### Tamamlanan
- [x] Gün 1-3 (Phase 0): requirements.md, decisions.md, ER diyagramı, repo + ilk commit
- [x] Gün 4-6: domain/application/infrastructure paket yapısı, BaseEntity
- [x] Gün 7-9: Tüm JPA entity'ler + Flyway V1__init.sql
- [x] Gün 10-12: Tüm Repository interface'leri (temel query'ler)
- [x] Gün 13-15: DTO'lar (Request/Response), MapStruct mapper'lar, Bean Validation
- [x] Gün 16-19: REST Controller'lar (AuthController, PortfolioController) + stub service'ler — Postman testleri geçti
- [x] Gün 20-23: Spring Security + JWT (JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig, UserService.login/register) — tüm auth testleri geçti
- [x] Gün 24-26: Global Exception Handling (GlobalExceptionHandler, BusinessException, NotFoundException, ErrorResponse) — 400/404/409/500 testleri geçti

- [x] Gün 27-35: PortfolioService gerçek implementasyonu (holding ekle/çıkar, kar/zarar hesabı) + CoinGecko fiyat entegrasyonu
- [x] Gün 35 (ek): TransactionService + TransactionController — POST/GET /api/v1/transactions, Postman testleri geçti
- [x] Gün 34-36: Redis Cache — Docker Redis, @Cacheable CoinGecko fiyatlari, @CacheEvict — cache testi gecti
- [x] Gün 37-39: Scheduled Tasks — @EnableScheduling, MarketScheduler (5 dk fiyat), saatlik portfoy gecmisi
- [x] Gün 40-42: WebSocket & Real-Time — WebSocketConfig, /topic/prices kanali, SecurityConfig /ws/** acildi
- [ ] Gün 43-44: Alarm Sistemi — D002 karari geregi ATLANDI
- [x] Gün 45-47: Swagger UI (springdoc-openapi) + Actuator /health /metrics — Swagger'da test edildi
- [x] Gün 48-55: Docker multi-stage Dockerfile + docker-compose (app/postgres/redis) — build ve calistirma testi gecti
- [x] Gün 56-59: Subscription sistemi — SubscriptionService, SubscriptionController, JWT role claim, @EnableMethodSecurity
- [x] Gün 60-63: Groq AI entegrasyonu — AiAnalysisService (openai/gpt-oss-20b), AiController (@PreAuthorize PREMIUM), OpenApiConfig (Swagger JWT Authorize butonu) — Swagger'da 200 OK test edildi

- [x] Gun 64-66: Raporlama Sistemi — D008 karari geregi ASKIYA ALINDI
- [x] Gun 67-69: Kullanici Davranis Takibi — BehaviorTrackingService (direkt servis cagrisi, AOP yerine), user_behavior_events tablosu, PostgreSQL'de dogrulandi
- [x] Gun 70-73: RabbitMQ entegrasyonu — docker-compose rabbitmq servisi, RabbitMQConfig, PriceUpdateMessage, PriceMessageConsumer, MarketScheduler RabbitTemplate ile guncellendi
- [x] Gun 74-76: Guvenlik — CORS (localhost:3000), Security Headers (X-Frame-Options, X-Content-Type-Options, HSTS), SecurityConfig duzeltildi

- [x] Gun 77-78: Performans — V3__add_indexes.sql (7 index), TransactionRepository @EntityGraph ile N+1 cozumu
- [x] Gun 79-80: Load Testing (JMeter: 50 kullanici, 250 istek, 0 hata, ort. 36ms), bilingual README (EN/TR)

### Sıradaki adım
Sprint 11 — Phase 3: React Frontend (Gun 81+)

### Gelecek Sprint'ler (Plan Ozeti)

**Sprint 9 — Event-Driven & Guvenlik (Gun 70-76)**
- Gun 70-73: RabbitMQ entegrasyonu — docker-compose, @RabbitListener
- Gun 74-76: Gelismis guvenlik — CORS, Security Headers, OWASP Top 10, Dependency Check

**Sprint 10 — Performans & Final (Gun 77-80)**
- Gun 77-78: Performans — Database indexing, N+1 cozumu, @EntityGraph
- Gun 79-80: Load Testing, README.md hazirlanmasi

**Phase 3 — React Frontend (Gun 81-100)**
- Gun 81-83: Vite + React Router kurulumu, Dashboard layout
- Gun 84-86: Axios + TanStack Query, Login/Register, JWT yonetimi
- Gun 87-92: Recharts ile grafik dashboard (LineChart, PieChart)
- Gun 93-95: Portfoy & Islem sayfalari, Modal, DataTable
- Gun 96-97: WebSocket + STOMP.js real-time UI
- Gun 98-100: AI Analiz sayfasi, react-markdown, Vercel/Netlify deploy

**Phase 4 — Microservice Donusumu (Gun 101-120)**
- Gun 101-104: User/Portfolio/Market-Service ayrisimi (DDD)
- Gun 105-107: Spring Cloud Gateway, JWT dogrulama gateway'e tasima
- Gun 108-110: Eureka Server, Config Server
- Gun 111-113: OpenFeign, RabbitMQ inter-service iletisim
- Gun 114-117: Micrometer, Zipkin, Prometheus, Grafana
- Gun 118-120: GitHub Actions CI/CD, docker-compose guncelleme

**Phase 5 — Kendi ML Modeliniz (Gun 121-150)**
- Gun 121-124: Python + FastAPI, /predict endpoint, Docker
- Gun 125-128: Pandas, user_behavior_events veri hazirligi
- Gun 129-133: scikit-learn Content-Based model, pickle
- Gun 134-137: FastAPI Serving, model.pkl deploy
- Gun 138-142: Java WebClient → FastAPI entegrasyonu, Groq fallback
- Gun 143-150: End-to-End test, demo senaryosu, CV/Portfolio sunum dosyasi

## Kapsam Notları (docs/decisions.md ile senkron)
- Tek portföy modeli (D001) — DB seviyesinde `Portfolio.user_id UNIQUE` ile zaten uygulanmış.
- Alarm sistemi kapsam dışı (D002).
- AI freemium ayrımı (D003) — FREE sabit izleme listesi 3 coin (BTC/ETH/SOL) olarak teyit edildi (2026-08-20).
- Rate limiting kapsam dışı (D005).
- Raporlama sistemi askiya alindi (D008) — proje bitince degerlendirilecek.
