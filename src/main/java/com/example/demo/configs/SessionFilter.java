package com.example.demo.configs;

import com.example.demo.dto.UserResponseDto;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class SessionFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SessionFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 🔹 CORS preflight isteklerini auth'tan muaf tut
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String urlPath = request.getRequestURI();

        // Mevcut projemizdeki serbest URL'ler
        String[] freeUrls = {"/user/register", "/user/login", "/h2-console", "/actuator", "/swagger-ui", "/v3/api-docs"};

        boolean isAuth = true;
        for (String freeUrl : freeUrls) {
            if (urlPath.startsWith(freeUrl)) {
                isAuth = false;
                break;
            }
        }

        // 🔹 CLIENT BİLGİLERİ
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String method = request.getMethod();
        String query = request.getQueryString();
        String referer = request.getHeader("Referer");

        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        HttpSession session = request.getSession(false);
        // Önceki projede "customer" olan session anahtarı, mevcut UserService'imizde "user" olarak ayarlanmıştı.
        Object user = (session != null) ? session.getAttribute("user") : null;

        // ✅ INFO LOG
        logger.info("""
                ====== REQUEST LOG ======
                Time      : {}
                IP        : {}
                Method    : {}
                URL       : {}
                Query     : {}
                Referer   : {}
                UserAgent : {}
                Session   : {}
                User      : {}
                ==========================
                """,
                time,
                ipAddress,
                method,
                urlPath,
                query,
                referer,
                userAgent,
                (session != null ? session.getId() : "No Session"),
                (user != null ? user : "Anonymous")
        );

        // AUTH KONTROL
        if (isAuth) {
            if (user == null) {

                logger.warn("Unauthorized access -> IP: {}, URL: {}", ipAddress, urlPath);

                // Eğer projede ileride Thymeleaf (MVC) kullanırsan diye uyarlandı
                if (urlPath.startsWith("/mvc")) {
                    response.sendRedirect("/mvc/user/login?auth=required");
                    return;
                }

                // REST API için JSON formatında 401 dön (Türkçe karakter sorunu için charset eklendi)
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                String jsonResponse = """
                        {
                          "success": false,
                          "message": "Yetkisiz erişim. Lütfen giriş yapın."
                        }
                        """;

                response.getWriter().write(jsonResponse);
                return;
            } else {
                // Oturum var
                if (urlPath.startsWith("/mvc")) {
                    UserResponseDto userResponseDto = (UserResponseDto) user;
                    request.setAttribute("user", userResponseDto);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}