package com.abhay.dubairealestate.infrastructure.persistence.repository;

import com.abhay.dubairealestate.infrastructure.persistence.entity.SaleTransactionEntity;
import com.abhay.dubairealestate.infrastructure.persistence.projection.AreaAveragePriceProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SaleTransactionJpaRepository
        extends JpaRepository<SaleTransactionEntity, Long>,
                JpaSpecificationExecutor<SaleTransactionEntity> {

    @Query("""
            SELECT s.areaName  AS areaName,
                   AVG(s.transValue) AS averagePrice,
                   COUNT(s)          AS transactionCount
            FROM SaleTransactionEntity s
            GROUP BY s.areaName
            ORDER BY s.areaName
            """)
    List<AreaAveragePriceProjection> findAveragePricesPerArea();

    @Query("""
            SELECT s.areaName  AS areaName,
                   AVG(s.transValue) AS averagePrice,
                   COUNT(s)          AS transactionCount
            FROM SaleTransactionEntity s
            WHERE LOWER(s.areaName) = LOWER(:areaName)
            GROUP BY s.areaName
            """)
    Optional<AreaAveragePriceProjection> findAveragePriceByArea(@Param("areaName") String areaName);

    @Query("SELECT DISTINCT s.areaName FROM SaleTransactionEntity s WHERE s.areaName IS NOT NULL ORDER BY s.areaName")
    List<String> findDistinctAreaNames();

    @Query("SELECT s FROM SaleTransactionEntity s WHERE LOWER(s.areaName) = LOWER(:areaName) ORDER BY s.transDate DESC")
    List<SaleTransactionEntity> findLastByAreaName(@Param("areaName") String areaName, Pageable pageable);
}
