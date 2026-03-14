package com.abhay.dubairealestate.infrastructure.persistence.adapter;

import com.abhay.dubairealestate.application.port.in.RentsFilter;
import com.abhay.dubairealestate.application.port.out.LoadRentsTransactionsPort;
import com.abhay.dubairealestate.application.port.out.SaveRentsTransactionsPort;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.RentTransaction;
import com.abhay.dubairealestate.infrastructure.persistence.entity.RentTransactionEntity;
import com.abhay.dubairealestate.infrastructure.persistence.projection.AreaAveragePriceProjection;
import com.abhay.dubairealestate.infrastructure.persistence.repository.RentTransactionJpaRepository;
import com.abhay.dubairealestate.infrastructure.persistence.specification.RentTransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RentsPersistenceAdapter implements SaveRentsTransactionsPort, LoadRentsTransactionsPort {

    private static final int BATCH_SIZE = 500;

    private final RentTransactionJpaRepository repository;
    private final JdbcTemplate jdbcTemplate;

    // ── Write ────────────────────────────────────────────────────────────────

    @Override
    public void saveRentsTransactions(List<RentTransaction> transactions) {
        String sql = """
                INSERT INTO rent_transactions
                  (contract_date, annual_amount, start_date, end_date,
                   area_name, project_name, usage, property_type, rooms, actual_area)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ON CONSTRAINT uq_rent_dedup DO NOTHING
                """;

        for (int i = 0; i < transactions.size(); i += BATCH_SIZE) {
            List<RentTransaction> chunk = transactions.subList(i, Math.min(i + BATCH_SIZE, transactions.size()));
            List<Object[]> args = chunk.stream()
                    .map(t -> new Object[]{
                            t.contractDate() != null ? Date.valueOf(t.contractDate()) : null,
                            t.annualAmount(),
                            t.startDate() != null ? Date.valueOf(t.startDate()) : null,
                            t.endDate() != null ? Date.valueOf(t.endDate()) : null,
                            t.areaName(),
                            t.projectName(),
                            t.usage(),
                            t.propertyType(),
                            t.rooms(),
                            t.actualArea()
                    })
                    .toList();
            jdbcTemplate.batchUpdate(sql, args);
        }
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Override
    public Page<RentTransaction> loadRents(RentsFilter filter, Pageable pageable) {
        return repository
                .findAll(RentTransactionSpecification.withFilter(filter), pageable)
                .map(this::toDomain);
    }

    @Override
    public List<AreaAveragePrice> loadAverageRentsPrices() {
        return repository.findAveragePricesPerArea().stream()
                .map(this::toAreaAveragePrice)
                .toList();
    }

    @Override
    public Optional<AreaAveragePrice> loadAverageRentsPriceByArea(String areaName) {
        return repository.findAveragePriceByArea(areaName)
                .map(this::toAreaAveragePrice);
    }

    @Override
    public List<String> loadRentsAreas() {
        return repository.findDistinctAreaNames();
    }

    @Override
    public List<RentTransaction> loadLastRentsByArea(String areaName, int limit) {
        return repository.findLastByAreaName(areaName, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private RentTransaction toDomain(RentTransactionEntity e) {
        return new RentTransaction(
                e.getId(),
                e.getContractDate(),
                e.getAnnualAmount(),
                e.getStartDate(),
                e.getEndDate(),
                e.getAreaName(),
                e.getProjectName(),
                e.getUsage(),
                e.getPropertyType(),
                e.getRooms(),
                e.getActualArea()
        );
    }

    private AreaAveragePrice toAreaAveragePrice(AreaAveragePriceProjection p) {
        BigDecimal avg = p.getAveragePrice() != null
                ? BigDecimal.valueOf(p.getAveragePrice()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new AreaAveragePrice(p.getAreaName(), avg, p.getTransactionCount());
    }
}
