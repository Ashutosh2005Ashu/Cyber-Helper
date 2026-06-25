package com.ashutosh.cyberhelper.repository;

import com.ashutosh.cyberhelper.entity.Role;
import com.ashutosh.cyberhelper.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(RoleName roleName);

    boolean existsByRoleName(RoleName roleName);
}
