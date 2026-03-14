package com.abhay.dubairealestate.application.port.in;

import com.abhay.dubairealestate.domain.model.SaleTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound port: query sales transactions with optional filters.
 */
public interface QuerySalesUseCase {
    Page<SaleTransaction> querySales(SalesFilter filter, Pageable pageable);
}
