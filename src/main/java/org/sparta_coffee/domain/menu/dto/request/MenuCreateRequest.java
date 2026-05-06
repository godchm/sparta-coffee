package org.sparta_coffee.domain.menu.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MenuCreateRequest(

        @NotBlank(message = "메뉴 이름은 필수입니다.")
        String name,

        @Min(value = 1, message = "가격은 1원 이상이어야 합니다.")
        long price
) {
}