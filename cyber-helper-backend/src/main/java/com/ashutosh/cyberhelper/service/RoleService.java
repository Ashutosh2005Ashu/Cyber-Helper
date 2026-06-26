package com.ashutosh.cyberhelper.service;

import com.ashutosh.cyberhelper.dto.role.RoleResponse;
import com.ashutosh.cyberhelper.entity.Role;
import com.ashutosh.cyberhelper.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoleService {
    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getRoleName().name(), role.getDescription());
    }
}
