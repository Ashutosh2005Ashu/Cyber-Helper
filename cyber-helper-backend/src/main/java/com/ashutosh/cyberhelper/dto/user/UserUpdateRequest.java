package com.ashutosh.cyberhelper.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UserUpdateRequest(
        @Email(message = "Email must be valid")
        String email,

        String firstName,

        String lastName,

        Long organizationId,

        List<Long> roleIds
) {
}
