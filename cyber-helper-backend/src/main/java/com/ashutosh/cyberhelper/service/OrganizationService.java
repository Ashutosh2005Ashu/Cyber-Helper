package com.ashutosh.cyberhelper.service;

import com.ashutosh.cyberhelper.dto.organization.OrganizationRequest;
import com.ashutosh.cyberhelper.dto.organization.OrganizationResponse;
import com.ashutosh.cyberhelper.entity.Organization;
import com.ashutosh.cyberhelper.entity.OrganizationStatus;
import com.ashutosh.cyberhelper.entity.OrganizationType;
import com.ashutosh.cyberhelper.exception.DuplicateResourceException;
import com.ashutosh.cyberhelper.exception.OrganizationNotFoundException;
import com.ashutosh.cyberhelper.repository.OrganizationRepository;
import com.ashutosh.cyberhelper.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrganizationService {
    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public OrganizationResponse getOrganizationById(Long id) {
        return OrganizationResponse.from(findOrgById(id));
    }

    public List<OrganizationResponse> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    public List<OrganizationResponse> getMasterOrganizations() {
        return organizationRepository.findByMasterTrue().stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    public List<OrganizationResponse> getOrganizationAliases(Long masterOrgId) {
        return organizationRepository.findAllByMasterOrganization_Id(masterOrgId).stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request, Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        if (!authenticatedUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only ADMIN users can create organizations");
        }

        if (organizationRepository.existsByOrgName(request.orgName())) {
            throw new DuplicateResourceException("Organization with name '" + request.orgName() + "' already exists");
        }

        Organization org = new Organization();
        org.setOrgName(request.orgName());
        org.setOrgType(OrganizationType.valueOf(request.orgType()));
        org.setRegistrationNo(request.registrationNo());

        if (request.masterOrgId() != null) {
            Organization masterOrg = findOrgById(request.masterOrgId());
            if (!masterOrg.isMaster()) {
                throw new IllegalArgumentException("Master organization must be a master org");
            }
            org.setMasterOrganization(masterOrg);
            ;
            org.setMaster(false);
        } else {
            org.setMaster(true);
            org.setMasterOrganization(null);
        }

        org.setStatus(OrganizationStatus.ACTIVE);
        return OrganizationResponse.from(organizationRepository.save(org));
    }

    @Transactional
    public OrganizationResponse updateOrganization(Long id, OrganizationRequest request,
            Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        if (!authenticatedUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only ADMIN users can update organizations");
        }

        Organization org = findOrgById(id);

        if (request.orgName() != null && !request.orgName().isBlank() && !org.getOrgName().equals(request.orgName())) {
            if (organizationRepository.existsByOrgName(request.orgName())) {
                throw new DuplicateResourceException(
                        "Organization with name '" + request.orgName() + "' already exists");
            }
            org.setOrgName(request.orgName());
        }

        if (request.orgType() != null && !request.orgType().isBlank()) {
            org.setOrgType(OrganizationType.valueOf(request.orgType()));
        }

        if (request.registrationNo() != null && !request.registrationNo().isBlank()) {
            org.setRegistrationNo(request.registrationNo());
        }

        return OrganizationResponse.from(organizationRepository.save(org));
    }

    @Transactional
    public void deleteOrganization(Long id, Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        if (!authenticatedUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only ADMIN users can delete organizations");
        }

        Organization org = findOrgById(id);
        if (org.isMaster()) {
            List<Organization> aliases = organizationRepository.findAllByMasterOrganization_Id(id);
            if (!aliases.isEmpty()) {
                throw new IllegalArgumentException("Cannot delete master organization with active aliases");
            }
        }
        organizationRepository.delete(org);
    }

    @Transactional
    public OrganizationResponse changeOrganizationStatus(Long id, String status, Authentication authentication) {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser(authentication);
        if (!authenticatedUser.roles().contains("ADMIN")) {
            throw new AccessDeniedException("Only ADMIN users can change organization status");
        }

        Organization org = findOrgById(id);
        org.setStatus(OrganizationStatus.valueOf(status));
        return OrganizationResponse.from(organizationRepository.save(org));
    }

    public Organization getCanonicalOrganization(Long orgId) {
        Organization org = findOrgById(orgId);
        if (org.isMaster()) {
            return org;
        } else {
            return org.getMasterOrganization();
        }
    }

    private Organization findOrgById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + id));
    }

    private AuthenticatedUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new AccessDeniedException("Authentication required");
        }
        return authenticatedUser;
    }
}
