package org.sparta_coffee.domain.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.user.dto.request.LoginRequest;
import org.sparta_coffee.domain.user.dto.response.LoginResponse;
import org.sparta_coffee.domain.user.dto.response.LoginResult;
import org.sparta_coffee.domain.user.dto.response.TokenReissueResponse;
import org.sparta_coffee.domain.user.dto.response.TokenReissueResult;
import org.sparta_coffee.domain.user.service.AuthService;
import org.sparta_coffee.global.dto.ApiResponseDto;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.domain.UserException;
import org.sparta_coffee.global.security.CustomUserDetails;
import org.sparta_coffee.global.security.refreshToken.RefreshTokenCookieManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResult loginResult = authService.login(request);

        refreshTokenCookieManager.addRefreshTokenCookie(
                response,
                loginResult.refreshToken()
        );

        return ResponseEntity.ok(
                ApiResponseDto.success(HttpStatus.OK, loginResult.loginResponse())
        );
    }


    // 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponseDto<TokenReissueResponse>> reissue(
            @CookieValue(name = "${jwt.cookie.refresh-token.name}", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UserException(ErrorCode.INVALID_TOKEN);
        }

        TokenReissueResult reissueResult = authService.reissue(refreshToken);

        refreshTokenCookieManager.addRefreshTokenCookie(
                response,
                reissueResult.refreshToken()
        );

        return ResponseEntity.ok(
                ApiResponseDto.success(HttpStatus.OK, reissueResult.tokenReissueResponse())
        );
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response
    ) {
        authService.logout(userDetails.userId());

        refreshTokenCookieManager.deleteRefreshTokenCookie(response);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDto.successWithNoContent());
    }
}
