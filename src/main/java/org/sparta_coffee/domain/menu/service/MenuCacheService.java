package org.sparta_coffee.domain.menu.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta_coffee.domain.menu.dto.response.MenuResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String MENU_CACHE_KEY = "menu:";


    // 캐시를 조회
    public MenuResponse getMenuCache(long menuId){
        String key = MENU_CACHE_KEY + menuId;
        // menu: 1 menu:2 menu:3

        MenuResponse cachedMenu = (MenuResponse) redisTemplate.opsForValue().get(key);

        if (cachedMenu != null) {
            log.info("Redis 캐시 조회 성공: key={} 메뉴 데이터가 Redis에 있습니다.", key);
        } else {
            log.info("Redis 캐시 조회 실패: key={} 메뉴 데이터가 Redis에 없습니다.", key);
        }

        return cachedMenu;

    }

    // 캐시를 저장.
    public void saveMenuCache(long menuId, MenuResponse menuResponse){
        String key = MENU_CACHE_KEY + menuId;
        redisTemplate.opsForValue().set(key, menuResponse,10, TimeUnit.MINUTES);
        log.info("Redis 캐시 저장 완료: key={} 메뉴 데이터가 Redis에 저장되었습니다. TTL=10분", key);
    }

    // 캐시를 삭제
    public void deleteMenuCache(long menuId){
        String key = MENU_CACHE_KEY + menuId;
        redisTemplate.delete(key);
        log.info("Redis 캐시 삭제 완료: key={} 메뉴 데이터가 Redis에서 삭제되었습니다.", key);
    }
}
