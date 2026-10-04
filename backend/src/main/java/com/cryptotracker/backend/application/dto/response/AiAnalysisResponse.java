package com.cryptotracker.backend.application.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// AI analizinin tamami. Serbest metin yerine yapilandirilmis donuyoruz:
// arayuzdeki kartlar (sinyal rozeti, hedef, guven cubugu) ancak alanlar
// ayri ayri geldiginde doldurulabilir. Metni regex ile ayiklamak
// modelin cumle kurusuna bagli kalirdi.
@Getter
@Setter
public class AiAnalysisResponse {

    private String headline;        // "Portfoy dengeli, kisa vadede temkinli pozitif"
    private String summary;         // bir paragraflik genel yorum
    private String model;           // hangi model uretti
    private LocalDateTime generatedAt;
    private boolean personalized;   // PREMIUM ise true: kullanicinin kendi portfoyu
    private List<AiSignalResponse> signals;
    private String rawText;         // JSON cozulemezse ham metin buraya duser
}
