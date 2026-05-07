package org.sparta_coffee.common.config.security;

import lombok.RequiredArgsConstructor;
import org.sparta_coffee.global.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                        // 공개 조회 API
                        .requestMatchers(HttpMethod.GET, "/api/v1/popular-ranking/today").permitAll()

                        // 메뉴 조회: 로그인한 USER, ADMIN만 가능
                        .requestMatchers(HttpMethod.GET, "/api/v1/menus").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/menus/**").hasAnyRole("USER", "ADMIN")

                        // 인기검색어 이벤트 발행: 테스트 편하면 permitAll, 로그인 기반이면 authenticated
                        .requestMatchers(HttpMethod.POST, "/api/v1/popular-ranking").authenticated()

                        // 관리자 전용 메뉴 관리
                        .requestMatchers(HttpMethod.POST, "/api/v1/menus").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/menus/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/menus/**").hasRole("ADMIN")

                        // 관리자 전용 유저 관리
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN")

                        // 로그인 사용자
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/points/**").authenticated()
                        .requestMatchers("/api/v1/orders/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
