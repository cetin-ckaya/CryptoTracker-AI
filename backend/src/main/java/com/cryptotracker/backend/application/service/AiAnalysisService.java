package com.cryptotracker.backend.application.service;

import com.cryptotracker.backend.application.dto.response.AiAnalysisResponse;
import com.cryptotracker.backend.application.dto.response.AiSignalResponse;
import com.cryptotracker.backend.application.exception.NotFoundException;
import com.cryptotracker.backend.domain.model.Portfolio;
import com.cryptotracker.backend.infrastructure.external.CoinGeckoService;
import com.cryptotracker.backend.infrastructure.persistence.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private static final String MODEL = "openai/gpt-oss-20b";

    // D003: FREE kullaniciya gosterilen sabit izleme listesi.
    private static final List<String> FREE_WATCHLIST = List.of("BTC", "ETH", "SOL");

    @Value("${groq.api.key}")
    private String apiKey;

    private final PortfolioRepository portfolioRepository;
    private final CoinGeckoService coinGeckoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // CoinGeckoService ile ayni yaklasim — Windows loopback sorununu onler
    private final RestClient restClient = RestClient.builder()
            .requestFactory(new SimpleClientHttpRequestFactory())
            .build();

    public AiAnalysisService(PortfolioRepository portfolioRepository, CoinGeckoService coinGeckoService) {
        this.portfolioRepository = portfolioRepository;
        this.coinGeckoService = coinGeckoService;
    }

    // ---- FREE: sabit izleme listesi, herkese ayni cikti (D003) ----
    public AiAnalysisResponse analyzeFreeWatchlist() {
        String varliklar = FREE_WATCHLIST.stream()
                .map(s -> s + " (guncel fiyat " + coinGeckoService.getPrice(s) + " USD)")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        AiAnalysisResponse response = ask(varliklar, false);
        response.setPersonalized(false);
        return response;
    }

    // ---- PREMIUM: kullanicinin kendi portfoyu ----
    public AiAnalysisResponse analyzePortfolio(Long userId) {
        Portfolio portfolio = portfolioRepository.findByUserIdWithHoldings(userId)
                .orElseThrow(() -> new NotFoundException("Portfolio not found"));

        if (portfolio.getHoldings().isEmpty()) {
            AiAnalysisResponse bos = new AiAnalysisResponse();
            bos.setHeadline("Portfoyunuzde henuz varlik yok");
            bos.setSummary("Ilk isleminizi ekledikten sonra analiz burada gorunecek.");
            bos.setModel(MODEL);
            bos.setGeneratedAt(LocalDateTime.now());
            bos.setSignals(List.of());
            bos.setPersonalized(true);
            return bos;
        }

        String varliklar = portfolio.getHoldings().stream()
                .map(h -> h.getCoin().getSymbol()
                        + " (" + h.getQuantity() + " adet, guncel fiyat "
                        + coinGeckoService.getPrice(h.getCoin().getSymbol()) + " USD)")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        AiAnalysisResponse response = ask(varliklar, true);
        response.setPersonalized(true);
        return response;
    }

    // ---- Groq cagrisi ----
    //
    // response_format = json_object: Groq'u gecerli JSON dondurmeye zorlar.
    // Bu olmadan model cevabin basina "Tabii, iste analiz:" gibi bir cumle
    // ekleyebiliyor ve ayristirma patliyordu.
    private AiAnalysisResponse ask(String varliklar, boolean kisisel) {
        String baglam = kisisel
                ? "Bu kullanicinin kendi portfoyu; yorumu portfoy dengesini de dikkate alarak yap."
                : "Genel bir piyasa degerlendirmesi yap, kisisellestirme yapma.";

        String prompt = "Varliklar: " + varliklar + ".\n"
                + baglam + "\n"
                + "Yanitini SADECE su JSON semasiyla ver, baska hicbir metin ekleme:\n"
                + "{\n"
                + "  \"headline\": \"tek cumlelik genel durum, en fazla 60 karakter\",\n"
                + "  \"summary\": \"2-3 cumlelik genel degerlendirme\",\n"
                + "  \"signals\": [\n"
                + "    {\n"
                + "      \"symbol\": \"BTC\",\n"
                + "      \"action\": \"AL veya SAT veya TUT\",\n"
                + "      \"target\": \"kisa hedef veya seviye aciklamasi\",\n"
                + "      \"confidence\": 75,\n"
                + "      \"comment\": \"bu varlik icin 1-2 cumlelik gerekce\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n"
                + "Her varlik icin bir signals ogesi olmali. confidence 0-100 arasi tam sayi. Turkce yaz.";

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system",
                                "content", "Sen deneyimli bir kripto analistsin. Yalnizca gecerli JSON dondur."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> raw = restClient.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) raw.get("choices");
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        return parse(content);
    }

    // Modelin dondurdugu JSON'u DTO'ya cevirir.
    // Ayristirma basarisiz olursa ekrani bos birakmak yerine ham metni
    // rawText'e koyuyoruz; arayuz onu duz yazi olarak gosteriyor.
    private AiAnalysisResponse parse(String content) {
        AiAnalysisResponse response = new AiAnalysisResponse();
        response.setModel(MODEL);
        response.setGeneratedAt(LocalDateTime.now());
        response.setSignals(List.of());

        try {
            JsonNode root = objectMapper.readTree(content);
            response.setHeadline(text(root, "headline"));
            response.setSummary(text(root, "summary"));

            List<AiSignalResponse> signals = new ArrayList<>();
            JsonNode arr = root.get("signals");
            if (arr != null && arr.isArray()) {
                for (JsonNode n : arr) {
                    AiSignalResponse s = new AiSignalResponse();
                    String symbol = text(n, "symbol");
                    s.setSymbol(symbol == null ? null : symbol.toUpperCase());
                    s.setAction(normalizeAction(text(n, "action")));
                    s.setTarget(text(n, "target"));
                    s.setComment(text(n, "comment"));

                    JsonNode c = n.get("confidence");
                    s.setConfidence(c == null || !c.isNumber() ? null
                            : Math.max(0, Math.min(100, c.asInt())));

                    // Fiyati modele SORMUYORUZ — uydurabilir.
                    // Kendi onbellegimizden okuyoruz.
                    if (s.getSymbol() != null) {
                        try {
                            BigDecimal p = coinGeckoService.getPrice(s.getSymbol());
                            s.setPrice(p);
                        } catch (Exception e) {
                            log.warn("{} fiyati alinamadi: {}", s.getSymbol(), e.getMessage());
                        }
                    }
                    signals.add(s);
                }
            }
            response.setSignals(signals);
        } catch (Exception e) {
            log.warn("AI yaniti JSON olarak cozulemedi, ham metne dusuldu: {}", e.getMessage());
            response.setRawText(content);
            response.setHeadline("Analiz hazir");
        }
        return response;
    }

    // Model bazen "Al", "SATIS", "hold" gibi varyasyonlar dondurebiliyor.
    private String normalizeAction(String action) {
        if (action == null) {
            return "TUT";
        }
        String a = action.trim().toUpperCase();
        if (a.startsWith("AL") || a.startsWith("BUY")) {
            return "AL";
        }
        if (a.startsWith("SAT") || a.startsWith("SELL")) {
            return "SAT";
        }
        return "TUT";
    }

    private String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return n == null || n.isNull() ? null : n.asString();
    }
}
