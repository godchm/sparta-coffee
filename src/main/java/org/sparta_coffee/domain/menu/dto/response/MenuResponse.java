package org.sparta_coffee.domain.menu.dto.response;


import lombok.Builder;
import org.sparta_coffee.domain.menu.entity.Menu;

@Builder
public record MenuResponse(
        Long menuId,
        String name,
        long price
) {

    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .menuId(menu.getId())
                .name(menu.getName())
                .price(menu.getPrice())
                .build();
    }
}
