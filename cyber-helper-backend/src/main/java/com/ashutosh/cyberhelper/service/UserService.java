package com.ashutosh.cyberhelper.service;

import com.ashutosh.cyberhelper.dto.user.UserResponse;
import com.ashutosh.cyberhelper.dto.user.UserUpdateRequest;
import com.ashutosh.cyberhelper.entity.Organization;
import com.ashutosh.cyberhelper.entity.Role;
import com.ashutosh.cyberhelper.entity.User;
import com.ashutosh.cyberhelper.exception.OrganizationNotFoundException;
import com.ashutosh.cyberhelper.exception.RoleNotFoundException;
import com.ashutosh.cyberhelper.exception.UserNotFoundException;
import com.ashutosh.cyberhelper.repository.OrganizationRepository;
import com.ashutosh.cyberhelper.repository.RoleRepository;
import com.ashutosh.cyberhelper.repository.UserRepository;
import com.ashutosh.cyberhelper.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
    }

    public UserResponse getUserById(Long id) {
        return UserResponse.from(findUserById(id));
    }

    public UserResponse getUserById(Long id, Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        if (!authenticatedUser.id().equals(id) && !authenticatedUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("You are not allowed to view this user");
        }
        return UserResponse.from(findUserById(id));
    }

    public UserResponse getCurrentUser(Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        return UserResponse.from(findUserById(authenticatedUser.id()));
    }

    public List<UserResponse> getAllUsers(Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        if (!authenticatedUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only ADMIN users can view all users");
        }
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public List<UserResponse> getUsersByOrganization(Long orgId, Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        if (!authenticatedUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only ADMIN users can view organization users");
        }
        return userRepository.findByOrganizationId(orgId).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request, Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        if (!authenticatedUser.id().equals(id) && !authenticatedUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("You are not allowed to update this user");
        }

        User user = findUserById(id);

        if (request.email() != null && !request.email().isBlank()) {
            user.setEmail(request.email());
        }
        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName());
        }
        if (request.organizationId() != null) {
            Organization org = organizationRepository.findById(request.organizationId())
                    .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + request.organizationId()));
            user.setOrganization(org);
        }
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            Set<Role> roles = roleRepository.findAllById(request.roleIds()).stream().collect(java.util.stream.Collectors.toSet());
            if (roles.size() != request.roleIds().size()) {
                throw new RoleNotFoundException("Some roles not found");
            }
            user.setRoles(roles);
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id, Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        if (!authenticatedUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only ADMIN users can delete users");
        }

        User user = findUserById(id);
        userRepository.delete(user);
    }

    @Transactional
    public UserResponse assignRolesToUser(Long userId, List<Long> roleIds, Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        if (!authenticatedUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only ADMIN users can assign roles");
        }

        User user = findUserById(userId);
        Set<Role> roles = roleRepository.findAllById(roleIds).stream().collect(java.util.stream.Collectors.toSet());
        if (roles.size() != roleIds.size()) {
            throw new RoleNotFoundException("Some roles not found");
        }
        user.setRoles(roles);

        return UserResponse.from(userRepository.save(user));
    }

    public List<UserResponse> searchUsersByEmail(String email, Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        if (!authenticatedUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only ADMIN users can search users");
        }
        return userRepository.findByEmailContainingIgnoreCase(email).stream()
                .map(UserResponse::from)
                .toList();
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    private AuthenticatedUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new UserNotFoundException("Authenticated user is not available");
        }
        return authenticatedUser;
    }
}
