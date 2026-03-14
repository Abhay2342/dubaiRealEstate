package com.abhay.dubairealestate.infrastructure.persistence.adapter;

import com.abhay.dubairealestate.application.port.in.SalesFilter;
import com.abhay.dubairealestate.application.port.out.LoadSalesTransactionsPort;
import com.abhay.dubairealestate.application.port.out.SaveSalesTransactionsPort;
import com.abhay.dubairealestate.domain.model.AreaAveragePrice;
import com.abhay.dubairealestate.domain.model.SaleTransaction;
import com.abhay.dubairealestate.infrastructure.persistence.entity.SaleTransactionEntity;
import com.abhay.dubairealestate.infrastructure.persistence.projection.AreaAveragePriceProjection;
import com.abhay.dubairealestate.infrastructure.persistence.repository.SaleTransactionJpaRepository;
import com.abhay.dubairealestate.infrastructure.persistence.specification.SaleTransactionSpecification;
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
public class SalesPersistenceAdapter implements SaveSalesTransactionsPort, LoadSalesTransactionsPort {

    private static final int BATCH_SIZE = 500;

    private final SaleTransactionJpaRepository repository;
    private final JdbcTemplate jdbcTemplate;

    // ── Write ────────────────────────────────────────────────────────────────

    @Override
    public void saveSalesTransactions(List<SaleTransaction> transactions) {
        String sql = """
                INSERT INTO sale_transactions
                  (trans_id, trans_date, trans_value, area_name, project_name,
                   usage, registration_type, property_type, rooms, actual_area, meter_sale_price)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ON CONSTRAINT uq_sale_trans_id_area DO NOTHING
                """;

        for (int i = 0; i < transactions.size(); i += BATCH_SIZE) {
            List<SaleTransaction> chunk = transactions.subList(i, Math.min(i + BATCH_SIZE, transactions.size()));
            List<Object[]> args = chunk.stream()
                    .map(t -> new Object[]{
                            t.transId(),
                            t.transDate() != null ? Date.valueOf(t.transDate()) : null,
                            t.transValue(),
                            t.areaName(),
                            t.projectName(),
                            t.usage(),
                            t.registrationType(),
                            t.propertyType(),
                            t.rooms(),
                            t.actualArea(),
                            t.meterSalePrice()
                    })
                    .toList();
            jdbcTemplate.batchUpdate(sql, args);
        }
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Override
    public Page<SaleTransaction> loadSales(SalesFilter filter, Pageable pageable) {
        return repository
                .findAll(SaleTransactionSpecification.withFilter(filter), pageable)
                .map(this::toDomain);
    }

    @Override
    public List<AreaAveragePrice> loadAverageSalesPrices() {
        return repository.findAveragePricesPerArea().stream()
                .map(this::toAreaAveragePrice)
                .toList();
    }

    @Override
    public Optional<AreaAveragePrice> loadAverageSalesPriceByArea(String areaName) {
        return repository.findAveragePriceByArea(areaName)
                .map(this::toAreaAveragePrice);
    }

    @Override
    public List<String> loadSalesAreas() {
        return repository.findDistinctAreaNames();
    }

    @Override
    public List<SaleTransaction> loadLastSalesByArea(String areaName, int limit) {
        return repository.findLastByAreaName(areaName, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private SaleTransaction toDomain(SaleTransactionEntity e) {
        return new SaleTransaction(
                e.getId(),
                e.getTransId(),
                e.getTransDate(),
                e.getTransValue(),
                e.getAreaName(),
                e.getProjectName(),
                e.getUsage(),
                e.getRegistrationType(),
                e.getPropertyType(),
                e.getRooms(),
                e.getActualArea(),
                e.getMeterSalePrice()
        );
    }

    private AreaAveragePrice toAreaAveragePrice(AreaAveragePriceProjection p) {
        BigDecimal avg = p.getAveragePrice() != null
                ? BigDecimal.valueOf(p.getAveragePrice()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new AreaAveragePrice(p.getAreaName(), avg, p.getTransactionCount());
    }
}
