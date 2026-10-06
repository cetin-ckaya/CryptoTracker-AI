package com.cryptotracker.backend.application.service;


import com.cryptotracker.backend.application.dto.PortfolioMapper;
import com.cryptotracker.backend.application.dto.request.AddHoldingRequest;
import com.cryptotracker.backend.application.dto.response.PortfolioResponse;
import com.cryptotracker.backend.application.exception.NotFoundException;
import com.cryptotracker.backend.domain.model.Coin;
import com.cryptotracker.backend.domain.model.Holding;
import com.cryptotracker.backend.domain.model.Portfolio;
import com.cryptotracker.backend.domain.model.User;
import com.cryptotracker.backend.infrastructure.external.CoinGeckoService;
import com.cryptotracker.backend.infrastructure.persistence.CoinRepository;
import com.cryptotracker.backend.infrastructure.persistence.HoldingRepository;
import com.cryptotracker.backend.infrastructure.persistence.PortfolioRepository;
import com.cryptotracker.backend.domain.model.PortfolioValueHistory;
import com.cryptotracker.backend.infrastructure.persistence.PortfolioValueHistoryRepository;
import com.cryptotracker.backend.infrastructure.persistence.TransactionRepository;
import com.cryptotracker.backend.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// @ExtendWith: Mockito'yu JUnit 5 ile entegre eder
@ExtendWith(MockitoExtension.class)
public class PortfolioServiceTest {
    // @Mock: Gerçek bean yerine sahte nesne — HTTP isteği gitmez, DB sorgusu gitmez
    @Mock private PortfolioRepository portfolioRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private CoinRepository coinRepository;
    @Mock private UserRepository userRepository;
    @Mock private PortfolioMapper portfolioMapper;
    @Mock private CoinGeckoService coinGeckoService;
    // Gunluk kar/zarar hesabi icin eklendi; olmayinca getPortfolio NPE atiyordu
    @Mock private PortfolioValueHistoryRepository portfolioValueHistoryRepository;
    @Mock private TransactionRepository transactionRepository;

    // @InjectMocks: Yukaridaki mock'lari constructor'a inject ederek gercek service'i olusturur
    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void getPortfolio_whenPortfolioNotFound_throwsNotFoundException() {
        // GIVEN — kullanici portfoyu yok
        when(portfolioRepository.findByUserIdWithHoldings(99L)).thenReturn(Optional.empty());

        // WHEN & THEN — NotFoundException firlatilmali
        assertThatThrownBy(() -> portfolioService.getPortfolio(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Portfolio Not Found");
    }

    @Test
    void getPortfolio_calculatesProfitLossCorrectly() {
        // GIVEN — 0.5 BTC, alis fiyati 50000, guncel fiyat 60000
        Coin btc = new Coin();
        btc.setSymbol("BTC");

        Holding holding = new Holding();
        holding.setCoin(btc);
        holding.setQuantity(new BigDecimal("0.5"));
        holding.setAverageBuyPrice(new BigDecimal("50000"));

        Portfolio portfolio = new Portfolio();
        portfolio.setHoldings(List.of(holding));

        // Mock'lar ne dondurecek?
        when(portfolioRepository.findByUserIdWithHoldings(1L)).thenReturn(Optional.of(portfolio));
        when(portfolioMapper.toPortfolioResponse(portfolio)).thenReturn(new PortfolioResponse());
        // CoinGecko mock — gercek HTTP isteği gitmez
        when(coinGeckoService.getPrice("BTC")).thenReturn(new BigDecimal("60000"));

        // WHEN
        PortfolioResponse response = portfolioService.getPortfolio(1L);

        // THEN — 0.5 * 60000 = 30000 deger, 0.5 * 50000 = 25000 maliyet, kar = 5000
        assertThat(response.getTotalValue()).isEqualByComparingTo("30000");
        assertThat(response.getTotalInvested()).isEqualByComparingTo("25000");
        assertThat(response.getTotalProfitLoss()).isEqualByComparingTo("5000");
    }

    // ---------------------------------------------------------------
    // Yetkilendirme: baskasinin varligi silinemez (IDOR regresyon testi)
    // ---------------------------------------------------------------
    //
    // Bu testin varlik sebebi: removeHolding eskiden userId parametresini
    // aliyor ama HIC KULLANMIYORDU. Kimligi dogrulanmis herhangi bir
    // kullanici, id vererek baskasinin varligini silebiliyordu.
    // Ileride biri kontrolu kaldirirsa bu test kirilir.
    @Test
    void removeHolding_whenHoldingBelongsToAnotherUser_throwsAndDeletesNothing() {
        // GIVEN — holding 7 numarali kullanicinin portfoyune ait
        User sahibi = new User();
        sahibi.setId(7L);

        Portfolio baskasininPortfoyu = new Portfolio();
        baskasininPortfoyu.setUser(sahibi);

        Holding holding = new Holding();
        holding.setPortfolio(baskasininPortfoyu);

        when(holdingRepository.findById(5L)).thenReturn(Optional.of(holding));

        // WHEN & THEN — 1 numarali kullanici silmeye calisiyor
        // 403 degil 404: "bu id var ama senin degil" bilgisi sizdirilmamali,
        // aksi halde id tarayarak baskalarinin kayitlari kesfedilebilir.
        assertThatThrownBy(() -> portfolioService.removeHolding(1L, 5L))
                .isInstanceOf(NotFoundException.class);

        // En onemli dogrulama: silme hic cagrilmamis olmali
        verify(holdingRepository, never()).delete(any(Holding.class));
    }

    @Test
    void removeHolding_whenHoldingBelongsToUser_deletesIt() {
        User sahibi = new User();
        sahibi.setId(1L);

        Portfolio kendiPortfoyu = new Portfolio();
        kendiPortfoyu.setUser(sahibi);

        Holding holding = new Holding();
        holding.setPortfolio(kendiPortfoyu);

        when(holdingRepository.findById(5L)).thenReturn(Optional.of(holding));

        portfolioService.removeHolding(1L, 5L);

        verify(holdingRepository).delete(holding);
    }

    // ---------------------------------------------------------------
    // addHolding: ayni coin icin ikinci satir acilmaz, ortalama guncellenir
    // ---------------------------------------------------------------
    @Test
    void addHolding_whenCoinAlreadyHeld_mergesWithWeightedAverage() {
        // GIVEN — elde 1 BTC var, ortalama 50.000
        Coin btc = new Coin();
        btc.setId(1L);
        btc.setSymbol("BTC");

        Portfolio portfolio = new Portfolio();
        portfolio.setId(10L);

        Holding mevcut = new Holding();
        mevcut.setPortfolio(portfolio);
        mevcut.setCoin(btc);
        mevcut.setQuantity(new BigDecimal("1"));
        mevcut.setAverageBuyPrice(new BigDecimal("50000"));

        AddHoldingRequest request = new AddHoldingRequest();
        request.setCoinId(1L);
        request.setQuantity(new BigDecimal("1"));
        request.setAverageBuyPrice(new BigDecimal("80000"));

        when(coinRepository.findById(1L)).thenReturn(Optional.of(btc));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolioIdAndCoinId(10L, 1L)).thenReturn(Optional.of(mevcut));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN — 1 BTC daha, bu kez 80.000'den
        portfolioService.addHolding(1L, request);

        // THEN — yeni satir ACILMAMALI, mevcut satir guncellenmeli
        ArgumentCaptor<Holding> captor = ArgumentCaptor.forClass(Holding.class);
        verify(holdingRepository).save(captor.capture());
        Holding kaydedilen = captor.getValue();

        // (1 x 50000 + 1 x 80000) / 2 = 65000
        assertThat(kaydedilen.getQuantity()).isEqualByComparingTo("2");
        assertThat(kaydedilen.getAverageBuyPrice()).isEqualByComparingTo("65000");
    }

    // ---------------------------------------------------------------
    // Dayaniklilik: fiyat servisi cokse de portfoy sorgusu patlamaz
    // ---------------------------------------------------------------
    @Test
    void getPortfolio_whenPriceLookupFails_fallsBackToAverageBuyPrice() {
        Coin btc = new Coin();
        btc.setSymbol("BTC");

        Holding holding = new Holding();
        holding.setCoin(btc);
        holding.setQuantity(new BigDecimal("2"));
        holding.setAverageBuyPrice(new BigDecimal("50000"));

        Portfolio portfolio = new Portfolio();
        portfolio.setHoldings(List.of(holding));

        when(portfolioRepository.findByUserIdWithHoldings(1L)).thenReturn(Optional.of(portfolio));
        when(portfolioMapper.toPortfolioResponse(portfolio)).thenReturn(new PortfolioResponse());
        // CoinGecko erisilemiyor
        when(coinGeckoService.getPrice("BTC"))
                .thenThrow(new RuntimeException("I/O error on GET request"));

        // WHEN — istisna disari cikmamali
        PortfolioResponse response = portfolioService.getPortfolio(1L);

        // THEN — deger maliyete esit, yani kar/zarar sifir.
        // Sifir fiyat yazsaydik portfoy degeri 0 gorunur, kullanici
        // parasini kaybettigini sanirdi.
        assertThat(response.getTotalValue()).isEqualByComparingTo("100000");
        assertThat(response.getTotalInvested()).isEqualByComparingTo("100000");
        assertThat(response.getTotalProfitLoss()).isEqualByComparingTo("0");
    }

    // ---------------------------------------------------------------
    // Gunluk kar/zarar: kullanicinin kendi alim satimi performans sayilmaz
    // ---------------------------------------------------------------
    //
    // Gercek hata: 0,2 BTC satildiginda portfoyden varlik cikti, deger dustu
    // ve ekran "zarar" gosterdi. Oysa para nakde donmustu, kayip yoktu.
    @Test
    void getPortfolio_dailyChange_excludesUserCashFlow() {
        Coin btc = new Coin();
        btc.setSymbol("BTC");

        Holding holding = new Holding();
        holding.setCoin(btc);
        holding.setQuantity(new BigDecimal("1"));
        holding.setAverageBuyPrice(new BigDecimal("50000"));

        Portfolio portfolio = new Portfolio();
        portfolio.setId(3L);
        portfolio.setHoldings(List.of(holding));

        // Simdiki deger: 1 x 60000 = 60000
        when(portfolioRepository.findByUserIdWithHoldings(1L)).thenReturn(Optional.of(portfolio));
        when(portfolioMapper.toPortfolioResponse(portfolio)).thenReturn(new PortfolioResponse());
        when(coinGeckoService.getPrice("BTC")).thenReturn(new BigDecimal("60000"));

        // 24 saatten eski snapshot: 100000
        PortfolioValueHistory snapshot = new PortfolioValueHistory();
        snapshot.setTotalValue(new BigDecimal("100000"));
        snapshot.setRecordedAt(LocalDateTime.now().minusDays(2));
        when(portfolioValueHistoryRepository
                .findFirstByPortfolioIdAndRecordedAtBeforeOrderByRecordedAtDesc(eq(3L), any()))
                .thenReturn(Optional.of(snapshot));

        // O pencerede kullanici 45000 degerinde varlik SATTI (net akis -45000)
        when(transactionRepository.netCashFlowSince(eq(3L), any()))
                .thenReturn(new BigDecimal("-45000"));
        when(transactionRepository.totalRealizedProfitLoss(3L)).thenReturn(BigDecimal.ZERO);

        PortfolioResponse response = portfolioService.getPortfolio(1L);

        // Ham fark: 60000 - 100000 = -40000  (yaniltici "zarar")
        // Duzeltilmis: -40000 - (-45000) = +5000  (gercek performans)
        assertThat(response.getDailyProfitLoss()).isEqualByComparingTo("5000");
    }

    @Test
    void getPortfolio_exposesRealizedProfitLoss() {
        Portfolio portfolio = new Portfolio();
        portfolio.setId(3L);
        portfolio.setHoldings(List.of());

        when(portfolioRepository.findByUserIdWithHoldings(1L)).thenReturn(Optional.of(portfolio));
        when(portfolioMapper.toPortfolioResponse(portfolio)).thenReturn(new PortfolioResponse());
        when(portfolioValueHistoryRepository
                .findFirstByPortfolioIdAndRecordedAtBeforeOrderByRecordedAtDesc(eq(3L), any()))
                .thenReturn(Optional.empty());
        when(transactionRepository.totalRealizedProfitLoss(3L))
                .thenReturn(new BigDecimal("11151.02"));

        PortfolioResponse response = portfolioService.getPortfolio(1L);

        assertThat(response.getRealizedProfitLoss()).isEqualByComparingTo("11151.02");
    }

}
