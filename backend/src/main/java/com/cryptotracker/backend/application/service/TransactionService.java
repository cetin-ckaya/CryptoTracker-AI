package com.cryptotracker.backend.application.service;

import com.cryptotracker.backend.application.dto.PortfolioMapper;
import com.cryptotracker.backend.application.dto.request.CreateTransactionRequest;
import com.cryptotracker.backend.application.dto.response.TransactionResponse;
import com.cryptotracker.backend.application.exception.BusinessException;
import com.cryptotracker.backend.application.exception.NotFoundException;
import com.cryptotracker.backend.domain.model.Coin;
import com.cryptotracker.backend.domain.model.Holding;
import com.cryptotracker.backend.domain.model.Portfolio;
import com.cryptotracker.backend.domain.model.Transaction;
import com.cryptotracker.backend.domain.model.TransactionType;
import com.cryptotracker.backend.infrastructure.persistence.CoinRepository;
import com.cryptotracker.backend.infrastructure.persistence.HoldingRepository;
import com.cryptotracker.backend.infrastructure.persistence.PortfolioRepository;
import com.cryptotracker.backend.infrastructure.persistence.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final CoinRepository coinRepository;
    private final HoldingRepository holdingRepository;
    private final PortfolioMapper portfolioMapper;


    public TransactionService(TransactionRepository transactionRepository, PortfolioRepository portfolioRepository, CoinRepository coinRepository, HoldingRepository holdingRepository, PortfolioMapper portfolioMapper) {
        this.transactionRepository = transactionRepository;
        this.portfolioRepository = portfolioRepository;
        this.coinRepository = coinRepository;
        this.holdingRepository = holdingRepository;
        this.portfolioMapper = portfolioMapper;
    }

    // Yeni bir alım/satım kaydı oluşturur.
    // Iki tabloya birden yazar: transactions (degismez kayit defteri) ve
    // holdings (anlik pozisyon). Bu yuzden @Transactional zorunlu —
    // biri basarisiz olursa digeri de geri alinmali.
    @Transactional
    public TransactionResponse addTransaction(Long userId, CreateTransactionRequest request) {
        // Kullanıcının portföyünü bul — yoksa 404
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Portfolio Not Found"));

        // İşlem yapılacak coini bul — yoksa 404
        Coin coin = coinRepository.findById(request.getCoinId())
                .orElseThrow(() -> new NotFoundException("Coin Not Found"));

        // Miktar ve fiyat pozitif olmali. Bean Validation sadece null kontrolu
        // yapiyor (@NotNull), isaret kontrolunu burada yapiyoruz.
        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Miktar sifirdan buyuk olmali");
        }
        if (request.getPricePerUnit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Fiyat sifirdan buyuk olmali");
        }

        // totalAmount = quantity × pricePerUnit — DB'ye de kaydediyoruz
        // çünkü fiyat ileride değişse bile o anki tutarı tutmak gerekir
        BigDecimal totalAmount = request.getQuantity()
                .multiply(request.getPricePerUnit());

        // Once pozisyonu guncelle: satista yetersiz bakiye varsa buradan
        // exception firlar ve islem hic kaydedilmez.
        // Donus degeri satislarda gerceklesen kar/zarar, alimlarda null.
        BigDecimal realized = applyToHolding(portfolio, coin, request);

        // Transaction nesnesini oluştur ve alanları doldur
        Transaction transaction = new Transaction();
        transaction.setPortfolio(portfolio);
        transaction.setCoin(coin);
        transaction.setType(request.getType());
        transaction.setQuantity(request.getQuantity());
        transaction.setPricePerUnit(request.getPricePerUnit());
        transaction.setTotalAmount(totalAmount);
        // İşlem zamanı: şu an — client'tan göndermeye gerek yok, sunucu belirler
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setRealizedProfitLoss(realized);

        Transaction saved = transactionRepository.save(transaction);

        // MapStruct ile DTO'ya çevir (PortfolioMapper'da toResponse(Transaction) metodu var)
        return portfolioMapper.toResponse(saved);

    }

    // Islemin holdings tablosuna etkisini uygular.
    //
    // ALIM  : pozisyon yoksa olusturulur; varsa miktar artar ve ortalama alis
    //         fiyati agirlikli ortalama ile yeniden hesaplanir.
    // SATIM : miktar azalir. Elde olandan fazlasi satilamaz. Miktar sifira
    //         inerse pozisyon tamamen silinir.
    // Donus: satislarda gerceklesen kar/zarar, alimlarda null.
    private BigDecimal applyToHolding(Portfolio portfolio, Coin coin, CreateTransactionRequest request) {
        Optional<Holding> existing =
                holdingRepository.findByPortfolioIdAndCoinId(portfolio.getId(), coin.getId());

        if (request.getType() == TransactionType.BUY) {
            Holding holding = existing.orElseGet(() -> {
                Holding h = new Holding();
                h.setPortfolio(portfolio);
                h.setCoin(coin);
                h.setQuantity(BigDecimal.ZERO);
                h.setAverageBuyPrice(BigDecimal.ZERO);
                return h;
            });

            BigDecimal oldQuantity = holding.getQuantity();
            BigDecimal newQuantity = oldQuantity.add(request.getQuantity());

            // Agirlikli ortalama:
            // (eskiMiktar x eskiOrtalama + yeniMiktar x yeniFiyat) / toplamMiktar
            //
            // Ornek: elde 1 BTC var, ortalama 50.000$.
            //        1 BTC daha 80.000$'dan alinirsa yeni ortalama 65.000$ olur.
            BigDecimal oldCost = oldQuantity.multiply(holding.getAverageBuyPrice());
            BigDecimal newCost = request.getQuantity().multiply(request.getPricePerUnit());

            // divide() olcek ve yuvarlama verilmezse bolum sonsuz basamakli
            // ciktiginda ArithmeticException firlar — bu yuzden 8 basamak + HALF_UP.
            BigDecimal newAverage = oldCost.add(newCost)
                    .divide(newQuantity, 8, RoundingMode.HALF_UP);

            holding.setQuantity(newQuantity);
            holding.setAverageBuyPrice(newAverage);
            holdingRepository.save(holding);

            // Alimda gerceklesen kar/zarar yok
            return null;

        } else { // SELL
            Holding holding = existing.orElseThrow(() ->
                    new BusinessException("Bu coin portfoyunuzde bulunmuyor"));

            BigDecimal remaining = holding.getQuantity().subtract(request.getQuantity());

            // compareTo kullaniyoruz, equals degil: equals olcegi de karsilastirir
            // (0 ile 0.00 esit sayilmaz), compareTo yalnizca degere bakar.
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Yetersiz bakiye: elinizde "
                        + holding.getQuantity().stripTrailingZeros().toPlainString()
                        + " " + coin.getSymbol() + " var");
            }

            // GERCEKLESEN KAR/ZARAR.
            //
            // Ortalama maliyet yonteminde satilan birimlerin maliyeti, o andaki
            // ortalama alis fiyatidir. Kazanc = (satis fiyati - ortalama) x miktar.
            //
            // Bu degeri holding'i guncellemeden ONCE hesapliyoruz: asagida
            // holding silinebiliyor ve ortalama fiyata erisim kalmiyor.
            BigDecimal realized = request.getPricePerUnit()
                    .subtract(holding.getAverageBuyPrice())
                    .multiply(request.getQuantity());

            if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                // Pozisyon tamamen kapandi — satir birakmaya gerek yok
                holdingRepository.delete(holding);
            } else {
                // Satista ortalama alis fiyati degismez; yalnizca miktar azalir.
                // Ortalama maliyet, kalan birimler icin ayni kalmaya devam eder.
                holding.setQuantity(remaining);
                holdingRepository.save(holding);
            }

            return realized;
        }
    }

    // Kullanıcının işlem geçmişini tarihe göre azalan sırayla döndürür
    // TransactionRepository'deki findByPortfolioId...Desc metodu sıralamayı halleder
    public List<TransactionResponse> getTransaction(Long userId){
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Portfolio Not Found"));

        List<Transaction> transactions =
                transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(portfolio.getId());

        // Her Transaction → TransactionResponse (MapStruct stream mapping)
        return transactions.stream()
                .map(portfolioMapper::toResponse)
                .toList();
    }
}
