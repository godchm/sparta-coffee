package org.sparta_coffee.global.security.refreshToken;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 리프레쉬 토큰을 http 쿠키로 관리하는 클래스
 *
 * 로그인 또는 토큰 재발급시 리프레쉬 토큰을 응답 쿠키에 담아 내려준다.
 * 로그아웃 시 리스레쉬 토큰을 만료시켜서 브라우저에서 제거.
 *
 * 리프레쉬 토큰을 응답 바디에 직접 노출하지 않고 쿠키로 관리하기 위해 사용한다.
 *
 */

@Component
public class RefreshTokenCookieManager {

    private final String refreshTokenCookieName;
    private final int refreshTokenCookieMaxAge;
    private final boolean refreshTokenCookieSecure;


    /**
     * application.yml에 정의된 refresh token Cookie 설정값을 주입받는다.
     *
     * @param refreshTokenCookieName refresh token Cookie 이름
     * @param refreshTokenCookieMaxAge refresh token Cookie 유지 시간
     * @param refreshTokenCookieSecure HTTPS 환경에서만 Cookie를 전송할지 여부
     */

    public RefreshTokenCookieManager(
            @Value("${jwt.cookie.refresh-token.name}") String refreshTokenCookieName,
            @Value("${jwt.cookie.refresh-token.max-age}") int refreshTokenCookieMaxAge,

            // 현재는 브라우저 기반 프론트엔드가 없지만,
            // 추후 프론트엔드 연동 시 HTTPS 환경에서만 refresh token Cookie를 전송하도록 확장할 수 있다.
            // 현재는 로컬 테스트를 위해 false로 설정하지만,
            // 추후 HTTPS 환경에서는 true로 변경해 Cookie를 안전하게 전송할 수 있다.
            @Value("${jwt.cookie.refresh-token.secure}") boolean refreshTokenCookieSecure
    ) {
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.refreshTokenCookieMaxAge = refreshTokenCookieMaxAge;
        this.refreshTokenCookieSecure = refreshTokenCookieSecure;
    }

    /**
     * refresh token을 HttpOnly Cookie로 응답에 추가.
     *
     * 로그인 성공 또는 토큰 재발급 성공 시 호출됩.
     * HttpOnly 옵션을 적용해 JavaScript에서 refresh token에 직접 접근하지 못하도록 한다.
     *
     * @param response Cookie를 추가할 HTTP 응답 객체
     * @param refreshToken Cookie에 저장할 refresh token 값
     */

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(refreshTokenCookieName, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(refreshTokenCookieSecure);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(refreshTokenCookieMaxAge);

        response.addCookie(cookie);
    }

    /**
     * refresh token Cookie를 만료시켜 브라우저에서 제거.
     *
     * 로그아웃 시 호출.
     * 같은 이름과 path를 가진 Cookie의 maxAge를 0으로 설정해 기존 Cookie를 삭제.
     *
     * @param response Cookie 삭제 정보를 담을 HTTP 응답 객체
     */

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(refreshTokenCookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(refreshTokenCookieSecure);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }
}