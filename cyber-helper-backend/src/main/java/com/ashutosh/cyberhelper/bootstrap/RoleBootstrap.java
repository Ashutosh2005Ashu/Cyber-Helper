package com.ashutosh.cyberhelper.bootstrap;

import com.ashutosh.cyberhelper.entity.Role;
import com.ashutosh.cyberhelper.entity.RoleName;
import com.ashutosh.cyberhelper.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RoleBootstrap implements CommandLineRunner {
    private final RoleRepository roleRepository;

    public RoleBootstrap(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<RoleName, String> descriptions = new EnumMap<>(RoleName.class);
        descriptions.put(RoleName.ADMIN, "System administrator");
        descriptions.put(RoleName.RE_USER, "Registered end user");
        descriptions.put(RoleName.AUDITOR, "Compliance auditor");
        descriptions.put(RoleName.SEBI_OFFICER, "SEBI officer");

        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByRoleName(roleName).orElseGet(() -> roleRepository.save(Role.builder()
                    .roleName(roleName)
                    .description(descriptions.get(roleName))
                    .build()));
        }
    }
}
