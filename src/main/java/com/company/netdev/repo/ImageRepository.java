package com.company.netdev.repo;

import com.company.netdev.entity.UploadImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<UploadImage, Long> {
    List<UploadImage> findByBizTypeAndBizIdOrderByIdAsc(String bizType, Long bizId);

    void deleteByBizTypeAndBizId(String bizType, Long bizId);
}
