package com.company.netdev.repo;

import com.company.netdev.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    Page<OperationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<OperationLog> findByModuleOrderByCreatedAtDesc(String module, Pageable pageable);

    Page<OperationLog> findByDetailContainingOrderByCreatedAtDesc(String detail, Pageable pageable);
}
