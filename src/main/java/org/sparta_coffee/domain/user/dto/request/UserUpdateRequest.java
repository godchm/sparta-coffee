package org.sparta_coffee.domain.user.dto.request;


import jakarta.validation.constraints.NotNull;
import org.sparta_coffee.domain.user.enums.UserRole;
public record UserUpdateRequest(

        @NotNull(message = "유저 이름은 필수입니다.")
        String name,

        @NotNull(message = "유저 권한은 필수입니다.")
        UserRole role
) {
}