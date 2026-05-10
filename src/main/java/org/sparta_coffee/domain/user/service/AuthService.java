package org.sparta_coffee.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.user.dto.request.LoginRequest;
import org.sparta_coffee.domain.user.dto.response.LoginResponse;
import org.sparta_coffee.domain.user.dto.response.LoginResult;
import org.sparta_coffee.domain.user.dto.response.TokenReissueResponse;
import org.sparta_coffee.domain.user.dto.response.TokenReissueResult;
import org.sparta_coffee.domain.user.entity.User;
import org.sparta_coffee.domain.user.repository.UserRepository;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.domain.UserException;
import org.sparta_coffee.global.security.JwtTokenProvider;
import org.sparta_coffee.global.security.refreshToken.entity.RefreshToken;
import org.sparta_coffee.global.security.refreshToken.repository.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UserException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getRole());

        saveRefreshToken(user, refreshToken);

        LoginResponse loginResponse = LoginResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .build();

        return LoginResult.builder()
                .loginResponse(loginResponse)
                .refreshToken(refreshToken)
                .build();
    }


    @Transactional
    public TokenReissueResult reissue(String requestRefreshToken) {
        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            throw new UserException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(requestRefreshToken);

        RefreshToken savedRefreshToken = refreshTokenRepository.findByUser_Id(userId)
                .orElseThrow(() -> new UserException(ErrorCode.INVALID_TOKEN));

        if (!savedRefreshToken.getToken().equals(requestRefreshToken)) {
            throw new UserException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getRole());

        savedRefreshToken.updateToken(newRefreshToken);

        TokenReissueResponse tokenReissueResponse = TokenReissueResponse.builder()
                .accessToken(newAccessToken)
                .build();

        return TokenReissueResult.builder()
                .tokenReissueResponse(tokenReissueResponse)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.deleteByUser_Id(user.getId());
    }

    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByUser_Id(user.getId())
                .map(savedToken -> {
                    savedToken.updateToken(token);
                    return savedToken;
                })
                .orElseGet(() -> RefreshToken.builder()
                        .user(user)
                        .token(token)
                        .build());

        refreshTokenRepository.save(refreshToken);
    }
}
