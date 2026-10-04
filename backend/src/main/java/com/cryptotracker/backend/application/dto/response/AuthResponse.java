package com.cryptotracker.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Giris yanitinda token'in yani sira kullanicinin kimlik bilgisi de donuyor.
// Boylece arayuz, ismi gostermek icin ayrica bir istek atmak zorunda kalmiyor.
// Hassas alan (passwordHash) burada YOK — response DTO kurali.
@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String fullName;   // kayit sirasinda opsiyonel oldugu icin null olabilir
}
