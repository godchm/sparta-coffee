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

        saveMenuIfNotExists("카라멜마키아토", 4500);
        saveMenuIfNotExists("돌체라떼", 4600);
        saveMenuIfNotExists("연유라떼", 4500);
        saveMenuIfNotExists("헤이즐넛라떼", 4300);
        saveMenuIfNotExists("아인슈페너", 4800);
        saveMenuIfNotExists("콜드브루", 4200);
        saveMenuIfNotExists("콜드브루라떼", 4700);
        saveMenuIfNotExists("디카페인아메리카노", 3500);
        saveMenuIfNotExists("디카페인라떼", 4000);
        saveMenuIfNotExists("에스프레소", 2800);

        saveMenuIfNotExists("초콜릿라떼", 4300);
        saveMenuIfNotExists("말차라떼", 4500);
        saveMenuIfNotExists("고구마라떼", 4500);
        saveMenuIfNotExists("토피넛라떼", 4700);
        saveMenuIfNotExists("흑임자라떼", 4800);
        saveMenuIfNotExists("밀크티", 4500);
        saveMenuIfNotExists("얼그레이밀크티", 4700);
        saveMenuIfNotExists("딸기라떼", 4800);
        saveMenuIfNotExists("블루베리라떼", 4800);
        saveMenuIfNotExists("민트초코라떼", 4700);

        saveMenuIfNotExists("레몬에이드", 4500);
        saveMenuIfNotExists("자몽에이드", 4500);
        saveMenuIfNotExists("청포도에이드", 4500);
        saveMenuIfNotExists("유자에이드", 4500);
        saveMenuIfNotExists("패션후르츠에이드", 4800);
        saveMenuIfNotExists("복숭아아이스티", 3800);
        saveMenuIfNotExists("레몬티", 4000);
        saveMenuIfNotExists("자몽티", 4000);
        saveMenuIfNotExists("유자차", 4000);
        saveMenuIfNotExists("캐모마일티", 3500);

        saveMenuIfNotExists("페퍼민트티", 3500);
        saveMenuIfNotExists("얼그레이티", 3500);
        saveMenuIfNotExists("히비스커스티", 3800);
        saveMenuIfNotExists("플레인요거트스무디", 5200);
        saveMenuIfNotExists("딸기요거트스무디", 5500);
        saveMenuIfNotExists("블루베리요거트스무디", 5500);
        saveMenuIfNotExists("망고스무디", 5500);
        saveMenuIfNotExists("쿠키앤크림프라페", 5800);
        saveMenuIfNotExists("자바칩프라페", 5800);
        saveMenuIfNotExists("카라멜프라페", 5600);
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
