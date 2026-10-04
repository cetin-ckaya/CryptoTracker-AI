package com.cryptotracker.backend.application.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// AI analiz sayfasindaki tek bir coin karti.
//
// action: AL / SAT / TUT  — arayuzdeki renkli rozet bunu kullanir.
// confidence: modelin KENDI beyan ettigi guven yuzdesi. Olculmus bir
//             olasilik degil; bu yuzden arayuzde "model guveni" diyoruz.
@Getter
@Setter
public class AiSignalResponse {

    private String symbol;
    private String name;
    private BigDecimal price;
    private String action;
    private String target;
    private Integer confidence;
    private String comment;
}
