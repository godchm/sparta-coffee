package org.sparta_coffee.domain.menu.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta_coffee.domain.menu.dto.request.MenuCreateRequest;
import org.sparta_coffee.domain.menu.dto.request.MenuUpdateRequest;
import org.sparta_coffee.domain.menu.dto.response.MenuResponse;
import org.sparta_coffee.domain.menu.entity.Menu;
import org.sparta_coffee.domain.menu.repository.MenuRepository;
import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.domain.MenuException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuCacheService menuCacheService;


    // 메뉴 단건 조회
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenusByIds(String menuIds) {
        List<Long> ids = parseMenuIds(menuIds);

        List<MenuResponse> result = new java.util.ArrayList<>();
        List<Long> cacheMissIds = new java.util.ArrayList<>();

        for (Long menuId : ids) {
            MenuResponse cachedMenu = menuCacheService.getMenuCache(menuId);

            if (cachedMenu != null) {
                log.info("메뉴 캐시 조회 성공: Redis에서 메뉴를 가져왔습니다. menuId={}", menuId);
                result.add(cachedMenu);
            } else {
                log.info("메뉴 캐시 조회 실패: Redis에 메뉴가 없습니다. menuId={}", menuId);
                cacheMissIds.add(menuId);
            }
        }

        if (!cacheMissIds.isEmpty()) {
            log.info("메뉴 DB 조회: Redis에 없는 메뉴를 DB에서 조회합니다. menuIds={}", cacheMissIds);
            List<Menu> menus = menuRepository.findAllByIdInAndActiveTrue(cacheMissIds);

            if (menus.size() != cacheMissIds.size()) {
                throw new MenuException(ErrorCode.MENU_NOT_FOUND);
            }

            List<MenuResponse> dbMenus = menus.stream()
                    .map(MenuResponse::from)
                    .toList();

            for (MenuResponse menuResponse : dbMenus) {
                menuCacheService.saveMenuCache(menuResponse.menuId(), menuResponse);
            }

            result.addAll(dbMenus);
        }

        return result.stream()
                .sorted(java.util.Comparator.comparing(MenuResponse::menuId))
                .toList();
    }

    // 메뉴 모두 조회
    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {


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
        Menu menu = menuRepository.findByIdAndActiveTrue(menuId)
                .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));

        menu.update(request.name(), request.price());


        MenuResponse response = MenuResponse.from(menu);

        // 캐시 삭제
        menuCacheService.deleteMenuCache(menuId);

        return response;
    }


    @Transactional
    public void deleteMenu(Long menuId) {
        Menu menu = menuRepository.findByIdAndActiveTrue(menuId)
                .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));


        menu.delete();

        menuCacheService.deleteMenuCache(menuId);
    }



    private List<Long> parseMenuIds(String menuIds) {
        List<Long> ids;

        try {

            // PathVariable로 들어온 문자열을 콤마 기준으로 분리.
            // 예: "1,2,3" -> ["1", "2", "3"]
            ids = java.util.Arrays.stream(menuIds.split(","))

                    // 각 값 앞뒤 공백을 제거.
                    // 예: " 1 " -> "1"
                    .map(String::trim)

                    // 빈 문자열은 제거합니다.
                    // 예: "1,,2" -> 빈 값 제거 후 ["1", "2"]
                    .filter(id -> !id.isBlank())

                    // 문자열 ID를 Long 타입으로 변환.
                    // 숫자가 아닌 값이 들어오면 NumberFormatException이 발생.
                    .map(Long::valueOf)

                    // 중복된 ID를 제거.
                    // 예: "1,1,2" -> [1, 2]
                    .distinct()
                    .toList();
        } catch (NumberFormatException exception) {

            // 숫자로 변환할 수 없는 값이 들어온 경우 잘못된 입력으로 처리.
            // 예: "1,abc,2"
            throw new MenuException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 콤마만 들어오거나 공백만 들어와서 유효한 ID가 하나도 없는 경우 예외 처리.
        // 예: ",,," 또는 "
        if (ids.isEmpty()) {
            throw new MenuException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return ids;
    }

}



