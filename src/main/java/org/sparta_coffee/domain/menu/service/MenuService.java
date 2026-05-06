package org.sparta_coffee.domain.menu.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.menu.dto.request.MenuCreateRequest;
import org.sparta_coffee.domain.menu.dto.request.MenuUpdateRequest;
import org.sparta_coffee.domain.menu.dto.response.MenuResponse;
import org.sparta_coffee.domain.menu.entity.Menu;
import org.sparta_coffee.domain.menu.repository.MenuRepository;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.domain.MenuException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.awt.SystemColor.menu;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    @Transactional(readOnly = true)
    public List<MenuResponse> getMenus() {
        return menuRepository.findAllByActiveTrueOrderByIdAsc()
                .stream()
                .map(MenuResponse::from)
                .toList();
    }


    @Transactional
    public MenuResponse createMenu(MenuCreateRequest request) {
        Menu menu = Menu.builder()
                .name(request.name())
                .price(request.price())
                .build();

        Menu savedMenu = menuRepository.save(menu);

        return MenuResponse.from(savedMenu);
    }


    @Transactional
    public MenuResponse updateMenu(Long menuId, MenuUpdateRequest request) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));

        menu.update(request.name(), request.price());

        return MenuResponse.from(menu);
    }


    @Transactional
    public void deleteMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));

        menuRepository.delete(menu);
    }
}



