package com.zidio.keystone.repository;

import com.zidio.keystone.domain.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimeLogRepository extends JpaRepository<TimeLog, UUID> {
    List<TimeLog> findByWorkOrderId(UUID workOrderId);
}
