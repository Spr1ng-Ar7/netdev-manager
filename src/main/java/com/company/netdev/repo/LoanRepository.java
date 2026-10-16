package com.company.netdev.repo;

import com.company.netdev.entity.LoanRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<LoanRecord, Long> {
    Page<LoanRecord> findByStatusOrderByIdDesc(String status, Pageable pageable);

    Page<LoanRecord> findAllByOrderByIdDesc(Pageable pageable);

    long countByStatus(String status);

    List<LoanRecord> findByStatusAndExpectReturnDateBefore(String status, LocalDate date);

    boolean existsByDeviceIdAndStatus(Long deviceId, String status);

    List<LoanRecord> findByDeviceId(Long deviceId);
}
