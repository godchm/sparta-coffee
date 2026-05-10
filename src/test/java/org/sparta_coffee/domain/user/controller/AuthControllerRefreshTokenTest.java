package org.sparta_coffee.domain.user.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta_coffee.domain.user.entity.User;
import org.sparta_coffee.domain.user.enums.UserRole;
import org.sparta_coffee.domain.user.repository.UserRepository;
import org.sparta_coffee.global.security.refreshToken.entity.RefreshToken;
import org.sparta_coffee.global.security.refreshToken.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerRefreshTokenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.cookie.refresh-token.name}")
    private String refreshTokenCookieName;

    @Test
    @DisplayName("로그인 성공 시 accessToken은 body로 응답하고 refreshToken은 HttpOnly Cookie로 내려간다")
    void loginReturnsAccessTokenAndRefreshTokenCookie() throws Exception {
        /*
         * given
         */
        refreshTokenRepository.deleteAll();

        String email = "refresh-login-" + System.nanoTime() + "@example.com";
        String rawPassword = "password1234";

        User user = userRepository.save(
                User.builder()
                        .name("리프레시 로그인 테스트")
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .role(UserRole.USER)
                        .build()
        );

        /*
         * when
         */
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, rawPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().exists(refreshTokenCookieName))
                .andExpect(cookie().httpOnly(refreshTokenCookieName, true))
                .andReturn();

        /*
         * then
         *
         * refreshToken은 response body가 아니라 Set-Cookie로 내려가야 한다.
         * 그리고 DB에도 같은 refreshToken이 저장되어 있어야 한다.
         */
        Cookie refreshTokenCookie = result.getResponse().getCookie(refreshTokenCookieName);
        assertThat(refreshTokenCookie).isNotNull();

        RefreshToken savedRefreshToken = refreshTokenRepository.findByUser_Id(user.getId())
                .orElseThrow();

        assertThat(savedRefreshToken.getToken()).isEqualTo(refreshTokenCookie.getValue());
    }

    @Test
    @DisplayName("refreshToken Cookie로 accessToken을 재발급하고 refreshToken도 새 쿠키로 갱신한다")
    void reissueAccessTokenWithRefreshTokenCookie() throws Exception {
        /*
         * given
         *
         * 먼저 로그인해서 refreshToken 쿠키를 발급받는다.
         */
        refreshTokenRepository.deleteAll();

        String email = "refresh-reissue-" + System.nanoTime() + "@example.com";
        String rawPassword = "password1234";

        User user = userRepository.save(
                User.builder()
                        .name("리프레시 재발급 테스트")
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .role(UserRole.USER)
                        .build()
        );

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, rawPassword)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(refreshTokenCookieName))
                .andReturn();

        Cookie oldRefreshTokenCookie = loginResult.getResponse().getCookie(refreshTokenCookieName);
        assertThat(oldRefreshTokenCookie).isNotNull();

        String oldRefreshToken = oldRefreshTokenCookie.getValue();

        /*
         * when
         *
         * 로그인 때 받은 refreshToken 쿠키를 그대로 /reissue에 보낸다.
         */
        MvcResult reissueResult = mockMvc.perform(post("/api/v1/auth/reissue")
                        .cookie(oldRefreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().exists(refreshTokenCookieName))
                .andExpect(cookie().httpOnly(refreshTokenCookieName, true))
                .andReturn();

        /*
         * then
         *
         * 새 accessToken은 body로 응답되고,
         * refreshToken은 Cookie로 다시 내려와야 한다.
         * 같은 초에 재발급되면 JWT 값이 기존 토큰과 같을 수 있으므로
         * 이전 토큰과 다른지는 검증하지 않는다.
         */
        Cookie newRefreshTokenCookie = reissueResult.getResponse().getCookie(refreshTokenCookieName);
        assertThat(newRefreshTokenCookie).isNotNull();

        String newRefreshToken = newRefreshTokenCookie.getValue();

        assertThat(newRefreshToken).isNotBlank();

        RefreshToken savedRefreshToken = refreshTokenRepository.findByUser_Id(user.getId())
                .orElseThrow();

        assertThat(savedRefreshToken.getToken()).isEqualTo(newRefreshToken);
    }

    @Test
    @DisplayName("refreshToken Cookie가 없으면 토큰 재발급에 실패한다")
    void reissueFailsWithoutRefreshTokenCookie() throws Exception {
        /*
         * when & then
         */
        mockMvc.perform(post("/api/v1/auth/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_004"));
    }
}