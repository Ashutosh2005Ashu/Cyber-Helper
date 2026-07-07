package com.ashutosh.cyberhelper.dto.organization;

import com.ashutosh.cyberhelper.entity.Organization;

import java.time.LocalDateTime;

public record OrganizationResponse(
        Long id,
        String orgName,
        String orgType,
        String registrationNo,
        String status,
        Boolean isMaster,
        Long masterOrgId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrganizationResponse from(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getOrgName(),
                org.getOrgType().name(),
                org.getRegistrationNo(),
                org.getStatus().name(),
                org.isMaster(),
                org.getMasterOrganization() != null
                        ? org.getMasterOrganization().getId()
                        : null,
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }
}
