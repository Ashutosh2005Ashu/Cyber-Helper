package com.ashutosh.cyberhelper.web;

import com.ashutosh.cyberhelper.dto.organization.OrganizationRequest;
import com.ashutosh.cyberhelper.dto.organization.OrganizationResponse;
import com.ashutosh.cyberhelper.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations() {
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }

    @GetMapping("/master")
    public ResponseEntity<List<OrganizationResponse>> getMasterOrganizations() {
        return ResponseEntity.ok(organizationService.getMasterOrganizations());
    }

    @GetMapping("/{masterId}/aliases")
    public ResponseEntity<List<OrganizationResponse>> getOrganizationAliases(@PathVariable Long masterId) {
        return ResponseEntity.ok(organizationService.getOrganizationAliases(masterId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody OrganizationRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createOrganization(request, authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(organizationService.updateOrganization(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteOrganization(@PathVariable Long id, Authentication authentication) {
        organizationService.deleteOrganization(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<OrganizationResponse> changeOrganizationStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {
        return ResponseEntity.ok(organizationService.changeOrganizationStatus(id, status, authentication));
    }
}
