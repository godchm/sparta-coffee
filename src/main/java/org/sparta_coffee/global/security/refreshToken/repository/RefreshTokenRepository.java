package org.sparta_coffee.global.security.refreshToken.repository;

import java.util.Optional;

import org.sparta_coffee.global.security.refreshToken.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);
}