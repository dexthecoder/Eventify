package com.example.demo.configs;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestControllerAdvice(basePackages = "com.example.demo.controller")
public class GlobalException {

    // @RequestBody (JSON) ile gelen hatalar için
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        List<HashMap<String, Object>> errors = parseError(e.getFieldErrors());
        return ResponseEntity.badRequest().body(errors);
    }

    // @ModelAttribute (Form-Data) ile gelen hatalar için
    @ExceptionHandler(BindException.class)
    public ResponseEntity handleBindException(BindException e){
        List<HashMap<String, Object>> errors = parseError(e.getFieldErrors());
        return ResponseEntity.badRequest().body(errors);
    }

    private List<HashMap<String, Object>> parseError(List<FieldError> fieldErrors) {
        List<HashMap<String, Object>> errors = new ArrayList<>();

        for (FieldError error : fieldErrors) {
            HashMap<String, Object> errorMap = new HashMap<>();
            errorMap.put("field", error.getField());

            Object rejectedValue = error.getRejectedValue();

            // Eğer kullanıcı metin bekleyen yere dosya göndermişse
            if (rejectedValue instanceof org.springframework.web.multipart.MultipartFile) {
                errorMap.put("rejectedValue", "[Dosya İçeriği/File Object]");
                errorMap.put("message", "Bu alan dosya yüklemesini desteklemez. Lütfen metin formatında geçerli bir veri giriniz.");
            } else {
                // Dosya değilse normal değerini yaz
                errorMap.put("rejectedValue", rejectedValue);

                // Tip uyuşmazlığı kontrolü
                if (error.getCode() != null && error.getCode().contains("typeMismatch")) {
                    errorMap.put("message", "Geçersiz veri girdiniz. Lütfen bu alan için beklenen formata uygun bir değer giriniz.");
                } else {
                    errorMap.put("message", error.getDefaultMessage());
                }
            }

            errors.add(errorMap);
        }
        return errors;
    }

    // URL üzerinden gelen tip uyuşmazlıkları için
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

        String paramName = ex.getName();
        Object value = ex.getValue();
        Class<?> requiredType = ex.getRequiredType();

        String message = "";

        if (requiredType != null) {
            if (requiredType.isEnum()) {
                message = paramName + " parametresi için geçersiz değer: '" + value + "'. Lütfen geçerli bir seçenek giriniz.";
            }
            else if (requiredType == Long.class || requiredType == Integer.class) {
                message = paramName + " sayısal bir değer olmalıdır. Gönderilen değer: " + value;
            }
            else if (requiredType == Boolean.class) {
                message = paramName + " true veya false olmalıdır. Gönderilen değer: " + value;
            }
            else {
                message = paramName + " parametresi için geçersiz değer: " + value;
            }
        } else {
            message = "Geçersiz parametre: " + paramName;
        }

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return ResponseEntity.badRequest().body(error);
    }

    // devasa dosyaları engelleyen kod
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity handleMaxSizeException(org.springframework.web.multipart.MaxUploadSizeExceededException exc) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Yüklemeye çalıştığınız dosya çok büyük! Lütfen en fazla 2MB boyutunda bir dosya seçiniz.");
        return ResponseEntity.status(413).body(error); // 413 Payload Too Large
    }

    // Bozuk veya eksik JSON formatı için
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Gönderilen veri formatı (JSON) bozuk veya okunamıyor. Lütfen veri yapınızı kontrol edin.");
        return ResponseEntity.badRequest().body(error);
    }
}