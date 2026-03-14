package com.abhay.dubairealestate.application.port.in;

import com.abhay.dubairealestate.domain.model.RentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Inbound port: query rent transactions with optional filters.
 */
public interface QueryRentsUseCase {
    Page<RentTransaction> queryRents(RentsFilter filter, Pageable pageable);
}
