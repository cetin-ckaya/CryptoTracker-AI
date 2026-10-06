package com.cryptotracker.backend.application.service;

import com.cryptotracker.backend.application.dto.PortfolioMapper;
import com.cryptotracker.backend.application.dto.request.AddHoldingRequest;
import com.cryptotracker.backend.application.dto.response.HoldingResponse;
import com.cryptotracker.backend.application.dto.response.PortfolioResponse;
import com.cryptotracker.backend.application.exception.NotFoundException;
import com.cryptotracker.backend.application.exception.BusinessException;
import com.cryptotracker.backend.domain.model.Coin;
import com.cryptotracker.backend.domain.model.Holding;
import com.cryptotracker.backend.domain.model.Portfolio;
import com.cryptotracker.backend.application.dto.response.PortfolioValuePointResponse;
import com.cryptotracker.backend.domain.model.PortfolioValueHistory;
import com.cryptotracker.backend.infrastructure.external.CoinGeckoService;
import com.cryptotracker.backend.infrastructure.persistence.CoinRepository;
import com.cryptotracker.backend.infrastructure.persistence.HoldingRepository;
import com.cryptotracker.backend.infrastructure.persistence.PortfolioRepository;
import com.cryptotracker.backend.infrastructure.persistence.PortfolioValueHistoryRepository;
import com.cryptotracker.backend.infrastructure.persistence.TransactionRepository;
import com.cryptotracker.backend.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final CoinRepository coinRepository;
    private final UserRepository userRepository;
    private final PortfolioMapper portfolioMapper;
    private final CoinGeckoService coinGeckoService;
    // Gunluk kar/zarar icin 24 saat onceki portfoy degeri buradan okunur.
    // Tabloyu MarketScheduler.savePortfolioHistory() saat basi dolduruyor.
    private final PortfolioValueHistoryRepository portfolioValueHistoryRepository;
    private final TransactionRepository transactionRepository;

    public PortfolioService(PortfolioRepository portfolioRepository, HoldingRepository holdingRepository, CoinRepository coinRepository, UserRepository userRepository, PortfolioMapper portfolioMapper, CoinGeckoService coinGeckoService, PortfolioValueHistoryRepository portfolioValueHistoryRepository, TransactionRepository transactionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.coinRepository = coinRepository;
        this.userRepository = userRepository;
        this.portfolioMapper = portfolioMapper;
        this.coinGeckoService = coinGeckoService;
        this.portfolioValueHistoryRepository = portfolioValueHistoryRepository;
        this.transactionRepository = transactionRepository;
    }


    public PortfolioResponse getPortfolio(Long userId){
        // Kullanıcının portföyünü bul — yoksa 404
        Portfolio portfolio = portfolioRepository.findByUserIdWithHoldings(userId)
                .orElseThrow(() -> new NotFoundException("Portfolio Not Found"));

        // MapStruct ile DTO'ya çevir
        PortfolioResponse response = portfolioMapper.toPortfolioResponse(portfolio);

        // Her holding için: anlık fiyat × miktar = güncel değer
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;

        for(Holding holding : portfolio.getHoldings()){
            BigDecimal currentPrice = guncelFiyat(holding);
            BigDecimal currentValue = currentPrice.multiply(holding.getQuantity());
            BigDecimal invested = holding.getAverageBuyPrice().multiply(holding.getQuantity());

            totalValue = totalValue.add(currentValue);
            totalInvested = totalInvested.add(invested);
        }
        BigDecimal profitLoss = totalValue.subtract(totalInvested);
        BigDecimal profitLossPercentage = totalInvested.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        response.setTotalValue(totalValue);
        response.setTotalInvested(totalInvested);
        response.setTotalProfitLoss(profitLoss);
        response.setTotalProfitLossPercentage(profitLossPercentage);

        // --- Gunluk kar/zarar ---
        // totalProfitLoss "aldigindan bu yana" kumulatif kar/zarar.
        // Gunluk olan ise: simdiki deger - 24 saat onceki deger.
        applyDailyChange(response, portfolio.getId(), totalValue);

        // Gerceklesen kar/zarar: satilmis pozisyonlardan elde edilen kazanc.
        // totalProfitLoss yalnizca ELDEKI varliklarin gerceklesmemis kar/zarari;
        // alinip satilan her seyin kazanci onun disinda kaliyordu.
        response.setRealizedProfitLoss(
                transactionRepository.totalRealizedProfitLoss(portfolio.getId()));

        return response;
    }

    // Holding'in guncel birim fiyati.
    //
    // getPrice() normalde Redis'ten doner (scheduler 5 dakikada bir doldurur).
    // Ama onbellek bossa gercek bir HTTP istegi atilir ve CoinGecko
    // erisilemezse ISTISNA FIRLATIR. Onceden bu istisna disari ciktigi icin
    // tum portfoy sorgusu 500 donuyordu: tek bir coinin fiyati alinamadigi
    // icin kullanici portfoyunu hic goremiyordu.
    //
    // Fiyat alinamadiginda ortalama alis fiyatini kullaniyoruz. Boylece o
    // varlik "kar/zarar yok" gorunur. Sifir yazmak daha kotu olurdu:
    // varlik yokmus gibi davranir ve portfoy degerini oldugundan dusuk
    // gosterirdi; kullanici parasini kaybettigini sanabilirdi.
    private BigDecimal guncelFiyat(Holding holding) {
        String symbol = holding.getCoin().getSymbol();
        try {
            BigDecimal price = coinGeckoService.getPrice(symbol);
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                return price;
            }
            log.warn("{} icin fiyat bos dondu, ortalama alis fiyati kullanildi", symbol);
        } catch (Exception e) {
            log.warn("{} fiyati alinamadi ({}), ortalama alis fiyati kullanildi",
                    symbol, e.getMessage());
        }
        return holding.getAverageBuyPrice();
    }

    // 24 saat onceki snapshot'a gore gunluk degisimi hesaplar ve response'a yazar.
    //
    // Tam 24 saat once kaydedilmis bir satir olmayabilir (scheduler saat basi calisiyor,
    // uygulama kapaliyken hic calismiyor). Bu yuzden "24 saat oncesinden daha eski
    // kayitlar icinde en yenisi" aliniyor — yani mevcut en yakin snapshot.
    //
    // Hic snapshot yoksa alanlar null birakilir; frontend bunu "Son 24 saatlik kayit
    // bekleniyor" olarak gosterir. Sifir yazmak yaniltici olurdu: "degisim yok" ile
    // "veri yok" ayni sey degil.
    private void applyDailyChange(PortfolioResponse response, Long portfolioId, BigDecimal totalValue) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        Optional<PortfolioValueHistory> snapshot = portfolioValueHistoryRepository
                .findFirstByPortfolioIdAndRecordedAtBeforeOrderByRecordedAtDesc(portfolioId, twentyFourHoursAgo);

        if (snapshot.isEmpty()) {
            return; // dailyProfitLoss ve dailyProfitLossPercentage null kalir
        }

        BigDecimal previousValue = snapshot.get().getTotalValue();

        // PARA AKISI DUZELTMESI.
        //
        // Ham fark (simdiki deger - onceki deger) performans olcmez, cunku
        // kullanicinin kendi alim satimini da iceriyor: coin alinca portfoy
        // degeri artar ve kar gibi gorunur, satinca duser ve zarar gibi gorunur.
        //
        // Gercek ornek: 0,2 BTC satildiginda portfoyden 17.240 dolarlik varlik
        // cikti ve ekran 14.198 dolar ZARAR gosterdi. Oysa para nakde donmustu,
        // kayip yoktu; akis duzeltilince sonuc +1.789 dolar KAR cikiyor.
        //
        // Pencere snapshot'in alindigi andan simdiye kadar — "son 24 saat" degil,
        // cunku karsilastirmanin tabani o snapshot.
        BigDecimal netCashFlow = transactionRepository
                .netCashFlowSince(portfolioId, snapshot.get().getRecordedAt());

        BigDecimal dailyProfitLoss = totalValue
                .subtract(previousValue)
                .subtract(netCashFlow);

        response.setDailyProfitLoss(dailyProfitLoss);

        // Sifira bolme korumasi: eski deger 0 ise yuzde tanimsizdir, null birak.
        // BigDecimal'da == veya equals yerine compareTo kullanilir; equals scale'i de
        // karsilastirir (0 ile 0.00 esit sayilmaz), compareTo sadece degere bakar.
        if (previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        // BigDecimal'da divide() olcek + yuvarlama verilmezse bolum sonsuz basamakli
        // ciktiginda ArithmeticException firlatir — bu yuzden 4 basamak + HALF_UP.
        BigDecimal dailyPercentage = dailyProfitLoss
                .divide(previousValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        response.setDailyProfitLossPercentage(dailyPercentage);
    }

    // Portfoy deger grafigi icin gercek snapshot'lar.
    //
    // Veri kaynagi MarketScheduler.savePortfolioHistory() — saat basi calisiyor.
    // Yani 1 gunluk aralikta en fazla 24 nokta olur ve uygulama kapaliyken
    // kayit birikmez. Veri yoksa bos liste doneriz; arayuz "yeterli kayit yok"
    // gosterir. Uydurma nokta uretmek grafigi gercekmis gibi gosterirdi.
    public List<PortfolioValuePointResponse> getValueHistory(Long userId, String range) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Portfolio Not Found"));

        LocalDateTime after = rangeStart(range);

        return portfolioValueHistoryRepository
                .findByPortfolioIdAndRecordedAtAfterOrderByRecordedAtAsc(portfolio.getId(), after)
                .stream()
                .map(h -> new PortfolioValuePointResponse(h.getRecordedAt(), h.getTotalValue()))
                .toList();
    }

    // Arayuzdeki buton etiketini baslangic tarihine cevirir.
    // Taninmayan deger 1 gun sayilir — hatali parametre bos grafik yerine
    // varsayilan gorunumu versin.
    private LocalDateTime rangeStart(String range) {
        LocalDateTime now = LocalDateTime.now();
        if (range == null) {
            return now.minusDays(1);
        }
        return switch (range.toUpperCase()) {
            case "1H" -> now.minusWeeks(1);
            case "1A" -> now.minusMonths(1);
            case "3A" -> now.minusMonths(3);
            case "1Y" -> now.minusYears(1);
            // "Tumu": pratikte tum kayitlar. Uygulamanin dogum tarihinden
            // oncesine gidiyoruz, boylece ayri bir sorgu yazmaya gerek kalmiyor.
            case "TUMU" -> now.minusYears(100);
            default -> now.minusDays(1);
        };
    }

    public HoldingResponse addHolding(Long userId, AddHoldingRequest request){
        // Kullanicinin ekleyecegi coini bul — yoksa 404
        Coin coin = coinRepository.findById(request.getCoinId())
                .orElseThrow(() -> new NotFoundException("Coin Not Found"));

        // Portfoyu bul — yoksa NotFoundException firlat
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Portfolio Not Found"));

        if (request.getQuantity() == null
                || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Miktar sifirdan buyuk olmali");
        }

        // AYNI COIN ICIN IKINCI SATIR ACMIYORUZ.
        //
        // Onceden her cagri yeni bir Holding kaydi yaratiyordu; ayni coin
        // portfoyde iki kez gorunuyor, toplam deger ve dagilim bozuluyordu.
        // TransactionService.applyToHolding zaten dogru davraniyordu, bu yol
        // ondan farkli kalmisti.
        Optional<Holding> mevcut =
                holdingRepository.findByPortfolioIdAndCoinId(portfolio.getId(), coin.getId());

        Holding holding = mevcut.orElseGet(() -> {
            Holding yeni = new Holding();
            yeni.setPortfolio(portfolio);
            yeni.setCoin(coin);
            yeni.setQuantity(BigDecimal.ZERO);
            yeni.setAverageBuyPrice(BigDecimal.ZERO);
            return yeni;
        });

        BigDecimal eskiMiktar = holding.getQuantity();
        BigDecimal yeniMiktar = eskiMiktar.add(request.getQuantity());

        // Agirlikli ortalama maliyet:
        // (eskiMiktar x eskiOrtalama + eklenenMiktar x eklenenFiyat) / toplamMiktar
        BigDecimal eskiMaliyet = eskiMiktar.multiply(holding.getAverageBuyPrice());
        BigDecimal yeniMaliyet = request.getQuantity().multiply(request.getAverageBuyPrice());

        // divide() olcek ve yuvarlama verilmezse bolum sonsuz basamakli
        // ciktiginda ArithmeticException firlar — 8 basamak + HALF_UP.
        BigDecimal ortalama = eskiMaliyet.add(yeniMaliyet)
                .divide(yeniMiktar, 8, RoundingMode.HALF_UP);

        holding.setQuantity(yeniMiktar);
        holding.setAverageBuyPrice(ortalama);

        // save() jenerik <S extends Holding> donuyor; once degiskene aliyoruz
        // ki mapper metodu net secilsin.
        Holding kaydedilen = holdingRepository.save(holding);
        return portfolioMapper.toHoldingResponse(kaydedilen);
    }

    public void removeHolding(Long userId, Long holdingId){
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new NotFoundException("Holding Not Found"));

        // SAHIPLIK KONTROLU.
        //
        // Bu metot userId parametresini aliyor ama kullanmiyordu: kimligi
        // dogrulanmis HERHANGI bir kullanici, id vererek baskasinin varligini
        // silebiliyordu. Id'ler ardisik oldugu icin tahmin bile gerekmiyordu.
        // (OWASP: Broken Access Control / IDOR.)
        //
        // Neden 403 degil 404: 403 "bu id var ama senin degil" bilgisini
        // sizdirir ve saldirgan id tarayarak baskalarinin kayit sayisini
        // ogrenebilir. 404 ile var olmayan ile baskasina ait olan ayirt
        // edilemez hale gelir.
        Long sahibi = holding.getPortfolio().getUser().getId();
        if (!sahibi.equals(userId)) {
            throw new NotFoundException("Holding Not Found");
        }

        holdingRepository.delete(holding);
    }
}
