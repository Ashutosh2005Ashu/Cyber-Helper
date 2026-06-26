package com.ashutosh.cyberhelper.dto.user;

import com.ashutosh.cyberhelper.entity.User;

import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String organizationName,
        List<String> roles
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getOrganization().getOrgName(),
                user.getRoles().stream()
                        .map(role -> role.getRoleName().name())
                        .sorted()
                        .toList()
        );
    }
}
