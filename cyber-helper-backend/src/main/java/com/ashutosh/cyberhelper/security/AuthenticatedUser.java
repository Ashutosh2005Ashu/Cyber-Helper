package com.ashutosh.cyberhelper.security;

import java.util.Set;

public record AuthenticatedUser(
        Long id,
        String email,
        Set<String> roles
) {
}
