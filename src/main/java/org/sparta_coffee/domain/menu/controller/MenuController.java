package org.sparta_coffee.domain.menu.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.menu.dto.request.MenuCreateRequest;
import org.sparta_coffee.domain.menu.dto.request.MenuUpdateRequest;
import org.sparta_coffee.global.dto.ApiResponseDto;
import org.sparta_coffee.domain.menu.dto.response.MenuResponse;
import org.sparta_coffee.domain.menu.service.MenuService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MenuController {

    private final MenuService menuService;


    // 메뉴 단건
    // 메뉴 ID 선택 조회
    // 예: GET /api/v1/menus/1
   // 예: GET /api/v1/menus/1,2
   // 예: GET /api/v1/menus/1,2,3
    @GetMapping("/v1/menus/{menuIds}")
    public ResponseEntity<ApiResponseDto<List<MenuResponse>>> getMenusByIds(
            @PathVariable String menuIds
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success(HttpStatus.OK, menuService.getMenusByIds(menuIds))
        );
    }


    // 메뉴 모두 조회
    @GetMapping("/v1/menus")
    public ResponseEntity<ApiResponseDto<List<MenuResponse>>> getAllMenus() {
        return ResponseEntity.ok(
                ApiResponseDto.success(HttpStatus.OK, menuService.getAllMenus())
        );
    }




   // 메뉴 생성
    @PostMapping("/v1/menus")
    public ResponseEntity<ApiResponseDto<MenuResponse>> createMenu(
            @Valid @RequestBody MenuCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, menuService.createMenu(request)));
    }


    // 메뉴 수정
    @PatchMapping("/v1/menus/{menuId}")
    public ResponseEntity<ApiResponseDto<MenuResponse>> updateMenu(
            @PathVariable Long menuId,
            @Valid @RequestBody MenuUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success(HttpStatus.OK, menuService.updateMenu(menuId, request))
        );
    }


    // 메뉴 삭제
    @DeleteMapping("/v1/menus/{menuId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteMenu(
            @PathVariable Long menuId
    ) {
        menuService.deleteMenu(menuId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDto.successWithNoContent());
    }




}
