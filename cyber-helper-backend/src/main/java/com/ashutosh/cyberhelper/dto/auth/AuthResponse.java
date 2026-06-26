package com.ashutosh.cyberhelper.dto.auth;

import com.ashutosh.cyberhelper.dto.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
