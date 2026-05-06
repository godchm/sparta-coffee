package org.sparta_coffee.global.security.refreshToken.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta_coffee.global.entity.BaseEntity;

@Entity
@Getter
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 500)
    private String token;

    @Builder
    public RefreshToken(String email, String token) {
        this.email = email;
        this.token = token;
    }

    public void updateToken(String token) {
        this.token = token;
    }
}
