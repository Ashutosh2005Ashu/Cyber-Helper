package com.ashutosh.cyberhelper.service;

import com.ashutosh.cyberhelper.dto.auth.AuthRequest;
import com.ashutosh.cyberhelper.dto.auth.AuthResponse;
import com.ashutosh.cyberhelper.dto.auth.UserRegisterRequest;
import com.ashutosh.cyberhelper.dto.user.UserResponse;
import com.ashutosh.cyberhelper.entity.Organization;
import com.ashutosh.cyberhelper.entity.Role;
import com.ashutosh.cyberhelper.entity.RoleName;
import com.ashutosh.cyberhelper.entity.User;
import com.ashutosh.cyberhelper.exception.DuplicateResourceException;
import com.ashutosh.cyberhelper.exception.InvalidCredentialsException;
import com.ashutosh.cyberhelper.exception.OrganizationNotFoundException;
import com.ashutosh.cyberhelper.exception.RoleNotFoundException;
import com.ashutosh.cyberhelper.repository.OrganizationRepository;
import com.ashutosh.cyberhelper.repository.RoleRepository;
import com.ashutosh.cyberhelper.repository.UserRepository;
import com.ashutosh.cyberhelper.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(UserRegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("A user with this email already exists");
        }

        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));

        Set<Role> roles = resolveRoles(request.roleIds());
        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .active(true)
                .organization(organization)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        return new AuthResponse(
                jwtTokenProvider.generateToken(savedUser),
                "Bearer",
                jwtTokenProvider.getExpirationMillis() / 1000,
                UserResponse.from(savedUser)
        );
    }

    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return new AuthResponse(
                jwtTokenProvider.generateToken(user),
                "Bearer",
                jwtTokenProvider.getExpirationMillis() / 1000,
                UserResponse.from(user)
        );
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        Set<Role> roles = new LinkedHashSet<>();
        if (roleIds == null || roleIds.isEmpty()) {
            roles.add(roleRepository.findByRoleName(RoleName.RE_USER)
                    .orElseThrow(() -> new RoleNotFoundException("Default role RE_USER is not configured")));
            return roles;
        }

        for (Long roleId : roleIds) {
            roles.add(roleRepository.findById(roleId)
                    .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId)));
        }
        return roles;
    }
}
