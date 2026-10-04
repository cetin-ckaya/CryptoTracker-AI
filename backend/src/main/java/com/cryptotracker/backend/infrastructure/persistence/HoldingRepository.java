package com.cryptotracker.backend.infrastructure.persistence;

import com.cryptotracker.backend.domain.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding,Long> {
    //Portföydeki tüm varlıkları getir
    List<Holding> findByPortfolioId(Long portfolioId);

    // Bir portföydeki belirli coin'in pozisyonunu getir.
    // Islem eklenirken "bu coin portfoyde zaten var mi" sorusunu cevaplar.
    // Optional: yoksa null yerine bos Optional doner, servis katmani
    // "yeni holding olustur" dalina girer.
    Optional<Holding> findByPortfolioIdAndCoinId(Long portfolioId, Long coinId);
}
