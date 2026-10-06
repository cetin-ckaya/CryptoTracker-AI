package com.cryptotracker.backend.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter

public class Transaction extends BaseEntity{

    @ManyToOne
    @JoinColumn(name = "portfolio_id",nullable = false)
    private Portfolio portfolio;

    @ManyToOne
    @JoinColumn(name = "coin_id",nullable = false)
    private Coin coin;

    @Enumerated(EnumType.STRING)
    @Column(name = "type",nullable = false)
    private TransactionType type;

    @Column(name = "quantity",nullable = false)
    private BigDecimal quantity;

    @Column(name = "price_per_unit")
    private BigDecimal pricePerUnit;

    @Column(name = "total_amount",nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "transaction_date",nullable = false)
    private LocalDateTime transactionDate;

    // Yalnizca SELL islemlerinde dolu: (satis fiyati - ortalama alis maliyeti) x miktar
    // Alimlarda null kalir — alirken gerceklesen bir kar/zarar olmaz.
    @Column(name = "realized_profit_loss")
    private BigDecimal realizedProfitLoss;
}
