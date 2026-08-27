package com.zidio.keystone.repository;

import com.zidio.keystone.domain.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<Site, UUID> {
    List<Site> findByCustomerId(UUID customerId);
}
