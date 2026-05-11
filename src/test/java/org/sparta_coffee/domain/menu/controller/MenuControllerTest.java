package org.sparta_coffee.domain.menu.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sparta_coffee.domain.menu.dto.response.MenuResponse;
import org.sparta_coffee.domain.menu.service.MenuService;
import org.sparta_coffee.global.security.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuController.class)
@AutoConfigureMockMvc(addFilters = false)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("메뉴 목록 조회에 성공한다")
    void getAllMenus() throws Exception {
        /*
         * given
         */
        when(menuService.getAllMenus()).thenReturn(List.of(
                MenuResponse.builder()
                        .menuId(1L)
                        .name("아메리카노")
                        .price(3000)
                        .build(),
                MenuResponse.builder()
                        .menuId(2L)
                        .name("카페라떼")
                        .price(3500)
                        .build()
        ));

        /*
         * when & then
         */
        mockMvc.perform(get("/api/v1/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[*].menuId", hasItem(1)))
                .andExpect(jsonPath("$.data[*].menuId", hasItem(2)))
                .andExpect(jsonPath("$.data[*].name", hasItem("아메리카노")))
                .andExpect(jsonPath("$.data[*].name", hasItem("카페라떼")))
                .andExpect(jsonPath("$.data[*].price", hasItem(3000)))
                .andExpect(jsonPath("$.data[*].price", hasItem(3500)));
    }
}
