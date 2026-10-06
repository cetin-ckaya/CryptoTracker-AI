package com.cryptotracker.backend.infrastructure.persistence;

import com.cryptotracker.backend.domain.model.Transaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    //İşlem geçmişini tarihe göre sıralı getir
    // @EntityGraph: Spring Data'ya "bu sorguyu çekerken coin'i de JOIN ile getir" der
    // attributePaths = hangi ilişkilerin JOIN FETCH ile yükleneceğini belirtir
    @EntityGraph(attributePaths = {"coin"})
    List<Transaction> findByPortfolioIdOrderByTransactionDateDesc(Long portfolioId);

    // Verilen andan bu yana portfoye giren NET para.
    // Alim para girisidir (+), satis para cikisidir (-).
    //
    // Gunluk kar/zarar hesabinda bu tutar farktan DUSULUR. Aksi halde
    // kullanicinin kendi alim satimi performans gibi gorunur: coin alinca
    // portfoy degeri artar ve ekran bunu kar sanar, satinca duser ve zarar sanar.
    //
    // COALESCE: hic islem yoksa SUM null doner, sifira ceviriyoruz.
    @Query("SELECT COALESCE(SUM(CASE WHEN t.type = com.cryptotracker.backend.domain.model.TransactionType.BUY "
            + "THEN t.totalAmount ELSE -t.totalAmount END), 0) "
            + "FROM Transaction t "
            + "WHERE t.portfolio.id = :portfolioId AND t.transactionDate > :after")
    BigDecimal netCashFlowSince(@Param("portfolioId") Long portfolioId,
                                @Param("after") LocalDateTime after);

    // Tum zamanlarin gerceklesen kar/zarari.
    // Yalnizca satis satirlarinda dolu oldugu icin alimlar toplama katilmaz.
    @Query("SELECT COALESCE(SUM(t.realizedProfitLoss), 0) "
            + "FROM Transaction t "
            + "WHERE t.portfolio.id = :portfolioId")
    BigDecimal totalRealizedProfitLoss(@Param("portfolioId") Long portfolioId);
}
