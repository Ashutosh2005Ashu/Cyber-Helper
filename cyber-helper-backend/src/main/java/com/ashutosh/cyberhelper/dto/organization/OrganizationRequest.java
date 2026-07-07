package com.ashutosh.cyberhelper.dto.organization;

import jakarta.validation.constraints.NotBlank;

public record OrganizationRequest(
        @NotBlank(message = "Organization name is required")
        String orgName,

        @NotBlank(message = "Organization type is required")
        String orgType,

        String registrationNo,

        Long masterOrgId
) {
}
