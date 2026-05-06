package org.sparta_coffee.domain.user.dto.response;

import lombok.Builder;
import org.sparta_coffee.domain.user.entity.User;
import org.sparta_coffee.domain.user.enums.UserRole;

@Builder
public record UserResponse(
        Long userId,
        String name,
        String email,
        UserRole role
) {

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}