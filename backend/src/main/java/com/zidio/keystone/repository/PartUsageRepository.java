package com.zidio.keystone.repository;

import com.zidio.keystone.domain.PartUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartUsageRepository extends JpaRepository<PartUsage, UUID> {
    List<PartUsage> findByWorkOrderId(UUID workOrderId);
}
