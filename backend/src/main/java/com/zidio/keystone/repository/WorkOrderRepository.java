package com.zidio.keystone.repository;

import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JpaSpecificationExecutor lets WorkOrderService build role-scoped, filterable
 * queries dynamically (see WorkOrderSpecifications) instead of hand-writing a
 * combinatorial explosion of finder methods.
 */
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID>, JpaSpecificationExecutor<WorkOrder> {

    Optional<WorkOrder> findByCode(String code);

    long countByStatus(WorkOrderStatus status);

    List<WorkOrder> findByStatusNotIn(List<WorkOrderStatus> statuses);
    @Query("SELECT w FROM WorkOrder w LEFT JOIN FETCH w.assignedTo WHERE w.status NOT IN :statuses")
    List<WorkOrder> findByStatusNotInWithAssignee(@Param("statuses") List<WorkOrderStatus> statuses);

}
