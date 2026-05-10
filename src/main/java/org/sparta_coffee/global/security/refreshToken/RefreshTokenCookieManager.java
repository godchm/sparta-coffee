package org.sparta_coffee.global.security.refreshToken;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieManager {

    private final String refreshTokenCookieName;
    private final int refreshTokenCookieMaxAge;
    private final boolean refreshTokenCookieSecure;

    public RefreshTokenCookieManager(
            @Value("${jwt.cookie.refresh-token.name}") String refreshTokenCookieName,
            @Value("${jwt.cookie.refresh-token.max-age}") int refreshTokenCookieMaxAge,
            @Value("${jwt.cookie.refresh-token.secure}") boolean refreshTokenCookieSecure
    ) {
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.refreshTokenCookieMaxAge = refreshTokenCookieMaxAge;
        this.refreshTokenCookieSecure = refreshTokenCookieSecure;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(refreshTokenCookieName, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(refreshTokenCookieSecure);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(refreshTokenCookieMaxAge);

        response.addCookie(cookie);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(refreshTokenCookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(refreshTokenCookieSecure);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }
}