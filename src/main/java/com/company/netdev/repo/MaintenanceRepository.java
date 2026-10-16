package com.company.netdev.repo;

import com.company.netdev.entity.MaintenanceTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRepository extends JpaRepository<MaintenanceTicket, Long> {
    Page<MaintenanceTicket> findByStatusOrderByIdDesc(String status, Pageable pageable);

    Page<MaintenanceTicket> findAllByOrderByIdDesc(Pageable pageable);

    Page<MaintenanceTicket> findByDeviceIdOrderByIdDesc(Long deviceId, Pageable pageable);

    List<MaintenanceTicket> findByDeviceIdOrderByIdDesc(Long deviceId);

    long countByStatus(String status);
}
