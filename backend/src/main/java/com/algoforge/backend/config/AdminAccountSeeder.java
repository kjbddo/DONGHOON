package com.algoforge.backend.config;

import com.algoforge.backend.user.domain.Role;
import com.algoforge.backend.user.domain.User;
import com.algoforge.backend.user.domain.UserStatus;
import com.algoforge.backend.user.repository.RoleRepository;
import com.algoforge.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 첫 기동 시 관리자 계정이 없으면 자동으로 생성한다.
 *
 * 운영 환경에서는 반드시 환경변수로 비밀번호를 override 하고,
 * 첫 로그인 직후 비밀번호를 변경할 것.
 *
 *   ADMIN_INIT_EMAIL=admin@algoforge.local
 *   ADMIN_INIT_USERNAME=admin
 *   ADMIN_INIT_PASSWORD=<강력한 비밀번호>
 *   ADMIN_INIT_ENABLED=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${algoforge.seed.admin.enabled:true}")
    private boolean enabled;

    @Value("${algoforge.seed.admin.email:admin@algoforge.local}")
    private String adminEmail;

    @Value("${algoforge.seed.admin.username:admin}")
    private String adminUsername;

    @Value("${algoforge.seed.admin.password:admin1234}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("[AdminAccountSeeder] disabled, skip");
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("[AdminAccountSeeder] admin already exists: {}", adminEmail);
            return;
        }

        Role adminRole = roleRepository.findByName(Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN seed missing"));
        Role userRole = roleRepository.findByName(Role.USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER seed missing"));

        User admin = User.builder()
                .email(adminEmail)
                .username(adminUsername)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .status(UserStatus.ACTIVE)
                .build();
        admin.assignRoles(List.of(adminRole, userRole));
        userRepository.save(admin);

        log.warn("============================================================");
        log.warn(" [AdminAccountSeeder] 초기 관리자 계정이 생성되었습니다.");
        log.warn("   email   : {}", adminEmail);
        log.warn("   username: {}", adminUsername);
        log.warn("   password: <환경변수 ADMIN_INIT_PASSWORD 또는 기본값>");
        log.warn(" 첫 로그인 후 즉시 비밀번호를 변경하세요.");
        log.warn("============================================================");
    }
}
