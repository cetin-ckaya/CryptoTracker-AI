package com.cryptotracker.backend.presentation.exception;

import com.cryptotracker.backend.application.exception.BusinessException;
import com.cryptotracker.backend.application.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
// DIKKAT: java.nio.file.AccessDeniedException DEGIL. Ayni isimde iki sinif var,
// yanlis olani import edilirse handler hic calismaz ve hata 500 donmeye devam eder.
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

// @RestControllerAdvice: tüm @RestController'lardan fırlayan exception'ları yakalar.
// Buraya düşen her exception için doğru HTTP kodu + JSON body üretiyoruz.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // NotFoundException → 404
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage(), LocalDateTime.now()));
    }

    // @Valid başarısız olursa Spring bu exception'ı fırlatır → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // İlk validation hatasının mesajını al
        String message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, message, LocalDateTime.now()));
    }

    // Yetki yok → 403 Forbidden
    //
    // Bu handler olmadan @PreAuthorize("hasRole('PREMIUM')") bir ucu FREE kullanıcı
    // çağırdığında fırlayan AccessDeniedException, aşağıdaki RuntimeException
    // handler'ına düşüyordu ve istemciye 500 dönüyordu. Oysa sunucuda bir arıza yok;
    // istek bilinçli olarak reddedildi.
    //
    // 401 ile farkı: 401 "kim olduğunu bilmiyorum" (token yok/geçersiz),
    // 403 "kim olduğunu biliyorum ama bu işlem için yetkin yok".
    //
    // Spring en ÖZEL handler'ı seçer; AccessDeniedException da bir RuntimeException
    // olduğu halde bu metot kazanır.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        // Mesajı sabit tutuyoruz: exception'ın kendi metni iç yapıyı sızdırabilir.
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Bu işlem için yetkiniz yok", LocalDateTime.now()));
    }

    // Genel RuntimeException → 500 (beklenmedik hatalar için güvenlik ağı)
    //
    // Istemciye SABIT bir mesaj donuyoruz. Onceden ex.getMessage() dogrudan
    // yanita yaziliyordu; Hibernate veya JDBC kaynakli bir istisna yakalandiginda
    // tablo adi, sutun adi, hatta SQL parcasi disari cikabiliyordu. Bu bilgi
    // saldirgana veri modelini haritalandirma imkani verir.
    //
    // Ayrinti kaybolmuyor: tam stack trace sunucu log'una yaziliyor. Hata
    // ayiklamak gelistiricinin isi, istemcinin degil.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("Beklenmeyen hata", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500,
                        "Beklenmeyen bir hata olustu. Lutfen tekrar deneyin.",
                        LocalDateTime.now()));
    }

    // BusinessException → 409 Conflict
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getMessage(), LocalDateTime.now()));
    }
}