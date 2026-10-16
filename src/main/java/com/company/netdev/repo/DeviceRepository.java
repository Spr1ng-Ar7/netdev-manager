package com.company.netdev.repo;

import com.company.netdev.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {

    long countByStatus(String status);

    @Query("select d.type as name, count(d) as value from Device d group by d.type")
    List<Object[]> countByType();

    @Query("select d.status as name, count(d) as value from Device d group by d.status")
    List<Object[]> countByStatusGroup();

    @Query("select d.department.name as name, count(d) as value from Device d where d.department is not null group by d.department.name")
    List<Object[]> countByDepartment();

    @Query(value = "select date_format(purchase_date, '%Y-%m') as month, count(*) as cnt from biz_device " +
            "where purchase_date is not null group by month order by month", nativeQuery = true)
    List<Object[]> countByPurchaseMonth();

    long countByDepartmentId(Long departmentId);

    long countByLocationId(Long locationId);
}
