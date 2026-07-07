package com.ashutosh.cyberhelper.validation;

import com.ashutosh.cyberhelper.exception.DuplicateResourceException;
import com.ashutosh.cyberhelper.exception.UserNotFoundException;
import com.ashutosh.cyberhelper.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class ValidationHelper {
    private final UserRepository userRepository;

    public ValidationHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void validateEmailUnique(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email '" + email + "' is already in use");
        }
    }

    public void validateEmailUniqueExcept(String email, Long userId) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (!user.getId().equals(userId)) {
                throw new DuplicateResourceException("Email '" + email + "' is already in use");
            }
        });
    }

    public void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
    }

    public void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    public void validateOrgNameFormat(String orgName) {
        if (orgName == null || orgName.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization name cannot be empty");
        }
        if (orgName.length() < 2) {
            throw new IllegalArgumentException("Organization name must be at least 2 characters");
        }
        if (orgName.length() > 255) {
            throw new IllegalArgumentException("Organization name cannot exceed 255 characters");
        }
    }

    public void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found: " + userId);
        }
    }
}
