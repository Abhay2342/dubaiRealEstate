package com.abhay.dubairealestate.infrastructure.persistence.projection;

/**
 * Spring Data JPA projection for aggregated average price per area.
 * Returned by aggregate JPQL queries in the repositories.
 */
public interface AreaAveragePriceProjection {
    String getAreaName();
    Double getAveragePrice();
    Long getTransactionCount();
}
