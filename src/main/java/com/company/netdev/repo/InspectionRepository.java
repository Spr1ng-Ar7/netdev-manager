package com.company.netdev.repo;

import com.company.netdev.entity.InspectionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionRepository extends JpaRepository<InspectionRecord, Long> {
    Page<InspectionRecord> findAllByOrderByIdDesc(Pageable pageable);

    Page<InspectionRecord> findByDeviceIdOrderByIdDesc(Long deviceId, Pageable pageable);

    Page<InspectionRecord> findByResultOrderByIdDesc(String result, Pageable pageable);

    List<InspectionRecord> findByDeviceId(Long deviceId);
}
