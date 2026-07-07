package com.ashutosh.cyberhelper.repository;

import com.ashutosh.cyberhelper.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByOrgNameIgnoreCase(String orgName);

    boolean existsByOrgNameIgnoreCase(String orgName);

    boolean existsByOrgName(String orgName);

    List<Organization> findByMasterTrue();

    List<Organization> findAllByMasterOrganization_Id(Long masterOrganizationId);

}

