package org.sparta_coffee.common.config;

import lombok.RequiredArgsConstructor;
import org.sparta_coffee.domain.menu.entity.Menu;
import org.sparta_coffee.domain.menu.repository.MenuRepository;
import org.sparta_coffee.domain.user.entity.User;
import org.sparta_coffee.domain.user.enums.UserRole;
import org.sparta_coffee.domain.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class InitData implements CommandLineRunner {

    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initMenus();
        initUsers();
    }

    private void initMenus() {
        saveMenuIfNotExists("아메리카노", 3000);
        saveMenuIfNotExists("카페라떼", 3500);
        saveMenuIfNotExists("바닐라라떼", 4000);
        saveMenuIfNotExists("카푸치노", 3800);
        saveMenuIfNotExists("카페모카", 4200);
    }

    private void saveMenuIfNotExists(String name, long price) {
        if (menuRepository.findByName(name).isPresent()) {
            return;
        }

        Menu menu = Menu.builder()
                .name(name)
                .price(price)
                .build();

        menuRepository.save(menu);
    }

    private void initUsers() {
        saveUserIfNotExists(
                "일반유저",
                "user@test.com",
                "1234",
                UserRole.USER
        );

        saveUserIfNotExists(
                "관리자",
                "admin@test.com",
                "1234",
                UserRole.ADMIN
        );
    }

    private void saveUserIfNotExists(String name, String email, String password, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();

        userRepository.save(user);
    }
}
