package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequestDto {

    @NotBlank(message = "Ad alanı boş bırakılamaz")
    @Size(min = 2, max = 50, message = "Ad alanı 2 ile 50 karakter arasında olmalıdır")
    private String name;

    @NotBlank(message = "Soyad alanı boş bırakılamaz")
    @Size(min = 2, max = 50, message = "Soyad alanı 2 ile 50 karakter arasında olmalıdır")
    private String surname;

    @NotBlank(message = "Email alanı boş bırakılamaz")
    @Email(message = "Email formatı hatalı")
    @Pattern(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "Geçerli ve güvenli bir email adresi giriniz")
    private String email;

    @NotBlank(message = "Telefon alanı boş bırakılamaz")
    // Türkiye için 05 ile başlayan ve toplam 11 haneli olan numara formatı
    @Pattern(regexp = "^(05)[0-9]{9}$", message = "Telefon numarası 05XX XXX XX XX formatında 11 haneli olmalıdır")
    private String phone;

    @NotBlank(message = "Şifre alanı boş bırakılamaz")
    @Size(min = 6, max = 30, message = "Şifre en az 6, en fazla 30 karakter olmalıdır")
    // Şifre: En az 1 büyük harf, 1 küçük harf, 1 rakam ve 1 özel karakter içermeli
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!.])(?=\\S+$).*$",
            message = "Şifre boşluk içeremez ve en az bir rakam, bir büyük harf, bir küçük harf ve bir özel karakter içermelidir"
    )
    private String password;
}