package org.sparta_coffee.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.user.dto.request.LoginRequest;
import org.sparta_coffee.domain.user.dto.response.LoginResponse;
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
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UserException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getRole());

        saveRefreshToken(user, refreshToken);

        return LoginResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken(accessToken)
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.deleteByUser_Id(userId);
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