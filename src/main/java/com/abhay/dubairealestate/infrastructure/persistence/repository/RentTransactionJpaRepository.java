package com.abhay.dubairealestate.infrastructure.persistence.repository;

import com.abhay.dubairealestate.infrastructure.persistence.entity.RentTransactionEntity;
import com.abhay.dubairealestate.infrastructure.persistence.projection.AreaAveragePriceProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RentTransactionJpaRepository
        extends JpaRepository<RentTransactionEntity, Long>,
                JpaSpecificationExecutor<RentTransactionEntity> {

    @Query("""
            SELECT r.areaName  AS areaName,
                   AVG(r.annualAmount) AS averagePrice,
                   COUNT(r)            AS transactionCount
            FROM RentTransactionEntity r
            GROUP BY r.areaName
            ORDER BY r.areaName
            """)
    List<AreaAveragePriceProjection> findAveragePricesPerArea();

    @Query("""
            SELECT r.areaName  AS areaName,
                   AVG(r.annualAmount) AS averagePrice,
                   COUNT(r)            AS transactionCount
            FROM RentTransactionEntity r
            WHERE LOWER(r.areaName) = LOWER(:areaName)
            GROUP BY r.areaName
            """)
    Optional<AreaAveragePriceProjection> findAveragePriceByArea(@Param("areaName") String areaName);

    @Query("SELECT DISTINCT r.areaName FROM RentTransactionEntity r WHERE r.areaName IS NOT NULL ORDER BY r.areaName")
    List<String> findDistinctAreaNames();

    @Query("SELECT r FROM RentTransactionEntity r WHERE LOWER(r.areaName) = LOWER(:areaName) ORDER BY r.contractDate DESC")
    List<RentTransactionEntity> findLastByAreaName(@Param("areaName") String areaName, Pageable pageable);
}
