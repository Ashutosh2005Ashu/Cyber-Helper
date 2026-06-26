package com.ashutosh.cyberhelper.service;

import com.ashutosh.cyberhelper.dto.user.UserResponse;
import com.ashutosh.cyberhelper.entity.User;
import com.ashutosh.cyberhelper.exception.UserNotFoundException;
import com.ashutosh.cyberhelper.repository.UserRepository;
import com.ashutosh.cyberhelper.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
