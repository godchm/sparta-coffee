package org.sparta_coffee.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sparta_coffee.domain.user.enums.UserRole;

public record UserCreateRequest(


        @NotBlank(message = "유저 이름은 필수입니다.")
        String name,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @NotNull(message = "유저 권한은 필수입니다.")
        UserRole role
) {
}